/**
 * Copyright 2016 Groupon.com
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

import akka.actor.ActorSystem;
import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Units test for InfluxDbSink.
 *
 * @author Daniel Guerrero (dguerreromartin at groupon dot com)
 */
public class InfluxDbSinkTest {

    private InfluxDbSink.Builder _influxBuilder;
    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.apply();
    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();

    @AfterClass
    public static void afterClass() {
        ACTOR_SYSTEM.shutdown();
    }

    @Test
    public void testSerializeWithTwoValue() throws Exception {

        _influxBuilder = new InfluxDbSink.Builder()
                .setName("monitord_sink_test")
                .setActorSystem(ACTOR_SYSTEM)
                .setUri(URI.create("http://localhost:8888"));


        final String service = "service-testSerializeMerge";
        final String metric = "metric-testSerializeMerge";
        final Period period = Period.minutes(5);
        final String host = "test-host";
        final String cluster = "test-cluster";
        final DateTime dateTime = new DateTime(1456361906636L);
        final ImmutableList<AggregatedData> data = ImmutableList.of(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("count"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(50d).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("mean"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(0.2).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build());
        final InfluxDbSink influxDbSink = _influxBuilder.build();
        final PeriodicData periodicData = TestBeanFactory.createPeriodicDataBuilder()
                .setData(data)
                .setStart(dateTime)
                .setDimensions(ImmutableMap.of("host", host))
                .build();
        final Collection<byte[]> results = influxDbSink.serialize(periodicData);
        Assert.assertEquals(1, results.size());
        final String expectedResponse =
            "PT5M.metric-testSerializeMerge,cluster=test-cluster,service=service-testSerializeMerge,host=test-host "
                + "mean=0.2,count=50.0 1456361906636";
        Assert.assertArrayEquals(expectedResponse.getBytes(StandardCharsets.UTF_8), Iterables.getFirst(results, null));

    }

    @Test
    public void testSerializeWithTwoMetrics() throws Exception {

        _influxBuilder = new InfluxDbSink.Builder()
                .setName("monitord_sink_test")
                .setActorSystem(ACTOR_SYSTEM)
                .setUri(URI.create("http://localhost:8888"));


        final String service = "service-testSerializeMerge";
        final String metric = "metric-testSerializeMerge";
        final String metric2 = "metric-testSerializeMerge2";
        final Period period = Period.minutes(5);
        final String host = "test-host";
        final String cluster = "test-cluster";
        final DateTime dateTime = new DateTime(1456361906636L);
        final ImmutableList<AggregatedData> data = ImmutableList.of(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("count"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(50d).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric2)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("count"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(0.2).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build());
        final InfluxDbSink influxDbSink = _influxBuilder.build();
        final PeriodicData periodicData = TestBeanFactory.createPeriodicDataBuilder()
                .setData(data)
                .setStart(dateTime)
                .setDimensions(ImmutableMap.of("host", host))
                .build();
        final Collection<byte[]> results = influxDbSink.serialize(periodicData);
        Assert.assertEquals(1, results.size());
        final String expectedResponse =
            "PT5M.metric-testSerializeMerge2,cluster=test-cluster,service=service-testSerializeMerge,host=test-host "
                + "count=0.2 1456361906636\n"
            + "PT5M.metric-testSerializeMerge,cluster=test-cluster,service=service-testSerializeMerge,host=test-host "
                + "count=50.0 1456361906636";
        Assert.assertArrayEquals(expectedResponse.getBytes(StandardCharsets.UTF_8), Iterables.getFirst(results, null));
    }

    @Test
    public void testSerializeEscapeCharacters() throws Exception {

        _influxBuilder = new InfluxDbSink.Builder()
                .setName("monitord_sink_test")
                .setActorSystem(ACTOR_SYSTEM)
                .setUri(URI.create("http://localhost:8888"));


        final String service = "service test,Serialize=Merge";
        final String metric = "metric test,Serialize=Merge";
        final Period period = Period.minutes(5);
        final String host = "test host";
        final String cluster = "test cluster";
        final DateTime dateTime = new DateTime(1456361906636L);
        final ImmutableList<AggregatedData> data = ImmutableList.of(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("count"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(50d).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build(),
                TestBeanFactory.createAggregatedDataBuilder()
                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                .setService(service)
                                .setCluster(cluster)
                                .setMetric(metric)
                                .setStatistic(STATISTIC_FACTORY.getStatistic("mean"))
                                .build())
                        .setValue(
                                TestBeanFactory.createSampleBuilder().setValue(0.2).build()
                        )
                        .setPeriod(period)
                        .setStart(dateTime)
                        .build());
        final InfluxDbSink influxDbSink = _influxBuilder.build();
        final PeriodicData periodicData = TestBeanFactory.createPeriodicDataBuilder()
                .setData(data)
                .setStart(dateTime)
                .setDimensions(ImmutableMap.of("host", host))
                .build();
        final Collection<byte[]> results = influxDbSink.serialize(periodicData);
        Assert.assertEquals(1, results.size());
        final String expectedResponse =
            "PT5M.metric\\ test\\,Serialize_Merge,cluster=test\\ cluster,service=service\\ test\\,Serialize_Merge,"
                + "host=test\\ host mean=0.2,count=50.0 1456361906636";
        Assert.assertArrayEquals(expectedResponse.getBytes(StandardCharsets.UTF_8), Iterables.getFirst(results, null));

    }

}
