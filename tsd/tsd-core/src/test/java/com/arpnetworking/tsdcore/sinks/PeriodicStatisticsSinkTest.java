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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.test.TestBeanFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * Tests for the <code>PeriodicStatisticsSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class PeriodicStatisticsSinkTest {

    @Before
    public void before() {
        _mockMetrics = Mockito.mock(Metrics.class);
        _mockMetricsFactory = Mockito.mock(MetricsFactory.class);
        Mockito.doReturn(_mockMetrics).when(_mockMetricsFactory).create();
        _statisticsSinkBuilder = new PeriodicStatisticsSink.Builder()
                .setName("periodic_statistics_sink_test")
                .setMetricsFactory(_mockMetricsFactory)
                .setIntervalInSeconds(Long.valueOf(60));
    }

    @Test
    public void testFlushOnClose() {
        final Sink statisticsSink = _statisticsSinkBuilder.build();
        Mockito.verify(_mockMetricsFactory).create();
        Mockito.verify(_mockMetrics).resetCounter(COUNTER_NAME);

        statisticsSink.close();
        Mockito.verifyNoMoreInteractions(_mockMetricsFactory);
        Mockito.verify(_mockMetrics).close();
    }

    @Test
    public void testPeriodicFlush() throws InterruptedException {
        final long intervalInSeconds = 3;
        final Sink statisticsSink = _statisticsSinkBuilder
                .setIntervalInSeconds(Long.valueOf(intervalInSeconds))
                .build();
        Mockito.verify(_mockMetricsFactory).create();
        Mockito.verify(_mockMetrics).resetCounter(COUNTER_NAME);

        statisticsSink.recordAggregateData(Collections.singletonList(TestBeanFactory.createAggregatedData()));
        Mockito.verify(_mockMetrics).incrementCounter(COUNTER_NAME, 1);
        Thread.sleep((intervalInSeconds + 1) * 1000);

        Mockito.verify(_mockMetrics).close();
        Mockito.verify(_mockMetricsFactory, Mockito.times(2)).create();
        Mockito.verify(_mockMetrics, Mockito.times(2)).resetCounter(COUNTER_NAME);

        statisticsSink.close();
        Mockito.verify(_mockMetrics, Mockito.times(2)).close();
    }

    @Test
    public void testRecordProcessedAggregateData() {
        final Sink statisticsSink = _statisticsSinkBuilder.build();
        Mockito.verify(_mockMetricsFactory).create();
        Mockito.verify(_mockMetrics).resetCounter(COUNTER_NAME);

        statisticsSink.recordAggregateData(Collections.singletonList(TestBeanFactory.createAggregatedData()));
        Mockito.verify(_mockMetrics).incrementCounter(COUNTER_NAME, 1);
        statisticsSink.close();
        Mockito.verify(_mockMetrics).close();
    }

    private PeriodicStatisticsSink.Builder _statisticsSinkBuilder;
    private Metrics _mockMetrics;
    private MetricsFactory _mockMetricsFactory;

    private static final String COUNTER_NAME = "Sinks/PeriodicStatisticsSink/periodic_statistics_sink_test/AggregatedData";
}
