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
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class containing entry point for Time Series Data (TSD) Aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class Main implements Launchable {

    /**
     * Entry point for Time Series Data (TSD) Aggregator.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info().setMessage("Launching tsd-aggregator").log();

        // Global initialization
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) ->
                        LOGGER.error()
                                .setMessage("Unhandled exception")
                                .setThrowable(throwable)
                                .log());

        System.setProperty("org.vertx.logger-delegate-factory-class-name", "org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory");

        // Run the tsd aggregator
        if (args.length != 1) {
            throw new RuntimeException("No configuration file specified");
        }
        LOGGER.debug()
                .setMessage("Loading configuration")
                .addData("file", args[0])
                .log();

        final File configurationFile = new File(args[0]);
        final Configurator<Main, TsdAggregatorConfiguration> configurator =
                new Configurator<>(Main::new, TsdAggregatorConfiguration.class);
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info().setMessage("Stopping tsd-aggregator").log();
            configuration.shutdown();
            configurator.shutdown();

            LOGGER.info().setMessage("Exiting tsd-aggregator").log();
            final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            context.stop();
        }));
    }

    /**
     * Public constructor.
     *
     * @param configuration Instance of <code>TsdAggregatorConfiguration</code>.
     */
    public Main(final TsdAggregatorConfiguration configuration) {
        _configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch() {
        final Injector injector = launchGuice();
        launchAkka(injector);
        launchLimiters();
        launchPipelines(injector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        shutdownPipelines();
        shutdownLimiters();
        shutdownAkka();
        shutdownGuice();
    }

    private void launchPipelines(final Injector injector) {
        LOGGER.info().setMessage("Launching pipelines").log();

        final ObjectMapper objectMapper = PipelineConfiguration.createObjectMapper(injector);

        final File[] files = MoreObjects.firstNonNull(
                _configuration.getPipelinesDirectory().listFiles(),
                new File[0]);

        for (final File configurationFile : files) {
            LOGGER.debug()
                    .setMessage("Creating pipeline")
                    .addData("configuration", configurationFile)
                    .log();

            final Configurator<Pipeline, PipelineConfiguration> pipelineConfigurator =
                    new Configurator<>(Pipeline::new, PipelineConfiguration.class);
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

            LOGGER.debug()
                    .setMessage("Launching pipeline")
                    .addData("pipeline", pipelineConfiguration)
                    .log();
            pipelineConfiguration.launch();
            _pipelineLaunchables.add(pipelineConfigurator);
            _pipelineLaunchables.add(pipelineConfiguration);
        }

        if (_pipelineLaunchables.isEmpty()) {
            LOGGER.warn()
                    .setMessage("No pipelines found in pipelines directory")
                    .addData("path", _configuration.getPipelinesDirectory().getAbsolutePath())
                    .log();
        }
    }

    private void launchLimiters() {
        LOGGER.info().setMessage("Launching limiters").log();

        _limiters.addAll(_configuration.getLimiters().values());
        for (final Launchable limiter : _limiters) {
            LOGGER.debug()
                    .setMessage("Launching limiter")
                    .addData("limiter", limiter)
                    .log();
            limiter.launch();
        }
    }

    private void launchAkka(final Injector injector) {
        LOGGER.info().setMessage("Launching akka").log();

        // Initialize Akka
        final Config akkaConfiguration = ConfigFactory.parseMap(_configuration.getAkkaConfiguration());
        _actorSystem = ActorSystem.create("TsdAggregator", ConfigFactory.load(akkaConfiguration));
        final Routes routes = new Routes(_actorSystem, injector.getInstance(MetricsFactory.class));

        // Create the status actor
        _actorSystem.actorOf(Props.create(Status.class), "status");

        final MaterializerSettings materializerSettings = MaterializerSettings.create(_actorSystem);
        final FlowMaterializer materializer = FlowMaterializer.create(materializerSettings, _actorSystem);

        // Create and bind Http server
        final HttpExt httpExt = (HttpExt) Http.get(_actorSystem);
        final akka.http.Http.ServerBinding binding = httpExt.bind(
                _configuration.getHttpHost(),
                _configuration.getHttpPort(),
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

    private Injector launchGuice() {
        LOGGER.info().setMessage("Launching guice").log();

        // Create directories
        if (_configuration.getLogDirectory().mkdirs()) {
            LOGGER.info()
                    .setMessage("Created log directory")
                    .addData("directory", _configuration.getLogDirectory())
                    .log();
        }
        if (_configuration.getPipelinesDirectory().mkdirs()) {
            LOGGER.info()
                    .setMessage("Created pipelines directory")
                    .addData("directory", _configuration.getPipelinesDirectory())
                    .log();
        }

        // Instantiate the metrics factory
        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(
                        new TsdQueryLogSink.Builder()
                                .setPath(_configuration.getLogDirectory().getAbsolutePath())
                                .setName("tsd-aggregator-query")
                                .build()))
                .build();

        // Instantiate Guice
        // TODO(vkoskela): Move limiter registration to launchLimiters [MAI-466]
        return Guice.createInjector(
                new AbstractModule() {
                    @Override
                    public void configure() {
                        bind(MetricsFactory.class).toInstance(metricsFactory);
                        for (final Map.Entry<String, MetricsLimiter> entry : _configuration.getLimiters().entrySet()) {
                            bind(MetricsLimiter.class).annotatedWith(Names.named(entry.getKey())).toInstance(entry.getValue());
                        }
                    }
                });
    }

    private void shutdownPipelines() {
        LOGGER.info().setMessage("Stopping pipelines").log();

        for (final Launchable pipeline : _pipelineLaunchables) {
            LOGGER.debug()
                    .setMessage("Stopping pipeline")
                    .addData("pipeline", pipeline)
                    .log();
            pipeline.shutdown();
        }
        _pipelineLaunchables.clear();
    }

    private void shutdownLimiters() {
        LOGGER.info().setMessage("Stopping limiters").log();

        for (final Launchable limiter : _limiters) {
            LOGGER.debug()
                    .setMessage("Stopping limiter")
                    .addData("limiter", limiter)
                    .log();
            limiter.shutdown();
        }
        _limiters.clear();
    }

    private void shutdownAkka() {
        LOGGER.info().setMessage("Stopping akka").log();

        if (_actorSystem != null) {
            _actorSystem.shutdown();
            _actorSystem.awaitTermination();
            _actorSystem = null;
        }
    }

    private void shutdownGuice() {
        LOGGER.info().setMessage("Stopping guice").log();
    }

    private final TsdAggregatorConfiguration _configuration;

    private final List<Launchable> _limiters = Collections.synchronizedList(Lists.newArrayList());
    private final List<Launchable> _pipelineLaunchables = Collections.synchronizedList(Lists.newArrayList());

    private volatile ActorSystem _actorSystem;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
}
