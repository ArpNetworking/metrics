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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>FilteringSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class FilteringSinkTest {

    @Before
    public void before() {
        _mockSink = Mockito.mock(Sink.class);
        _sinkBuilder = new FilteringSink.Builder()
                .setName("filtering_sink_test")
                .setSink(_mockSink);
    }

    @Test
    public void testIncludeByDefault() {
        final Sink sink = _sinkBuilder.build();
        final List<AggregatedData> data = Collections.singletonList(TestBeanFactory.createAggregatedData());
        sink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_mockSink).recordAggregateData(
                Matchers.eq(data),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testExcludeOverDefault() {
        final Sink sink = _sinkBuilder
                .setExcludeFilters(Collections.singletonList(".*MATCHES HERE.*"))
                .build();
        final List<AggregatedData> data = Collections.singletonList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setMetric("Metric name MATCHES HERE for exclusion")
                                .build())
                        .build());
        sink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_mockSink, Mockito.never()).recordAggregateData(
                Matchers.anyListOf(AggregatedData.class),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testIncludeOverExclude() {
        final Sink sink = _sinkBuilder
                .setExcludeFilters(Collections.singletonList(".*MATCHES HERE.*"))
                .setIncludeFilters(Collections.singletonList(".*for inclusion.*"))
                .build();
        final List<AggregatedData> data = Collections.singletonList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setMetric("Metric name MATCHES HERE for inclusion")
                                .build())
                        .build());
        sink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_mockSink).recordAggregateData(
                Matchers.eq(data),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    private FilteringSink.Builder _sinkBuilder;
    private Sink _mockSink;
}
