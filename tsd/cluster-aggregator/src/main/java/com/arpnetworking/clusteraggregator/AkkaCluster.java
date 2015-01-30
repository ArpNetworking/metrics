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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.Http;
import akka.http.HttpExt;
import akka.stream.FlowMaterializer;
import akka.stream.MaterializerSettings;
import com.arpnetworking.clusteraggregator.aggregation.AggMessageExtractor;
import com.arpnetworking.clusteraggregator.aggregation.Aggregator;
import com.arpnetworking.clusteraggregator.aggregation.Bookkeeper;
import com.arpnetworking.clusteraggregator.bookkeeper.persistence.InMemoryBookkeeper;
import com.arpnetworking.clusteraggregator.client.AggClientServer;
import com.arpnetworking.clusteraggregator.http.Routes;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.tsdcore.sinks.MonitordSink;
import com.arpnetworking.tsdcore.sinks.MultiSink;
import com.arpnetworking.tsdcore.sinks.PeriodicStatisticsSink;
import com.google.common.base.Throwables;
import com.typesafe.config.Config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Entry point for the akka-based cluster aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AkkaCluster {

    private AkkaCluster() {
        _system = ActorSystem.apply("Metrics");
        _log = Logging.getLogger(_system, this);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                _log.error(e, "Unhandled exception");
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // TODO(barp): Implement a clean shutdown [MAI-420]
                _log.info("Starting shutdown process");
                final Cluster cluster = Cluster.get(_system);
                cluster.leave(cluster.selfAddress());
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                    _log.warning("Interrupted at shutdown", e);
                }
                _log.info("Shutting down the actor system");
                _system.shutdown();
            }
        });

        final Config config = _system.settings().config();
        final String metricsPath = config.getString("metrics.path");
        final String metricsName = config.getString("metrics.name");
        final Sink sink = new TsdQueryLogSink.Builder()
                .setName(metricsName)
                .setPath(metricsPath)
                .build();
        _metricsFactory = new TsdMetricsFactory.Builder().setSinks(Collections.singletonList(sink)).build();
    }

    private void run() {
        _log.info("Starting cluster aggregator");
        final ActorRef emitter = createEmitter();

        _log.info("Starting bookkeeper singleton and proxy");
        _system.actorOf(
                ClusterSingletonManager.defaultProps(
                        Bookkeeper.props(new InMemoryBookkeeper()),
                        "bookkeeper",
                        PoisonPill.getInstance(),
                        null),
                "singleton");

        final ActorRef bookkeeperProxy = _system.actorOf(
                ClusterSingletonProxy.defaultProps(
                        "/user/singleton/bookkeeper",
                        null),
                "bookkeeperProxy");

        final ActorRef aggLifecycle = _system.actorOf(AggregatorLifecycle.props(), "agg-lifecycle");
        aggLifecycle.tell(new AggregatorLifecycle.Subscribe(bookkeeperProxy), bookkeeperProxy);


        final ActorRef metricsListener = _system.actorOf(PeriodicStatisticsActor.props(_metricsFactory));
        final ClusterSharding aggShardRegion = ClusterSharding.get(_system);
        aggShardRegion.start(AGG_SHARD_NAME,
                             Aggregator.props(bookkeeperProxy, metricsListener, emitter),
                             _aggMessageExtractor);
        _log.info("Shard region setup, starting tcp server");

        _system.actorOf(Props.create(AggClientServer.class), "tcp-server");

        // Create the status actor
        final Cluster cluster = Cluster.get(_system);
        final ActorRef clusterStatus = _system.actorOf(ClusterStatusCache.props(cluster), "cluster-status");
        _system.actorOf(Status.props(bookkeeperProxy, cluster, clusterStatus, metricsListener), "status");

        final MaterializerSettings materializerSettings = MaterializerSettings.apply(_system);
        final FlowMaterializer materializer = FlowMaterializer.create(materializerSettings, _system);

        // Create and bind Http server
        final Routes routes = new Routes(_system, _metricsFactory);
        final HttpExt httpExt = (HttpExt) Http.get(_system);
        final Http.ServerBinding binding = httpExt.bind(
                "0.0.0.0",
                7066,
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

    private ActorRef createEmitter() {
        ActorRef emitter = null;
        try {
            final MonitordSink monitordSink = new MonitordSink.Builder()
                    .setUri(new URI("http://monitord:8080/results"))
                    .setName("MonitordSink")
                    .build();
            final PeriodicStatisticsSink statisticsSink = new PeriodicStatisticsSink.Builder()
                    .setIntervalInSeconds(60L)
                    .setName("PeriodStatsSink")
                    .setMetricsFactory(_metricsFactory)
                    .build();
            final MultiSink multiSink = new MultiSink.Builder()
                    .setName("MultiSink")
                    .addSink(monitordSink)
                    .addSink(statisticsSink)
                    .build();
            emitter = _system.actorOf(Emitter.props(multiSink), "emitter");
        } catch (final URISyntaxException e) {
            _log.error(e, "Could not start emitter");
            Throwables.propagate(e);
        }
        return emitter;
    }

    /**
     * Entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new AkkaCluster().run();
    }

    /**
     * The name of the aggregation shard.
     */
    public static final String AGG_SHARD_NAME = "Aggregator";

    private final ActorSystem _system;
    private final ShardRegion.MessageExtractor _aggMessageExtractor = new AggMessageExtractor();
    private final MetricsFactory _metricsFactory;
    private final LoggingAdapter _log;
}
