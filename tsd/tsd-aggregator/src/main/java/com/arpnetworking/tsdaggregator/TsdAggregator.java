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

import ch.qos.logback.classic.LoggerContext;

import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration;
import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration.PipelineConfigurationFactory;
import com.arpnetworking.tsdaggregator.configuration.TsdAggregatorConfiguration;
import com.arpnetworking.tsdcore.exceptions.ConfigurationException;
import com.arpnetworking.tsdcore.sinks.MultiSink;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.utility.DefaultHostResolver;
import com.arpnetworking.utility.HostResolver;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class containing entry point for Time Series Data (TSD) Aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TsdAggregator implements Runnable {

    /**
     * Entry point for Time Series Data (TSD) Aggregator.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Starting tsd-aggregator");

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
        new TsdAggregator(Arrays.asList(args)).run();

        LOGGER.info("Shutting down tsd-aggregator");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            // Configure and launch
            configure();
            launchPipelines();

            // Wait until killed
            while (!_processors.isEmpty() || !_sinks.isEmpty() || !_sources.isEmpty()) {
                try {
                    Thread.sleep(30000);
                } catch (final InterruptedException e) {
                    break;
                }
            }
        } finally {
            // Shutdown
            shutdownPipelines();
        }
    }

    private void shutdownPipelines() {
        LOGGER.info("Stopping pipelines");

        for (final Source source : _sources) {
            source.stop();
        }
        for (final LineProcessor processor : _processors) {
            processor.closeAggregations();
            processor.shutdown();
        }
        for (final Sink sink : _sinks) {
            sink.close();
        }
    }

    private void launchPipelines() {
        LOGGER.info("Launching pipelines");

        String hostName = null;
        try {
            hostName = HOST_RESOLVER.getLocalHostName();
        } catch (final UnknownHostException e) {
            LOGGER.error("Failed to determine host name", e);
            Throwables.propagate(e);
        }

        for (final PipelineConfiguration pipelineConfiguration : _pipelineConfigurations) {
            LOGGER.debug(String.format("Launching pipeline; configuration=%s", pipelineConfiguration.toString()));

            // TODO(vkoskela): Refactor me [MAI-?]
            final Sink rootSink = new MultiSink.Builder()
                    .setName(pipelineConfiguration.getName())
                    .setSinks(pipelineConfiguration.getSinks())
                    .build();
            _sinks.add(rootSink);

            final LineProcessor processor = new LineProcessor(
                    pipelineConfiguration.getTimerStatistic(),
                    pipelineConfiguration.getCounterStatistic(),
                    pipelineConfiguration.getGaugeStatistic(),
                    hostName,
                    pipelineConfiguration.getServiceName(),
                    pipelineConfiguration.getPeriods(),
                    rootSink);
            _processors.add(processor);

            for (final Source source : pipelineConfiguration.getSources()) {
                source.attach(processor);
                source.start();
                _sources.add(source);
            }
            // ** End Refactor **
        }
    }

    private void configure() {
        LOGGER.info("Configuring tsd-aggregator");

        // Create the configuration for the tsd aggregator
        try {
            _configuration = TsdAggregatorConfiguration.create(_arguments);
        } catch (final ConfigurationException e) {
            LOGGER.error("Failed to load configuration", e);
            Throwables.propagate(e);
        }
        if (_configuration.getLogDirectory().mkdirs()) {
            LOGGER.info(String.format("Created log directory; directory=%s", _configuration.getLogDirectory()));
        }
        if (_configuration.getPipelinesDirectory().mkdirs()) {
            LOGGER.info(String.format("Created pipelines directory; directory=%s", _configuration.getPipelinesDirectory()));
        }

        // Instantiate the metrics factory
        _metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(
                        new TsdQueryLogSink.Builder()
                                .setPath(_configuration.getLogDirectory().getAbsolutePath())
                                .setName("tsd-aggregator-query")
                                .build()))
                .build();

        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    public void configure() {
                        bind(TsdAggregatorConfiguration.class).toInstance(_configuration);
                        bind(MetricsFactory.class).toInstance(_metricsFactory);
                    }
                },
                new PipelineConfiguration.Module());

        try {
            for (final URI uri : toUriSet(_configuration.getPipelinesDirectory().listFiles())) {
                LOGGER.info(String.format("Configuring pipeline; uri=%s", uri));
                final PipelineConfigurationFactory factory = injector.getInstance(PipelineConfigurationFactory.class);
                _pipelineConfigurations.add(factory.create(uri));
            }
        } catch (final ConfigurationException e) {
            LOGGER.error("Failed to load pipeline configuration", e);
            Throwables.propagate(e);
        }
    }

    private static Set<URI> toUriSet(final File[] files) {
        final Set<URI> uris = Sets.newHashSetWithExpectedSize(files.length);
        for (final File file : files) {
            uris.add(file.toURI());
        }
        return uris;
    }

    private TsdAggregator(final List<String> arguments) {
        _arguments = arguments;
    }

    private final List<String> _arguments;
    private final List<LineProcessor> _processors = Lists.newArrayList();;
    private final List<Sink> _sinks = Lists.newArrayList();;
    private final List<Source> _sources = Lists.newArrayList();
    private final List<PipelineConfiguration> _pipelineConfigurations = Lists.newArrayList();

    // TODO(vkoskela): Refactor to make these final [MAI-?]
    private TsdAggregatorConfiguration _configuration;
    private MetricsFactory _metricsFactory;

    private static final HostResolver HOST_RESOLVER = new DefaultHostResolver();
    private static final Logger LOGGER = LoggerFactory.getLogger(TsdAggregator.class);
}
