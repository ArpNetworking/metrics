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

import com.arpnetworking.metrics.generator.util.TestFileGenerator;
import com.arpnetworking.test.junitbenchmarks.JsonBenchmarkConsumer;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Perf tests that cover reading from a file and computing the aggregates
 * from it.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
// CHECKSTYLE.OFF: JavadocMethod - Performance tests will be exempted in the next build-resources release
// CHECKSTYLE.OFF: JavadocVariable - Performance tests will be exempted in the next build-resources release
@RunWith(JUnitParamsRunner.class)
@BenchmarkOptions(callgc = true, benchmarkRounds = 1, warmupRounds = 0)
public class ApplicationPipelinePT extends FilePerfTestBase {
    /**
     * Generates the parameters for the 3-dimensional perf tests.
     *
     * @return The needed arguments.
     */
    public Object[] createParameters() {
        final List<Integer> metricSamplesPerUOW = Lists.newArrayList(1, 5, 25);
        final List<Integer> uowPerInterval = Lists.newArrayList(10000, 30000, 90000);
        final List<Integer> metricNamesPerUOW = Lists.newArrayList(1, 10, 100);

        final ArrayList<Object> params = Lists.newArrayList();
        for (final Integer uowCount : uowPerInterval) {
            for (final Integer namesCount : metricNamesPerUOW) {
                for (final Integer samplesCount : metricSamplesPerUOW) {
                    params.add(Lists.newArrayList(uowCount, namesCount, samplesCount).toArray());
                }
            }
        }

        return params.toArray();
    }

    @Test
    @Parameters(method = "createParameters")
    public void test(final int uowCount, final int namesCount, final int samplesCount) throws
            IOException, InterruptedException, URISyntaxException {
        final RandomGenerator random = new MersenneTwister(1298); //Just pick a number as the seed.
        final Path path = Paths.get("build/tmp/perf/application-generated-sample.log");

        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final DateTime stop = start.plusMinutes(10);
        final TestFileGenerator generator = new TestFileGenerator.Builder()
                .setRandom(random)
                .setUnitOfWorkCount(uowCount)
                .setNamesCount(namesCount)
                .setSamplesCount(samplesCount)
                .setStartTime(start)
                .setEndTime(stop)
                .setFileName(path)
                .build();
        generator.generate();

        benchmark(new File(Resources.getResource("application_perf_pipeline.json").toURI()), Duration.standardMinutes(90));
    }

    @Rule
    public static final TestRule BENCHMARK_RULE = new BenchmarkRule(
            new JsonBenchmarkConsumer(Paths.get("build/reports/perf/benchmark-tsdagg.json")));

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationPipelinePT.class);
}
// CHECKSTYLE.ON: JavadocVariable
// CHECKSTYLE.ON: JavadocMethod
