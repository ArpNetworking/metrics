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
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.MetricType;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
                .setSpecifiedCounterStatistics(ImmutableSet.of(MIN_STATISTIC))
                .setSpecifiedGaugeStatistics(ImmutableSet.of(MEAN_STATISTIC))
                .setSpecifiedTimerStatistics(ImmutableSet.of(MAX_STATISTIC))
                .setDependentCounterStatistics(ImmutableSet.of())
                .setDependentGaugeStatistics(ImmutableSet.of(COUNT_STATISTIC, SUM_STATISTIC))
                .setDependentTimerStatistics(ImmutableSet.of())
                .setSpecifiedStatistics(_specifiedStatsCache)
                .setDependentStatistics(_dependentStatsCache)
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

        final ArgumentCaptor<PeriodicData> dataCaptor = ArgumentCaptor.forClass(PeriodicData.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture());

        final Collection<AggregatedData> data = dataCaptor.getValue().getData();
        Assert.assertEquals(1, data.size());

        Assert.assertThat(data, Matchers.contains(new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyCounter")
                        .setStatistic(MIN_STATISTIC)
                        .build())
                .setHost("MyHost")
                .setStart(start)
                .setIsSpecified(true)
                .setPeriod(Period.minutes(1))
                .setPopulationSize(-1L)
                .setSamples(Collections.emptyList())
                .setValue(ONE)
                .build()));
    }

    // CHECKSTYLE.OFF: MethodLength - Mostly data.
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
                .setSpecifiedCounterStatistics(ImmutableSet.of(MIN_STATISTIC))
                .setSpecifiedGaugeStatistics(ImmutableSet.of(MEAN_STATISTIC))
                .setSpecifiedTimerStatistics(ImmutableSet.of(MAX_STATISTIC))
                .setDependentCounterStatistics(ImmutableSet.of())
                .setDependentGaugeStatistics(ImmutableSet.of(COUNT_STATISTIC, SUM_STATISTIC))
                .setDependentTimerStatistics(ImmutableSet.of())
                .setSpecifiedStatistics(_specifiedStatsCache)
                .setDependentStatistics(_dependentStatsCache)
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

        final ArgumentCaptor<PeriodicData> dataCaptor = ArgumentCaptor.forClass(PeriodicData.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture());

        final Collection<AggregatedData> data = dataCaptor.getValue().getData();
        Assert.assertEquals(3, data.size());

        Assert.assertThat(
                data,
                Matchers.containsInAnyOrder(
                        new AggregatedData.Builder()
                                .setFQDSN(new FQDSN.Builder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyGauge")
                                        .setStatistic(MEAN_STATISTIC)
                                        .build())
                                .setHost("MyHost")
                                .setStart(start)
                                .setIsSpecified(true)
                                .setPeriod(Period.minutes(1))
                                .setPopulationSize(-1L)
//                                .setSamples(Collections.singletonList(THREE))
                                .setValue(TWO)
                                .build(),
                        new AggregatedData.Builder()
                                .setFQDSN(new FQDSN.Builder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyGauge")
                                        .setStatistic(SUM_STATISTIC)
                                        .build())
                                .setHost("MyHost")
                                .setStart(start)
                                .setIsSpecified(false)
                                .setPeriod(Period.minutes(1))
                                .setPopulationSize(-1L)
                                .setSamples(Collections.emptyList())
                                .setValue(SIX)
                                .build(),
                        new AggregatedData.Builder()
                                .setFQDSN(new FQDSN.Builder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyGauge")
                                        .setStatistic(COUNT_STATISTIC)
                                        .build())
                                .setHost("MyHost")
                                .setStart(start)
                                .setIsSpecified(false)
                                .setPeriod(Period.minutes(1))
                                .setPopulationSize(-1L)
                                .setSamples(Collections.emptyList())
                                .setValue(THREE)
                                .build()));
    }
    // CHECKSTYLE.ON: MethodLength

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
                .setSpecifiedCounterStatistics(ImmutableSet.of(MIN_STATISTIC))
                .setSpecifiedGaugeStatistics(ImmutableSet.of(MEAN_STATISTIC))
                .setSpecifiedTimerStatistics(ImmutableSet.of(MAX_STATISTIC))
                .setDependentCounterStatistics(ImmutableSet.of())
                .setDependentGaugeStatistics(ImmutableSet.of(COUNT_STATISTIC, SUM_STATISTIC))
                .setDependentTimerStatistics(ImmutableSet.of())
                .setSpecifiedStatistics(_specifiedStatsCache)
                .setDependentStatistics(_dependentStatsCache)
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

        final ArgumentCaptor<PeriodicData> dataCaptor = ArgumentCaptor.forClass(PeriodicData.class);
        Mockito.verify(sink).recordAggregateData(dataCaptor.capture());

        final Collection<AggregatedData> data = dataCaptor.getValue().getData();
        Assert.assertEquals(1, data.size());

        Assert.assertThat(data, Matchers.contains(new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyTimer")
                        .setStatistic(MAX_STATISTIC)
                        .build())
                .setHost("MyHost")
                .setStart(start)
                .setIsSpecified(true)
                .setPeriod(Period.minutes(1))
                .setPopulationSize(-1L)
                .setSamples(Collections.emptyList())
                .setValue(new Quantity.Builder()
                        .setValue(3.0)
                        .setUnit(Unit.SECOND)
                        .build())
                .build()));
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
                .setSpecifiedCounterStatistics(ImmutableSet.of(MIN_STATISTIC))
                .setSpecifiedGaugeStatistics(ImmutableSet.of(MEDIAN_STATISTIC))
                .setSpecifiedTimerStatistics(ImmutableSet.of(MAX_STATISTIC))
                .setDependentCounterStatistics(ImmutableSet.of())
                .setDependentGaugeStatistics(ImmutableSet.of(COUNT_STATISTIC, SUM_STATISTIC))
                .setDependentTimerStatistics(ImmutableSet.of())
                .setSpecifiedStatistics(_specifiedStatsCache)
                .setDependentStatistics(_dependentStatsCache)
                .build()
                .toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }

    private LoadingCache<String, Optional<ImmutableSet<Statistic>>> _specifiedStatsCache = CacheBuilder.newBuilder()
            .build(new AbsentStatisticCacheLoader());

    private LoadingCache<String, Optional<ImmutableSet<Statistic>>> _dependentStatsCache = CacheBuilder.newBuilder()
            .build(new AbsentStatisticCacheLoader());


    private static final Quantity ONE = new Quantity.Builder().setValue(1.0).build();
    private static final Quantity TWO = new Quantity.Builder().setValue(2.0).build();
    private static final Quantity THREE = new Quantity.Builder().setValue(3.0).build();
    private static final Quantity SIX = new Quantity.Builder().setValue(6.0).build();

    private static final Quantity ONE_SECOND = new Quantity.Builder().setValue(1.0).setUnit(Unit.SECOND).build();
    private static final Quantity TWO_SECONDS = new Quantity.Builder().setValue(2000.0).setUnit(Unit.MILLISECOND).build();
    private static final Quantity THREE_SECONDS = new Quantity.Builder().setValue(3.0).setUnit(Unit.SECOND).build();

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic MIN_STATISTIC = STATISTIC_FACTORY.getStatistic("min");
    private static final Statistic MEAN_STATISTIC = STATISTIC_FACTORY.getStatistic("mean");
    private static final Statistic MAX_STATISTIC = STATISTIC_FACTORY.getStatistic("max");
    private static final Statistic SUM_STATISTIC = STATISTIC_FACTORY.getStatistic("sum");
    private static final Statistic COUNT_STATISTIC = STATISTIC_FACTORY.getStatistic("count");
    private static final Statistic MEDIAN_STATISTIC = STATISTIC_FACTORY.getStatistic("median");

    private static final class AbsentStatisticCacheLoader extends CacheLoader<String, Optional<ImmutableSet<Statistic>>> {
        @Override
        public Optional<ImmutableSet<Statistic>> load(final String key) throws Exception {
            return Optional.absent();
        }
    }
}
