/**
 * Copyright 2014 Brandon Arp
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
package com.arpnetworking.tsdaggregator;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Foreach;
import akka.http.HttpExt;
import akka.http.model.japi.Http;
import akka.stream.FlowMaterializer;
import akka.stream.MaterializerSettings;
import ch.qos.logback.classic.LoggerContext;
import com.arpnetworking.configuration.FileTrigger;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.http.Routes;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration;
import com.arpnetworking.tsdaggregator.configuration.TsdAggregatorConfiguration;
import com.arpnetworking.tsdcore.limiter.MetricsLimiter;
import com.arpnetworking.utility.Configurator;
import com.arpnetworking.utility.Launchable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class containing entry point for Time Series Data (TSD) Aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TsdAggregator implements Launchable {

    /**
     * Entry point for Time Series Data (TSD) Aggregator.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Launching tsd-aggregator");

        // Global initialization
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable throwable) {
                LOGGER.error("Unhandled exception!", throwable);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                context.stop();
            }
        }));

        System.setProperty("org.vertx.logger-delegate-factory-class-name", "org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory");

        // Run the tsd aggregator
        if (args.length != 1) {
            throw new RuntimeException("No configuration file specified");
        }
        LOGGER.debug(String.format("Loading configuration from file; file=%s", args[0]));

        final File configurationFile = new File(args[0]);
        final Configurator<TsdAggregator, TsdAggregatorConfiguration> configurator =
                new Configurator<>(TsdAggregator.class, TsdAggregatorConfiguration.class);
        final ObjectMapper objectMapper = TsdAggregatorConfiguration.createObjectMapper();
        final DynamicConfiguration configuration = new DynamicConfiguration.Builder()
                .setObjectMapper(objectMapper)
                .addSourceBuilder(new JsonNodeFileSource.Builder()
                        .setObjectMapper(objectMapper)
                        .setFile(configurationFile))
                .addTrigger(new FileTrigger.Builder()
                        .setFile(configurationFile)
                        .build())
                .addListener(configurator)
                .build();

        configuration.launch();

        final AtomicBoolean isRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Stopping tsd-aggregator");
                configuration.shutdown();
                configurator.shutdown();
                isRunning.set(false);
            }
        }));

        while (isRunning.get()) {
            try {
                Thread.sleep(30000);
            } catch (final InterruptedException e) {
                break;
            }
        }

        LOGGER.info("Exiting tsd-aggregator");
    }

    /**
     * Public constructor.
     *
     * @param tsdAggregatorConfiguration Instance of <code>TsdAggregatorConfiguration</code>.
     */
    public TsdAggregator(final TsdAggregatorConfiguration tsdAggregatorConfiguration) {
        _tsdAggregatorConfiguration = tsdAggregatorConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch() {
        launchCore();
        launchAkka();
        launchLimiters();
        launchPipelines();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        shutdownPipelines();
        shutdownLimiters();
        shutdownAkka();
        shutdownCore();
    }

    private void launchPipelines() {
        LOGGER.info("Launching pipelines");

        final ObjectMapper objectMapper = PipelineConfiguration.createObjectMapper(_injector);

        final File[] files = MoreObjects.firstNonNull(
                _tsdAggregatorConfiguration.getPipelinesDirectory().listFiles(),
                new File[0]);

        for (final File configurationFile : files) {
            LOGGER.debug(String.format("Creating pipeline; configurationFile=%s", configurationFile));

            final Configurator<Pipeline, PipelineConfiguration> pipelineConfigurator =
                    new Configurator<>(Pipeline.class, PipelineConfiguration.class);
            final DynamicConfiguration pipelineConfiguration = new DynamicConfiguration.Builder()
                    .setObjectMapper(objectMapper)
                    .addSourceBuilder(new JsonNodeFileSource.Builder()
                            .setObjectMapper(objectMapper)
                            .setFile(configurationFile))
                    .addTrigger(new FileTrigger.Builder()
                            .setFile(configurationFile)
                            .build())
                    .addListener(pipelineConfigurator)
                    .build();

            pipelineConfiguration.launch();
            _pipelineLaunchables.add(pipelineConfigurator);
            _pipelineLaunchables.add(pipelineConfiguration);
        }

        if (_pipelineLaunchables.isEmpty()) {
            LOGGER.warn(String.format(
                    "No pipelines found in pipelines directory; path=%s",
                    _tsdAggregatorConfiguration.getPipelinesDirectory().getAbsolutePath()));
        }
    }

    private void launchLimiters() {
        LOGGER.info("Launching limiters");

        // NOTE: Limiters were "launched" when they were created
        _limiters.addAll(_tsdAggregatorConfiguration.getLimiters().values());

        for (final Launchable limiter : _limiters) {
            limiter.launch();
        }
    }

    private void launchAkka() {
        LOGGER.info("Launching akka");

        // Initialize Akka

        final Config akkaConfiguration = ConfigFactory.parseMap(_tsdAggregatorConfiguration.getAkkaConfiguration());
        _actorSystem = ActorSystem.create("TsdAggregator", ConfigFactory.load(akkaConfiguration));
        final Routes routes = new Routes(_actorSystem, _metricsFactory);

        // Create the status actor
        _actorSystem.actorOf(Props.create(Status.class), "status");

        final MaterializerSettings materializerSettings = MaterializerSettings.create(_actorSystem);
        final FlowMaterializer materializer = FlowMaterializer.create(materializerSettings, _actorSystem);

        // Create and bind Http server
        final HttpExt httpExt = (HttpExt) Http.get(_actorSystem);
        final akka.http.Http.ServerBinding binding = httpExt.bind(
                _tsdAggregatorConfiguration.getHttpHost(),
                _tsdAggregatorConfiguration.getHttpPort(),
                httpExt.bind$default$3(),
                httpExt.bind$default$4(),
                httpExt.bind$default$5(),
                httpExt.bind$default$6());
        binding.connections().foreach(
                new Foreach<akka.http.Http.IncomingConnection>() {
                    @Override
                    public void each(final akka.http.Http.IncomingConnection input) {
                        input.handleWithAsyncHandler(routes, materializer);
                    }
                },
                materializer);
    }

    private void launchCore() {
        LOGGER.info("Launching core");

        // Create directories
        if (_tsdAggregatorConfiguration.getLogDirectory().mkdirs()) {
            LOGGER.info(String.format(
                    "Created log directory; directory=%s",
                    _tsdAggregatorConfiguration.getLogDirectory()));
        }
        if (_tsdAggregatorConfiguration.getPipelinesDirectory().mkdirs()) {
            LOGGER.info(String.format(
                    "Created pipelines directory; directory=%s",
                    _tsdAggregatorConfiguration.getPipelinesDirectory()));
        }

        // Instantiate the metrics factory
        _metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(
                        new TsdQueryLogSink.Builder()
                                .setPath(_tsdAggregatorConfiguration.getLogDirectory().getAbsolutePath())
                                .setName("tsd-aggregator-query")
                                .build()))
                .build();

        // Instantiate Guice
        _injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    public void configure() {
                        bind(MetricsFactory.class).toInstance(_metricsFactory);
                        for (final Map.Entry<String, MetricsLimiter> entry : _tsdAggregatorConfiguration.getLimiters().entrySet()) {
                            bind(MetricsLimiter.class).annotatedWith(Names.named(entry.getKey())).toInstance(entry.getValue());
                        }
                    }
                });
    }

    private void shutdownPipelines() {
        LOGGER.info("Stopping pipelines");

        for (final Launchable pipeline : _pipelineLaunchables) {
            pipeline.shutdown();
        }
        _pipelineLaunchables.clear();
    }

    private void shutdownLimiters() {
        LOGGER.info("Stopping limiters");

        for (final Launchable limiter : _limiters) {
            limiter.shutdown();
        }

        _limiters.clear();
    }

    private void shutdownAkka() {
        LOGGER.info("Stopping akka");

        if (_actorSystem != null) {
            _actorSystem.shutdown();
            _actorSystem.awaitTermination();
            _actorSystem = null;
        }
    }

    private void shutdownCore() {
        LOGGER.info("Stopping core");
        _injector = null;
        _metricsFactory = null;
    }

    private final TsdAggregatorConfiguration _tsdAggregatorConfiguration;


    private final List<Launchable> _limiters = Lists.newArrayList();
    private final List<Launchable> _pipelineLaunchables = Lists.newArrayList();

    private Injector _injector;
    private ActorSystem _actorSystem;
    private MetricsFactory _metricsFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(TsdAggregator.class);
}
