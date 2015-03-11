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

package com.arpnetworking.tsdaggregator.perf;

import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.configuration.jackson.StaticConfiguration;
import com.arpnetworking.metrics.generator.util.TestFileGenerator;
import com.arpnetworking.tsdaggregator.Pipeline;
import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Serves as the base for performance tests that run a file through a tsd aggregator pipeline.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class FilePerfTestBase {

    /**
     * Runs a test.
     *
     * @param pipelineConfigurationFile Pipeline configuration file.
     * @param duration Timeout period.
     */
    protected void benchmark(final File pipelineConfigurationFile, final Duration duration) {
        LOGGER.debug(String.format("Launching pipeline; configuration=%s", pipelineConfigurationFile));

        // Create custom "canary" sink
        final CountDownLatch latch = new CountDownLatch(1);
        final Stopwatch timer = Stopwatch.createUnstarted();
        final ListeningSink sink = new ListeningSink(new Function<Collection<AggregatedData>, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable final Collection<AggregatedData> input) {
                if (input != null) {
                    final AggregatedData datum = Iterables.getFirst(input, null);
                    if (datum != null && TestFileGenerator.CANARY.equals(datum.getFQDSN().getMetric()) && timer.isRunning()) {
                        LOGGER.info(String.format(
                                "Performance test result; test=%s, seconds=%s",
                                this.getClass(),
                                timer.elapsed(TimeUnit.SECONDS)));
                        timer.stop();
                        latch.countDown();
                    }
                }
                return null;
            }
        });

        // Load the specified stock configuration
        final PipelineConfiguration stockPipelineConfiguration = new StaticConfiguration.Builder()
                .addSource(new JsonNodeFileSource.Builder()
                        .setFile(pipelineConfigurationFile)
                        .build())
                .setObjectMapper(PipelineConfiguration.createObjectMapper(_injector))
                .build()
                .getRequiredAs(PipelineConfiguration.class);

        // Add the custom "canary" sink
        final List<Sink> benchmarkSinks = Lists.newArrayList(stockPipelineConfiguration.getSinks());
        benchmarkSinks.add(sink);

        // Create the custom configuration
        final PipelineConfiguration benchmarkPipelineConfiguration =
                OvalBuilder.<PipelineConfiguration, PipelineConfiguration.Builder>clone(stockPipelineConfiguration)
                        .setSinks(benchmarkSinks)
                        .build();

        // Instantiate the pipeline
        final Pipeline pipeline = new Pipeline(benchmarkPipelineConfiguration);

        // Execute the pipeline until the canary flies the coop
        try {
            timer.start();
            pipeline.launch();

            if (!latch.await(duration.getMillis(), TimeUnit.MILLISECONDS)) {
                LOGGER.error("Test timed out");
                throw new RuntimeException("Test timed out");
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException("Test interrupted");
        } finally {
            pipeline.shutdown();
        }
    }

    private final Injector _injector = Guice.createInjector();

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePerfTestBase.class);
}
