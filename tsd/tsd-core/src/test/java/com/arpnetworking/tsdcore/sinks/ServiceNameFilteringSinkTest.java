/**
 * Copyright 2015 Groupon.com
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
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * Tests for the <code>FilteringSink</code> class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ServiceNameFilteringSinkTest {

    @Before
    public void before() {
        _mockSink = Mockito.mock(Sink.class);
        _sinkBuilder = new ServiceNameFilteringSink.Builder()
                .setName("filtering_sink_test")
                .setSink(_mockSink);
    }

    @Test
    public void testIncludeByDefault() {
        final Sink sink = _sinkBuilder.build();
        final PeriodicData data = TestBeanFactory.createPeriodicData();
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink).recordAggregateData(Matchers.eq(data));
    }

    @Test
    public void testExcludeOverDefault() {
        final Sink sink = _sinkBuilder
                .setExcludeFilters(Collections.singletonList(".*SVCMATCH.*"))
                .build();
        final PeriodicData data = TestBeanFactory.createPeriodicDataBuilder()
                .setData(
                        ImmutableList.of(
                                TestBeanFactory.createAggregatedDataBuilder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setMetric("Metric name does not match for exclusion")
                                                .setService("SVCMATCH_Prod")
                                                .build())
                                        .build()))
                .build();
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink, Mockito.never()).recordAggregateData(Matchers.any(PeriodicData.class));
    }

    @Test
    public void testIncludeOverExclude() {
        final Sink sink = _sinkBuilder
                .setExcludeFilters(Collections.singletonList(".*MATCHES HERE.*"))
                .setIncludeFilters(Collections.singletonList(".*for inclusion.*"))
                .build();
        final PeriodicData data = TestBeanFactory.createPeriodicDataBuilder()
                .setData(
                        ImmutableList.of(
                                TestBeanFactory.createAggregatedDataBuilder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setMetric("Metric name")
                                                .setService("service MATCHES HERE for inclusion")
                                                .build())
                                        .build()))
                .build();
        sink.recordAggregateData(data);
        Mockito.verify(_mockSink).recordAggregateData(Matchers.eq(data));
    }

    private ServiceNameFilteringSink.Builder _sinkBuilder;
    private Sink _mockSink;
}
