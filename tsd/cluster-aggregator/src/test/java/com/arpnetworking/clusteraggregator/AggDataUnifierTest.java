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

package com.arpnetworking.clusteraggregator;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the {@link AggDataUnifier}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggDataUnifierTest {
    @Test
    public void testPrivateConstructor() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor<AggDataUnifier> constructor = AggDataUnifier.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testEmptyList() {
        AggDataUnifier.unify(Collections.<AggregatedData>emptyList());
    }

    @Test
    public void testNoValueUnit() {
        final List<AggregatedData> data = Lists.newArrayList();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder().setValue(10d).build());
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(false, unifiedData.getValue().getUnit().isPresent());
    }

    @Test
    public void testNoUnits() {
        final List<AggregatedData> data = Lists.newArrayList();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder().setValue(10d).build());
        builder.setSamples(Collections.singletonList(new Quantity.Builder().setValue(10d).build()));
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(false, unifiedData.getValue().getUnit().isPresent());
    }

    @Test
    public void testOnlyValueUnit() {
        final List<AggregatedData> data = Lists.newArrayList();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build());
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
    }

    /**
     * Tests that a single AggregatedData with an already-unified value and sample does not
     * get converted.
     */
    @Test
    public void testSingleSampleUnified() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(1000d)
                .setUnit(Unit.MILLISECOND)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build());
        builder.setSamples(Collections.singletonList(sample));
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
        Assert.assertEquals(1, unifiedData.getSamples().size());
        final Quantity transformedSample = unifiedData.getSamples().get(0);
        Assert.assertEquals(1000.0d, transformedSample.getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), transformedSample.getUnit());
    }

    /**
     * Tests that a single AggregatedData with conflicting unit domains throws an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSingleSampleMismatch() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(1000d)
                .setUnit(Unit.MILLISECOND)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.BYTE)
                .build());
        builder.setSamples(Collections.singletonList(sample));
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
    }

    /**
     * Tests that a single AggregatedData with conflicting unit domains throws an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSampleMissingUnit() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(1000d)
                .setUnit(Unit.MILLISECOND)
                .build();
        final Quantity sample2 = new Quantity.Builder()
                .setValue(1200d)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build());
        builder.setSamples(Lists.newArrayList(sample, sample2));
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
    }

    /**
     * Tests that a single AggregatedData with a value with one unit and a sample
     * with another unit will be converted.  In this case, it is the value that will be converted.
     */
    @Test
    public void testSingleSampleValueDifferenceValueConvert() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(1d)
                .setUnit(Unit.SECOND)
                .build());
        builder.setSamples(Collections.singletonList(sample));
        final AggregatedData aggregatedData = builder.build();
        final List<Quantity> samples = aggregatedData.getSamples();
        data.add(aggregatedData);
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(1000d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
        Assert.assertEquals(1, unifiedData.getSamples().size());
        final Quantity transformedSample = unifiedData.getSamples().get(0);
        Assert.assertEquals(10d, transformedSample.getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), transformedSample.getUnit());

        // Also make sure that the samples we get back is the same instance since no conversion was needed
        Assert.assertSame(samples, unifiedData.getSamples());
    }

    /**
     * Tests that a single AggregatedData with a value with one unit and a sample
     * with another unit will be converted.  In this case, it is the sample that will be converted.
     */
    @Test
    public void testSingleSampleValueDifferenceSampleConvert() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(1d)
                .setUnit(Unit.SECOND)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build());
        builder.setSamples(Collections.singletonList(sample));
        data.add(builder.build());
        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(1, unified.size());
        final AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
        Assert.assertEquals(1, unifiedData.getSamples().size());
        final Quantity transformedSample = unifiedData.getSamples().get(0);
        Assert.assertEquals(1000.0d, transformedSample.getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), transformedSample.getUnit());
    }

    /**
     * Tests that a list with multiple agg data units will convert the samples properly.
     */
    @Test
    public void testCrossAggDataDifferenceSampleConvert() {
        final List<AggregatedData> data = Lists.newArrayList();
        //This should get converted to 1k milliseconds
        final Quantity sample = new Quantity.Builder()
                .setValue(1d)
                .setUnit(Unit.MILLISECOND)
                .build();
        final AggregatedData.Builder builder = getDataBuilder();
        builder.setValue(new Quantity.Builder()
                .setValue(10d)
                .setUnit(Unit.MILLISECOND)
                .build());
        builder.setSamples(Collections.singletonList(sample));
        data.add(builder.build());

        // The first sample is consistent, the second sample will have the value
        // in milliseconds but a sample in seconds.  We expect that the sample
        // will be converted to milliseconds

        builder.setSamples(Collections.singletonList(new Quantity.Builder()
                .setValue(1d)
                .setUnit(Unit.SECOND)
                .build()));
        data.add(builder.build());

        final List<AggregatedData> unified = AggDataUnifier.unify(data);
        Assert.assertEquals(2, unified.size());
        AggregatedData unifiedData = unified.get(0);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
        Assert.assertEquals(1, unifiedData.getSamples().size());
        Quantity transformedSample = unifiedData.getSamples().get(0);
        Assert.assertEquals(1d, transformedSample.getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), transformedSample.getUnit());

        unifiedData = unified.get(1);
        Assert.assertEquals(10d, unifiedData.getValue().getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), unifiedData.getValue().getUnit());
        Assert.assertEquals(1, unifiedData.getSamples().size());
        transformedSample = unifiedData.getSamples().get(0);
        Assert.assertEquals(1000d, transformedSample.getValue(), 0.001);
        Assert.assertEquals(Optional.of(Unit.MILLISECOND), transformedSample.getUnit());

    }

    private AggregatedData.Builder getDataBuilder() {
        return new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("testcluster")
                        .setService("testservice")
                        .setMetric("someSumStat")
                        .setStatistic(SUM_STATISTIC)
                        .build())
                .setPeriod(Period.minutes(1))
                .setStart(DateTime.now().hourOfDay().roundFloorCopy())
                .setHost("testhost")
                .setIsSpecified(true)
                .setPopulationSize(0L)
                .setSamples(Collections.<Quantity>emptyList())
                .setValue(new Quantity.Builder()
                        .setValue(0d)
                        .build());
    }

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic SUM_STATISTIC = STATISTIC_FACTORY.getStatistic("sum");
}
