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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>MonitordSink</code> class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class MonitordSinkTest {

    @Before
    public void before() {
        _monitordSinkBuilder = new MonitordSink.Builder()
                .setName("monitord_sink_test")
                .setCluster("cluster")
                .setHost("localhost")
                .setUri(URI.create("http://localhost:8888"));
    }

    @Test
    public void testSerializeFilterSubMinuteData() {
        final List<AggregatedData> data = Collections.singletonList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.seconds(1))
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testSerializeFilterOldData() {
        final List<AggregatedData> data = Collections.singletonList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriodStart(new DateTime().minusMinutes(15))
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testSerializeMerge() {
        final String service = "service-testSerializeMerge";
        final String metric = "metric-testSerializeMerge";
        final Period period = Period.minutes(5);
        final List<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric)
                        .setPeriod(period)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric)
                        .setPeriod(period)
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testSerializeNoMergeService() {
        final String service = "service-testSerializeNoMergeService";
        final String metric = "metric-testSerializeNoMergeService";
        final Period period = Period.minutes(5);
        final List<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service + "-1")
                        .setMetric(metric)
                        .setPeriod(period)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service + "-2")
                        .setMetric(metric)
                        .setPeriod(period)
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testSerializeNoMergeMetric() {
        final String service = "service-testSerializeNoMergeMetric";
        final String metric = "metric-testSerializeNoMergeMetric";
        final Period period = Period.minutes(5);
        final List<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric + "-1")
                        .setPeriod(period)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric + "-2")
                        .setPeriod(period)
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testSerializeNoMergePeriod() {
        final String service = "service-testSerializeNoMergePeriod";
        final String metric = "metric-testSerializeNoMergePeriod";
        final List<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric)
                        .setPeriod(Period.minutes(5))
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setService(service)
                        .setMetric(metric)
                        .setPeriod(Period.minutes(1))
                        .build());
        final MonitordSink monitordSink = (MonitordSink) _monitordSinkBuilder.build();
        final Collection<String> results = monitordSink.serialize(data);
        Assert.assertEquals(2, results.size());
    }

    private MonitordSink.Builder _monitordSinkBuilder;
}
