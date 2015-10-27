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

import com.arpnetworking.test.junitbenchmarks.JsonBenchmarkConsumer;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

/**
 * Perf tests the collectd pipeline with a real collectd sample and a pipeline pulled from production.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
// CHECKSTYLE.OFF: JavadocMethod - Performance tests will be exempted in the next build-resources release
// CHECKSTYLE.OFF: JavadocVariable - Performance tests will be exempted in the next build-resources release
@BenchmarkOptions(callgc = true, benchmarkRounds = 1, warmupRounds = 0)
public class CollectdPipelinePT extends FilePerfTestBase {
    @Test
    public void test() throws
            IOException, InterruptedException, URISyntaxException {
        // Extract the sample file

        final Path gzipPath = Paths.get("build/resources/perf/collectd-sample1.log.gz");
        final FileInputStream fileInputStream = new FileInputStream(gzipPath.toFile());
        final GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
        final Path path = Paths.get("build/tmp/perf/collectd-sample1.log");
        final FileOutputStream outputStream = new FileOutputStream(path.toFile());

        IOUtils.copy(gzipInputStream, outputStream);

        benchmark(new File(Resources.getResource("collectd_sample1_pipeline.json").toURI()), Duration.standardMinutes(20));
    }

    //CHECKSTYLE.OFF: VisibilityModifier - Needs to be public for it to work
    @Rule
    public final TestRule _benchmarkRule = new BenchmarkRule(
            new JsonBenchmarkConsumer(Paths.get("build/reports/perf/benchmark-collectd-tsdagg.json")));
    //CHECKSTYLE.ON: VisibilityModifier

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationPipelinePT.class);
}
// CHECKSTYLE.ON: JavadocVariable
// CHECKSTYLE.ON: JavadocMethod
