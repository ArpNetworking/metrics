/**
 * Copyright 2014 Groupon.com
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
import akka.contrib.pattern.ShardRegion;
import akka.dispatch.Foreach;
import akka.http.Http;
import akka.http.HttpExt;
import akka.stream.FlowMaterializer;
import akka.stream.MaterializerSettings;
import ch.qos.logback.classic.LoggerContext;
import com.arpnetworking.clusteraggregator.aggregation.AggMessageExtractor;
import com.arpnetworking.clusteraggregator.aggregation.Aggregator;
import com.arpnetworking.clusteraggregator.aggregation.Bookkeeper;
import com.arpnetworking.clusteraggregator.bookkeeper.persistence.InMemoryBookkeeper;
import com.arpnetworking.clusteraggregator.client.AggClientServer;
import com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration;
import com.arpnetworking.clusteraggregator.configuration.ConfigurableActorProxy;
import com.arpnetworking.clusteraggregator.configuration.EmitterConfiguration;
import com.arpnetworking.clusteraggregator.http.Routes;
import com.arpnetworking.configuration.FileTrigger;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.utility.ActorConfigurator;
import com.arpnetworking.utility.Configurator;
import com.arpnetworking.utility.Launchable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

/**
 * Entry point for the akka-based cluster aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Main implements Launchable {

    /**
     * Entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Launching cluster-aggregator");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOGGER.error("Unhandled exception!", throwable));

        if (args.length != 1) {
            throw new RuntimeException("No configuration file specified");
        }

        LOGGER.debug(String.format("Loading configuration from file; file=%s", args[0]));

        final File configurationFile = new File(args[0]);
        final Configurator<Main, ClusterAggregatorConfiguration> configurator =
                new Configurator<>(Main::new, ClusterAggregatorConfiguration.class);
        final ObjectMapper objectMapper = ClusterAggregatorConfiguration.createObjectMapper();
        final DynamicConfiguration configuration = new DynamicConfiguration.Builder()
                .setObjectMapper(objectMapper)
                .addSourceBuilder(new JsonNodeFileSource.Builder().setObjectMapper(objectMapper).setFile(configurationFile))
                .addTrigger(new FileTrigger.Builder().setFile(configurationFile).build())
                .addListener(configurator)
                .build();

        configuration.launch();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            configuration.shutdown();
            configurator.shutdown();
            LOGGER.info("Stopping cluster-aggregator");
            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.stop();
        }));


    }

    /**
     * Public constructor.
     *
     * @param configuration The configuration object.
     */
    public Main(final ClusterAggregatorConfiguration configuration) {
        _configuration = configuration;
    }

    /**
     * Launch the component.
     */
    @Override
    public void launch() {
        final Injector injector = launchGuice();
        launchAkka();
        launchActors(injector);
    }

    private Injector launchGuice() {
        final Sink sink = new TsdQueryLogSink.Builder()
                .setName("cluster-aggregator-query")
                .setPath(_configuration.getLogDirectory().getAbsolutePath())
                .build();

        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(sink))
                .build();

        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MetricsFactory.class).toInstance(metricsFactory);
            }
        });
    }

    /**
     * Shutdown the component.
     */
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down");
        shutdownAkka();
    }

    private ActorRef launchEmitter(final Injector injector, final File config) {

        final ActorRef emitterConfigurationProxy = _system.actorOf(ConfigurableActorProxy.props(Emitter::props), "emitter-configurator");
        final ActorConfigurator<EmitterConfiguration> configurator =
                new ActorConfigurator<>(emitterConfigurationProxy, EmitterConfiguration.class);
        final ObjectMapper objectMapper = EmitterConfiguration.createObjectMapper(injector);
        final DynamicConfiguration configuration = new DynamicConfiguration.Builder()
                .setObjectMapper(objectMapper)
                .addSourceBuilder(
                        new JsonNodeFileSource.Builder()
                                .setObjectMapper(objectMapper)
                                .setFile(config))
                .addTrigger(new FileTrigger.Builder().setFile(_configuration.getPipelineConfiguration()).build())
                .addListener(configurator)
                .build();

        configuration.launch();

        return emitterConfigurationProxy;
    }

    private void launchActors(final Injector injector) {
        final MetricsFactory metricsFactory = injector.getInstance(MetricsFactory.class);

        final ActorRef emitter = launchEmitter(injector, _configuration.getPipelineConfiguration());
        final ActorRef bookkeeperProxy = launchBookkeeper();
        launchAggregatorLifecycleTracker(bookkeeperProxy);
        final ActorRef metricsListener = launchPeriodicStatsActor(metricsFactory);
        launchAggregatorShardRegion(emitter, bookkeeperProxy, metricsListener);
        launchTcpServer();
        launchClusterStatusCache(bookkeeperProxy, metricsListener);
        launchHttpServer(metricsFactory);
    }

    private void launchClusterStatusCache(final ActorRef bookkeeperProxy, final ActorRef metricsListener) {
        final Cluster cluster = Cluster.get(_system);
        final ActorRef clusterStatusCache = _system.actorOf(ClusterStatusCache.props(cluster), "cluster-status");
        _system.actorOf(Status.props(bookkeeperProxy, cluster, clusterStatusCache, metricsListener), "status");
    }

    private void launchTcpServer() {
        LOGGER.info("Launching tcp server");
        _system.actorOf(Props.create(AggClientServer.class), "tcp-server");
    }

    private void launchAggregatorShardRegion(
            final ActorRef emitter,
            final ActorRef bookkeeperProxy,
            final ActorRef metricsListener) {
        final ClusterSharding aggShardRegion = ClusterSharding.get(_system);
        LOGGER.info("Launching shard region");
        aggShardRegion.start(AGG_SHARD_NAME,
                             Aggregator.props(bookkeeperProxy, metricsListener, emitter),
                             AGG_MESSAGE_EXTRACTOR);
    }

    private ActorRef launchPeriodicStatsActor(final MetricsFactory metricsFactory) {
        return _system.actorOf(PeriodicStatisticsActor.props(metricsFactory));
    }

    private void launchHttpServer(final MetricsFactory metricsFactory) {
        final MaterializerSettings materializerSettings = MaterializerSettings.apply(_system);
        final FlowMaterializer materializer = FlowMaterializer.create(materializerSettings, _system);

        // Create and bind Http server
        final Routes routes = new Routes(_system, metricsFactory);
        final HttpExt httpExt = (HttpExt) Http.get(_system);
        final Http.ServerBinding binding = httpExt.bind(
                _configuration.getHttpHost(),
                _configuration.getHttpPort(),
                httpExt.bind$default$3(),
                httpExt.bind$default$4(),
                httpExt.bind$default$5(),
                httpExt.bind$default$6());
        binding.connections().foreach(new Foreach<Http.IncomingConnection>() {
            @Override
            public void each(final Http.IncomingConnection result) {
                result.handleWithAsyncHandler(routes, materializer);
            }
        }, materializer);
    }

    private void launchAggregatorLifecycleTracker(final ActorRef bookkeeperProxy) {
        final ActorRef aggLifecycle = _system.actorOf(AggregatorLifecycle.props(), "agg-lifecycle");
        aggLifecycle.tell(new AggregatorLifecycle.Subscribe(bookkeeperProxy), bookkeeperProxy);
    }

    private ActorRef launchBookkeeper() {
        LOGGER.info("Launching bookkeeper singleton and proxy");
        _system.actorOf(
                ClusterSingletonManager.defaultProps(
                        Bookkeeper.props(new InMemoryBookkeeper()),
                        "bookkeeper",
                        PoisonPill.getInstance(),
                        null),
                "singleton");

        return _system.actorOf(
                ClusterSingletonProxy.defaultProps(
                        "/user/singleton/bookkeeper",
                        null),
                "bookkeeperProxy");
    }

    private void launchAkka() {
        LOGGER.info("Launching Akka");
        _system = ActorSystem.apply(
                "Metrics",
                ConfigFactory.parseMap(_configuration.getAkkaConfiguration(), "cluster-aggregator configuration"));
    }

    private void shutdownAkka() {
        LOGGER.info("Stopping Akka");
        // TODO(barp): Implement a clean shutdown [MAI-420]
        final Cluster cluster = Cluster.get(_system);
        cluster.leave(cluster.selfAddress());
        try {
            Thread.sleep(5000);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            LOGGER.warn("Interrupted at shutdown", e);
        }
        _system.shutdown();
    }

    private final ClusterAggregatorConfiguration _configuration;

    /**
     * The name of the aggregation shard.
     */
    // TODO(barp): use dependency injection instead of this field [MAI-469]
    public static final String AGG_SHARD_NAME = "Aggregator";

    private ActorSystem _system;
    private static final ShardRegion.MessageExtractor AGG_MESSAGE_EXTRACTOR = new AggMessageExtractor();
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
}
