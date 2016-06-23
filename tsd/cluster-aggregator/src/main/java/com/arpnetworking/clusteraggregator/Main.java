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
import akka.http.javadsl.IncomingConnection;
import akka.http.javadsl.ServerBinding;
import akka.stream.javadsl.Source;
import ch.qos.logback.classic.LoggerContext;
import com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.configuration.triggers.FileTrigger;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.utility.Configurator;
import com.arpnetworking.utility.Database;
import com.arpnetworking.utility.Launchable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        LOGGER.info()
                .setMessage("Launching cluster-aggregator")
                .log();

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> {
                    System.err.println("Unhandled exception! exception: " + throwable.toString());
                    throwable.printStackTrace(System.err);
                });

        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, throwable) -> {
                    LOGGER.error()
                            .setMessage("Unhandled exception!")
                            .setThrowable(throwable)
                            .log();
                }
        );

        if (args.length != 1) {
            throw new RuntimeException("No configuration file specified");
        }

        LOGGER.debug()
                .setMessage("Loading configuration from file")
                .addData("file", args[0])
                .log();

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
                            LOGGER.info()
                                    .setMessage("Stopping cluster-aggregator")
                                    .log();
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
    public synchronized void launch() {
        final Injector injector = launchGuice();
        launchDatabases(injector);
        launchAkka(injector);
        launchActors(injector);
    }

    /**
     * Shutdown the component.
     */
    @Override
    public synchronized void shutdown() {
        shutdownAkka();
        shutdownDatabases();
    }

    private Injector launchGuice() {
        return Guice.createInjector(new GuiceModule(_configuration));
    }

    private void launchDatabases(final Injector injector) {
        _databases = Lists.newArrayList();
        for (final String databaseName : _configuration.getDatabaseConfigurations().keySet()) {
            final Database database = injector.getInstance(Key.get(Database.class, Names.named(databaseName)));
            _databases.add(database);
            LOGGER.info()
                    .setMessage("Launching database")
                    .addData("database", database)
                    .log();
            database.launch();
        }
    }

    private void launchActors(final Injector injector) {
        injector.getInstance(Key.get(ActorRef.class, Names.named("host-emitter")));
        injector.getInstance(Key.get(ActorRef.class, Names.named("cluster-emitter")));

        LOGGER.info()
                .setMessage("Launching bookkeeper singleton and proxy")
                .log();
        injector.getInstance(Key.get(ActorRef.class, Names.named("bookkeeper-proxy")));

        injector.getInstance(Key.get(ActorRef.class, Names.named("aggregator-lifecycle")));
        injector.getInstance(Key.get(ActorRef.class, Names.named("periodic-statistics")));

        LOGGER.info()
                .setMessage("Launching shard region")
                .log();
        injector.getInstance(Key.get(ActorRef.class, Names.named("aggregator-shard-region")));

        LOGGER.info()
                .setMessage("Launching tcp server")
                .log();
        injector.getInstance(Key.get(ActorRef.class, Names.named("tcp-server")));
        injector.getInstance(Key.get(ActorRef.class, Names.named("status-cache")));

        LOGGER.info()
                .setMessage("Launching JVM metrics collector")
                .log();
        injector.getInstance(Key.get(ActorRef.class, Names.named("jvm-metrics-collector")));

        LOGGER.info()
                .setMessage("Launching http server")
                .log();
        injector.getInstance(
                Key.get(
                        SOURCE_TYPE_LITERAL,
                        Names.named("http-server")));

        LOGGER.info()
                .setMessage("Launching graceful shutdown actor")
                .log();
        _shutdownActor = injector.getInstance(Key.get(ActorRef.class, Names.named("graceful-shutdown-actor")));
    }

    private void launchAkka(final Injector injector) {
        LOGGER.info()
                .setMessage("Launching Akka")
                .log();
        _system = injector.getInstance(ActorSystem.class);
    }

    private void shutdownDatabases() {
        for (final Database database : _databases) {
            LOGGER.info()
                    .setMessage("Stopping database")
                    .addData("database", database)
                    .log();
            database.shutdown();
        }
    }

    private void shutdownAkka() {
        LOGGER.info()
                .setMessage("Stopping Akka")
                .log();
        _shutdownActor.tell(GracefulShutdownActor.Shutdown.instance(), ActorRef.noSender());
        try {
            Await.result(_system.whenTerminated(), SHUTDOWN_TIMEOUT);
            // CHECKSTYLE.OFF: IllegalCatch - Prevent program shutdown
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            LOGGER.warn()
                    .setMessage("Interrupted at shutdown")
                    .setThrowable(e)
                    .log();
        }
    }

    private final ClusterAggregatorConfiguration _configuration;

    private volatile ActorSystem _system;
    private volatile ActorRef _shutdownActor;
    private volatile List<Database> _databases;

    private static final Logger LOGGER = com.arpnetworking.steno.LoggerFactory.getLogger(Main.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.create(3, TimeUnit.MINUTES);
    private static final SourceTypeLiteral SOURCE_TYPE_LITERAL = new SourceTypeLiteral();

    private static class SourceTypeLiteral extends TypeLiteral<Source<IncomingConnection, Future<ServerBinding>>> {}
}
