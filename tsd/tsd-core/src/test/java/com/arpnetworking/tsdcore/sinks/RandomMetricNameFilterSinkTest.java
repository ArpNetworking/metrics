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
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
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
        final List<AggregatedData> data = Lists.newArrayListWithCapacity(count);
        for (int x = 0; x < count; x++) {
            data.add(TestBeanFactory.createAggregatedData());
        }
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink).recordAggregateData(_aggregatedData.capture(), Mockito.eq(Collections.<Condition>emptyList()));
        final List<AggregatedData> captured = _aggregatedData.getValue();
        Assert.assertThat(captured.size(), Matchers.lessThanOrEqualTo((int) (upperThreshold * count)));
        Assert.assertThat(captured.size(), Matchers.greaterThanOrEqualTo((int) (lowerThreshold * count)));
    }

    @Test
    public void testWorksWithSmallList() {
        final Sink sink = _sinkBuilder.setPassPercent(1).build();
        final List<AggregatedData> data = Collections.singletonList(TestBeanFactory.createAggregatedData());
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink).recordAggregateData(Mockito.anyList(), Mockito.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testClosesWrapped() {
        final Sink sink = _sinkBuilder.setPassPercent(15).build();
        final List<AggregatedData> data = Lists.newArrayListWithCapacity(10);
        for (int x = 0; x < 10; x++) {
            data.add(TestBeanFactory.createAggregatedData());
        }
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink).recordAggregateData(Mockito.anyList(), Mockito.eq(Collections.<Condition>emptyList()));
        sink.close();
        Mockito.verify(_mockSink).close();
    }

    @Captor
    private ArgumentCaptor<List<AggregatedData>> _aggregatedData;
    private RandomMetricNameFilterSink.Builder _sinkBuilder;
    private Sink _mockSink;
}
