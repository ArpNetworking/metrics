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
import akka.cluster.Cluster;
import akka.http.Http;
import akka.stream.scaladsl.Source;
import ch.qos.logback.classic.LoggerContext;
import com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration;
import com.arpnetworking.configuration.FileTrigger;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.utility.Configurator;
import com.arpnetworking.utility.Launchable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import java.io.File;

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

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> LOGGER.error(
                        "Unhandled exception!",
                        throwable));

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
                .addSourceBuilder(
                        new JsonNodeFileSource.Builder().setObjectMapper(objectMapper)
                                .setFile(configurationFile))
                .addTrigger(new FileTrigger.Builder().setFile(configurationFile).build())
                .addListener(configurator)
                .build();

        configuration.launch();

        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
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
        launchAkka(injector);
        launchActors(injector);
    }

    /**
     * Shutdown the component.
     */
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down");
        shutdownAkka();
    }

    private Injector launchGuice() {
        return Guice.createInjector(new GuiceModule(_configuration));
    }

    private void launchActors(final Injector injector) {
        injector.getInstance(Key.get(ActorRef.class, Names.named("emitter")));

        LOGGER.info("Launching bookkeeper singleton and proxy");
        injector.getInstance(Key.get(ActorRef.class, Names.named("bookkeeper-proxy")));

        injector.getInstance(Key.get(ActorRef.class, Names.named("aggregator-lifecycle")));
        injector.getInstance(Key.get(ActorRef.class, Names.named("periodic-statistics")));

        LOGGER.info("Launching shard region");
        injector.getInstance(Key.get(ActorRef.class, Names.named("aggregator-shard-region")));

        LOGGER.info("Launching tcp server");
        injector.getInstance(Key.get(ActorRef.class, Names.named("tcp-server")));
        injector.getInstance(Key.get(ActorRef.class, Names.named("status-cache")));

        LOGGER.info("Launching JVM metrics collector");
        injector.getInstance(Key.get(ActorRef.class, Names.named("jvm-metrics-collector")));

        LOGGER.info("Launching http server");
        injector.getInstance(
                Key.get(
                        new TypeLiteral<Source<Http.IncomingConnection, Future<Http.ServerBinding>>>() {},
                        Names.named("http-server")));
    }

    private void launchAkka(final Injector injector) {
        LOGGER.info("Launching Akka");
        _system = injector.getInstance(ActorSystem.class);
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

    private ActorSystem _system;
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
}
