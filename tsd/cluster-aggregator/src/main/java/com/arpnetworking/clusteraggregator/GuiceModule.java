/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.clusteraggregator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.contrib.pattern.ClusterSharding;
import akka.contrib.pattern.ClusterSingletonManager;
import akka.contrib.pattern.ClusterSingletonProxy;
import akka.http.Http;
import akka.http.HttpExt;
import akka.stream.ActorFlowMaterializer;
import akka.stream.ActorFlowMaterializerSettings;
import akka.stream.scaladsl.Source;
import com.arpnetworking.clusteraggregator.aggregation.AggMessageExtractor;
import com.arpnetworking.clusteraggregator.aggregation.Aggregator;
import com.arpnetworking.clusteraggregator.aggregation.Bookkeeper;
import com.arpnetworking.clusteraggregator.bookkeeper.persistence.InMemoryBookkeeper;
import com.arpnetworking.clusteraggregator.client.AggClientServer;
import com.arpnetworking.clusteraggregator.client.AggClientSupervisor;
import com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration;
import com.arpnetworking.clusteraggregator.configuration.ConfigurableActorProxy;
import com.arpnetworking.clusteraggregator.configuration.EmitterConfiguration;
import com.arpnetworking.clusteraggregator.configuration.RebalanceConfiguration;
import com.arpnetworking.clusteraggregator.http.Routes;
import com.arpnetworking.configuration.FileTrigger;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.guice.akka.GuiceActorCreator;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.utility.ActorConfigurator;
import com.arpnetworking.utility.ParallelLeastShardAllocationStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.compat.java8.JFunction;
import scala.concurrent.Future;

import java.util.Collections;
import java.util.Optional;

/**
 * The primary Guice module used to bootstrap the cluster aggregator. NOTE: this module will be constructed whenever
 * a new configuration is loaded, and will be torn down when another configuration is loaded.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GuiceModule extends AbstractModule {
    /**
     * Public constructor.
     *
     * @param configuration The configuration.
     */
    public GuiceModule(final ClusterAggregatorConfiguration configuration) {
        _configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(ClusterAggregatorConfiguration.class).toInstance(_configuration);
    }

    @Provides
    @Singleton
    @Named("akka-config")
    private Config provideAkkaConfig() {
        return ConfigFactory.parseMap(_configuration.getAkkaConfiguration(), _configuration.toString());
    }

    @Provides
    @Singleton
    private MetricsFactory provideMetricsFactory() {
        final Sink sink = new TsdQueryLogSink.Builder()
                .setName("cluster-aggregator-query")
                .setPath(_configuration.getLogDirectory().getAbsolutePath())
                .build();

        return new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(sink))
                .build();
    }

    @Provides
    @Singleton
    private ActorSystem provideActorSystem(@Named("akka-config") final Config akkaConfig) {
        return ActorSystem.apply("Metrics", akkaConfig);
    }

    @Provides
    @Singleton
    @Named("emitter")
    private ActorRef provideEmitter(final Injector injector, final ActorSystem system) {
        final ActorRef emitterConfigurationProxy = system.actorOf(ConfigurableActorProxy.props(Emitter::props), "emitter-configurator");
        final ActorConfigurator<EmitterConfiguration> configurator =
                new ActorConfigurator<>(emitterConfigurationProxy, EmitterConfiguration.class);
        final ObjectMapper objectMapper = EmitterConfiguration.createObjectMapper(injector);
        final DynamicConfiguration configuration = new DynamicConfiguration.Builder()
                .setObjectMapper(objectMapper)
                .addSourceBuilder(
                        new JsonNodeFileSource.Builder()
                                .setObjectMapper(objectMapper)
                                .setFile(_configuration.getPipelineConfiguration()))
                .addTrigger(new FileTrigger.Builder().setFile(_configuration.getPipelineConfiguration()).build())
                .addListener(configurator)
                .build();

        configuration.launch();

        return emitterConfigurationProxy;
    }

    @Provides
    @Singleton
    @Named("bookkeeper-proxy")
    private ActorRef provideBookkeeperProxy(final ActorSystem system) {
        system.actorOf(
                ClusterSingletonManager.defaultProps(
                        Bookkeeper.props(new InMemoryBookkeeper()),
                        "bookkeeper",
                        PoisonPill.getInstance(),
                        null),
                "singleton");

        return system.actorOf(
                ClusterSingletonProxy.defaultProps(
                        "/user/singleton/bookkeeper",
                        null),
                "bookkeeperProxy");
    }

    @Provides
    @Singleton
    @Named("status-cache")
    private ActorRef provideStatusCache(
            final ActorSystem system,
            @Named("bookkeeper-proxy") final ActorRef bookkeeperProxy,
            @Named("periodic-statistics") final ActorRef periodicStats) {
        final Cluster cluster = Cluster.get(system);
        final ActorRef clusterStatusCache = system.actorOf(ClusterStatusCache.props(cluster), "cluster-status");
        return system.actorOf(Status.props(bookkeeperProxy, cluster, clusterStatusCache, periodicStats), "status");
    }

    @Provides
    @Singleton
    @Named("tcp-server")
    private ActorRef provideTcpServer(final Injector injector, final ActorSystem system) {
        return system.actorOf(Props.create(new GuiceActorCreator(injector, AggClientServer.class)), "tcp-server");
    }

    @Provides
    @Singleton
    @Named("aggregator-lifecycle")
    private ActorRef provideAggregatorLifecycleTracker(
            final ActorSystem system,
            @Named("bookkeeper-proxy") final ActorRef bookkeeperProxy) {
        final ActorRef aggLifecycle = system.actorOf(AggregatorLifecycle.props(), "agg-lifecycle");
        aggLifecycle.tell(new AggregatorLifecycle.Subscribe(bookkeeperProxy), bookkeeperProxy);
        return aggLifecycle;
    }

    @Provides
    @Singleton
    @Named("http-server")
    private Source<Http.IncomingConnection, Future<Http.ServerBinding>> provideHttpServer(
            final ActorSystem system,
            final MetricsFactory metricsFactory) {
        final ActorFlowMaterializerSettings materializerSettings = ActorFlowMaterializerSettings.apply(system);
        final ActorFlowMaterializer materializer = ActorFlowMaterializer.create(materializerSettings, system);

        // Create and bind Http server
        final Routes routes = new Routes(system, metricsFactory);
        final HttpExt httpExt = (HttpExt) Http.get(system);
        final Source<Http.IncomingConnection, Future<Http.ServerBinding>> binding = httpExt.bind(
                _configuration.getHttpHost(),
                _configuration.getHttpPort(),
                httpExt.bind$default$3(),
                httpExt.bind$default$4(),
                httpExt.bind$default$5(),
                httpExt.bind$default$6(),
                materializer);
        binding.runForeach(
                        JFunction.proc(r -> r.handleWithAsyncHandler(routes, materializer)),
                        materializer);
        return binding;
    }


    @Provides
    @Singleton
    @Named("periodic-statistics")
    private ActorRef providePeriodicStatsActor(final ActorSystem system, final MetricsFactory metricsFactory) {
        return system.actorOf(PeriodicStatisticsActor.props(metricsFactory));
    }

    @Provides
    @Singleton
    @Named("aggregator-shard-region")
    private ActorRef provideAggregatorShardRegion(
            final ActorSystem system,
            final Injector injector,
            final AggMessageExtractor extractor) {
        final ClusterSharding clusterSharding = ClusterSharding.get(system);
        final RebalanceConfiguration rebalanceConfiguration = _configuration.getRebalanceConfiguration();
        return clusterSharding.start(
                "Aggregator",
                Props.create(new GuiceActorCreator(injector, Aggregator.class)),
                extractor,
                new ParallelLeastShardAllocationStrategy(
                        rebalanceConfiguration.getMaxParallel(),
                        rebalanceConfiguration.getThreshold(),
                        Optional.of(system.actorSelection("/user/cluster-status"))));
    }

    @Provides
    @Singleton
    @Named("jvm-metrics-collector")
    private ActorRef provideJvmMetricsCollector(final ActorSystem system, final MetricsFactory metricsFactory) {
        return system.actorOf(JvmMetricsCollector.props(_configuration.getJvmMetricsCollectionInterval(), metricsFactory));
    }

    @Provides
    @Singleton
    private AggMessageExtractor provideExtractor() {
        return new AggMessageExtractor();
    }

    @Provides
    @Named("agg-client-supervisor")
    private Props provideAggClientSupervisorProvider(final Injector injector) {
        return Props.create(new GuiceActorCreator(injector, AggClientSupervisor.class));
    }

    private final ClusterAggregatorConfiguration _configuration;
}
