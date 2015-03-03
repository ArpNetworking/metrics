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

package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.model.DefaultMetric;
import com.arpnetworking.tsdaggregator.model.DefaultRecord;
import com.arpnetworking.tsdaggregator.model.MetricType;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.arpnetworking.tsdcore.statistics.MedianStatistic;
import com.arpnetworking.tsdcore.statistics.TP0Statistic;
import com.arpnetworking.tsdcore.statistics.TP100Statistic;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;

/**
 * Tests for the <code>Bucket</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class BucketTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testCounter() {
        final Sink sink = Mockito.mock(Sink.class);
        final DateTime start = DateTime.parse("2015-02-05T00:00:00Z");
        final Bucket bucket = new Bucket.Builder()
                .setSink(sink)
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setCounterStatistics(ImmutableSet.of(new TP0Statistic()))
                .setGaugeStatistics(ImmutableSet.of(new MeanStatistic()))
                .setTimerStatistics(ImmutableSet.of(new TP100Statistic()))
                .build();

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(10)))
                        .setMetrics(ImmutableMap.of(
                                "MyCounter",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.COUNTER)
                                        .setValues(Collections.singletonList(ONE))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(20)))
                        .setMetrics(ImmutableMap.of(
                                "MyCounter",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.COUNTER)
                                        .setValues(Collections.singletonList(TWO))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(30)))
                        .setMetrics(ImmutableMap.of(
                                "MyCounter",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.COUNTER)
                                        .setValues(Collections.singletonList(THREE))
                                        .build()))
                        .build());

        bucket.close();

        final ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture(), Mockito.eq(Collections.<Condition>emptyList()));

        final Collection<AggregatedData> data = dataCaptor.getValue();
        Assert.assertEquals(1, data.size());

        Assert.assertThat(data, Matchers.contains(new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyCounter")
                        .setStatistic(new TP0Statistic())
                        .build())
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setPopulationSize(3L)
                .setSamples(Lists.newArrayList(ONE, TWO, THREE))
                .setValue(ONE)
                .build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGauge() {
        final Sink sink = Mockito.mock(Sink.class);
        final DateTime start = DateTime.parse("2015-02-05T00:00:00Z");
        final Bucket bucket = new Bucket.Builder()
                .setSink(sink)
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setCounterStatistics(ImmutableSet.of(new TP0Statistic()))
                .setGaugeStatistics(ImmutableSet.of(new MeanStatistic()))
                .setTimerStatistics(ImmutableSet.of(new TP100Statistic()))
                .build();

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(10)))
                        .setMetrics(ImmutableMap.of(
                                "MyGauge",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.GAUGE)
                                        .setValues(Collections.singletonList(TWO))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(20)))
                        .setMetrics(ImmutableMap.of(
                                "MyGauge",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.GAUGE)
                                        .setValues(Collections.singletonList(ONE))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(30)))
                        .setMetrics(ImmutableMap.of(
                                "MyGauge",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.GAUGE)
                                        .setValues(Collections.singletonList(THREE))
                                        .build()))
                        .build());

        bucket.close();

        final ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture(), Mockito.eq(Collections.<Condition>emptyList()));

        final Collection<AggregatedData> data = dataCaptor.getValue();
        Assert.assertEquals(1, data.size());

        Assert.assertThat(data, Matchers.contains(new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyGauge")
                        .setStatistic(new MeanStatistic())
                        .build())
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setPopulationSize(3L)
                .setSamples(Lists.newArrayList(TWO, ONE, THREE))
                .setValue(TWO)
                .build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTimer() {
        final Sink sink = Mockito.mock(Sink.class);
        final DateTime start = DateTime.parse("2015-02-05T00:00:00Z");
        final Bucket bucket = new Bucket.Builder()
                .setSink(sink)
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setCounterStatistics(ImmutableSet.of(new TP0Statistic()))
                .setGaugeStatistics(ImmutableSet.of(new MeanStatistic()))
                .setTimerStatistics(ImmutableSet.of(new TP100Statistic()))
                .build();

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(10)))
                        .setMetrics(ImmutableMap.of(
                                "MyTimer",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.TIMER)
                                        .setValues(Collections.singletonList(THREE_SECONDS))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(20)))
                        .setMetrics(ImmutableMap.of(
                                "MyTimer",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.TIMER)
                                        .setValues(Collections.singletonList(TWO_SECONDS))
                                        .build()))
                        .build());

        bucket.add(
                new DefaultRecord.Builder()
                        .setTime(start.plus(Duration.standardSeconds(30)))
                        .setMetrics(ImmutableMap.of(
                                "MyTimer",
                                new DefaultMetric.Builder()
                                        .setType(MetricType.TIMER)
                                        .setValues(Collections.singletonList(ONE_SECOND))
                                        .build()))
                        .build());

        bucket.close();

        final ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture(), Mockito.eq(Collections.<Condition>emptyList()));

        final Collection<AggregatedData> data = dataCaptor.getValue();
        Assert.assertEquals(1, data.size());

        Assert.assertThat(data, Matchers.contains(new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyTimer")
                        .setStatistic(new TP100Statistic())
                        .build())
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setPopulationSize(3L)
                .setSamples(Lists.newArrayList(ONE_SECOND, TWO_SECONDS, THREE_SECONDS))
                .setValue(new Quantity.Builder()
                        .setValue(3000.0)
                        .setUnit(Unit.MILLISECOND)
                        .build())
                .build()));
    }

    @Test
    public void testGetThreshold() {
        final DateTime start = DateTime.parse("2015-02-05T00:00:00Z");
        final Bucket bucket = new Bucket.Builder()
                .setSink(Mockito.mock(Sink.class))
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setStart(start)
                .setPeriod(Period.minutes(1))
                .setCounterStatistics(ImmutableSet.of(new TP0Statistic()))
                .setGaugeStatistics(ImmutableSet.of(new MedianStatistic()))
                .setTimerStatistics(ImmutableSet.of(new TP100Statistic()))
                .build();
        Assert.assertEquals(start.plus(Duration.standardMinutes(1)), bucket.getThreshold());
    }

    @Test
    public void testToString() {
        final String asString = new Bucket.Builder()
                .setSink(Mockito.mock(Sink.class))
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setStart(new DateTime())
                .setPeriod(Period.minutes(1))
                .setCounterStatistics(ImmutableSet.of(new TP0Statistic()))
                .setGaugeStatistics(ImmutableSet.of(new MedianStatistic()))
                .setTimerStatistics(ImmutableSet.of(new TP100Statistic()))
                .build()
                .toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }

    private static final Quantity ONE = new Quantity.Builder().setValue(1.0).build();
    private static final Quantity TWO = new Quantity.Builder().setValue(2.0).build();
    private static final Quantity THREE = new Quantity.Builder().setValue(3.0).build();

    private static final Quantity ONE_SECOND = new Quantity.Builder().setValue(1.0).setUnit(Unit.SECOND).build();
    private static final Quantity TWO_SECONDS = new Quantity.Builder().setValue(2000.0).setUnit(Unit.MILLISECOND).build();
    private static final Quantity THREE_SECONDS = new Quantity.Builder().setValue(3.0).setUnit(Unit.SECOND).build();
}
