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
package com.arpnetworking.tsdcore.limiter.legacy;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.CountStatistic;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

/**
 * Common constants and fixtures for MetricsLimiter tests.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
public class MetricsLimiterTestBase {

    /**
     * Execute before each test.
     *
     * @throws Exception If setup fails.
     */
    @Before
    public void setUpBase() throws Exception {
        Files.deleteIfExists(STATE_FILE);
        _now = System.currentTimeMillis();
    }

    /**
     * Execute after each test.
     *
     * @throws Exception If tear down fails.
     */
    @After
    public void tearDownBase() throws Exception {
        Files.deleteIfExists(STATE_FILE);
    }

    /**
     * Create a <code>MetricsLimiter</code> suitable for testing.
     *
     * TODO(vkoskela): This should be refactored into a TestBeanFactory [MAI-168].
     *
     * @return New instance of <code>MetricsLimiter</code>.
     */
    public static LegacyMetricsLimiter testLimiter() {
        return new LegacyMetricsLimiter.Builder()
                .setEnableStateAutoWriter(Boolean.FALSE)
                .setMaxAggregations(Long.MAX_VALUE)
                .setStateManagerBuilder(new MetricsLimiterStateManager.Builder()
                        .setStateFile(Paths.get("/dev/null"))
                        .setStateFileFlushInterval(Duration.standardDays(365 * 100)) // ~100
                                                                                      // years
                ).build();
    }

    /**
     * Assert that the state file is created.
     */
    protected static void assertStateFileAppears() {
        int tries = 250;
        while (tries > 0 && !Files.exists(STATE_FILE)) {
            try {
                Thread.sleep(2);
            } catch (final InterruptedException e) {
                Assert.fail("Interrupted waiting for state file to appear");
            }
            tries -= 1;
        }
        Assert.assertTrue("File hasn't appeared after 500ms", tries > 0);
    }

    /**
     * Create the metric key for an instance of <code>AggregatedData</code>.
     *
     * @param data Instance of <code>AggregatedData</code>.
     * @return The metric key.
     */
    protected String createName(final AggregatedData data) {
        return data.getFQDSN().getCluster() + "-"
                + data.getFQDSN().getService() + "-"
                + data.getFQDSN().getMetric() + "-"
                + data.getFQDSN().getStatistic().getName() + "-"
                + data.getPeriod();
    }

    protected long _now;

    protected static final Duration WEEK = Duration.standardDays(7);
    protected static final DateTime WEEK_0 = new DateTime(1990, 1, 1, 0, 0);
    protected static final DateTime WEEK_1 = WEEK_0.plus(WEEK);
    protected static final DateTime WEEK_2 = WEEK_1.plus(WEEK);
    protected static final DateTime WEEK_3 = WEEK_2.plus(WEEK);
    protected static final Path STATE_FILE = Paths.get("build/tmp/metrics-limiter-test");

    protected static final Sink NULL_PUBLISHER = new Sink() {
        @Override
        public void recordAggregateData(final Collection<AggregatedData> data) {}

        @Override
        public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {}

        @Override
        public void close() {}
    };

    protected static final String METRIC_A = "metric.A";
    // 3 aggregations x 2 stats = 6 "aggregations"
    protected static final AggregatedData TSD_1A = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new MeanStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(5))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
    protected static final AggregatedData TSD_1B = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new MeanStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(15))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
    protected static final AggregatedData TSD_1C = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new MeanStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(60))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
    protected static final AggregatedData TSD_1D = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new CountStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(5))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
    protected static final AggregatedData TSD_1E = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new CountStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(15))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
    protected static final AggregatedData TSD_1F = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("navy")
                    .setCluster("cluster")
                    .setStatistic(new CountStatistic())
                    .setMetric(METRIC_A)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.minutes(60))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();

    protected static final String METRIC_B = "metric.B";
    // 1 aggregation x 1 stat = 1 "aggregation"
    protected static final AggregatedData TSD_2A = new AggregatedData.Builder()
            .setFQDSN(new FQDSN.Builder()
                    .setService("army")
                    .setCluster("cluster")
                    .setStatistic(new TP99Statistic())
                    .setMetric(METRIC_B)
                    .build())
            .setHost("example.com")
            .setPeriod(Period.hours(1))
            .setStart(new DateTime())
            .setPopulationSize(1L)
            .setSamples(Collections.<Quantity>emptyList())
            .setValue(TestBeanFactory.createSample())
            .build();
}
