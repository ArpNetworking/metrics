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

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * Tests for the {@link RandomMetricNameFilterSink} class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class RandomMetricNameFilterSinkTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        _mockSink = Mockito.mock(Sink.class);
        _sinkBuilder = new RandomMetricNameFilterSink.Builder()
                .setName("random_filtering_sink_test")
                .setSink(_mockSink);
    }

    @Test
    public void testTargetInclude() {
        final int limit = 15;
        final Sink sink = _sinkBuilder.setPassPercent(limit).build();
        final double target = (double) limit / 100;
        final double spread = 0.01;  // Make sure we're within 1%
        final double upperThreshold = target + spread;
        final double lowerThreshold = target - spread;
        final int count = 10000;
        final ImmutableList.Builder<AggregatedData> dataBuilder = ImmutableList.builder();
        for (int x = 0; x < count; x++) {
            final String cluster = "cluster" + x;
            final String metric = "metric" + x;
            final String service = "service" + x;
            final Statistic statistic = MEAN_STATISTIC;
            final Period period = Period.minutes(1);
            final FQDSN fqdsn = TestBeanFactory.createFQDSNBuilder()
                    .setCluster(cluster)
                    .setMetric(metric)
                    .setService(service)
                    .setStatistic(statistic)
                    .build();
            dataBuilder.add(
                    TestBeanFactory.createAggregatedDataBuilder()
                            .setFQDSN(fqdsn)
                            .setPeriod(period)
                            .build());
        }
        sink.recordAggregateData(
                TestBeanFactory.createPeriodicDataBuilder()
                        .setData(dataBuilder.build())
                        .build());
        Mockito.verify(_mockSink).recordAggregateData(_actualPeriodicData.capture());
        final List<AggregatedData> captured = _actualPeriodicData.getValue().getData();
        Assert.assertThat(captured.size(), Matchers.lessThanOrEqualTo((int) (upperThreshold * count)));
        Assert.assertThat(captured.size(), Matchers.greaterThanOrEqualTo((int) (lowerThreshold * count)));
    }

    @Test
    public void testClosesWrapped() {
        final Sink sink = _sinkBuilder.setPassPercent(15).build();
        sink.close();
        Mockito.verify(_mockSink).close();
    }

    @Captor
    private ArgumentCaptor<PeriodicData> _actualPeriodicData;
    private RandomMetricNameFilterSink.Builder _sinkBuilder;
    private Sink _mockSink;

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic MEAN_STATISTIC = STATISTIC_FACTORY.getStatistic("mean");
}
