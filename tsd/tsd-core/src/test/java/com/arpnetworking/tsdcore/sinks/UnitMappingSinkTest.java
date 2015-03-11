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
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>UnitMappingSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class UnitMappingSinkTest {

    @Test
    public void testMappingNanosecondToSecond() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.NANOSECOND).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                                org.hamcrest.Matchers.<AggregatedData>contains(dataBuilder
                                        .setValue(new Quantity.Builder().setValue(0.000000001).setUnit(Unit.SECOND).build())
                                        .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testMappingMicrosecondToSecond() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(2d).setUnit(Unit.MICROSECOND).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                                org.hamcrest.Matchers.contains(dataBuilder
                                        .setValue(new Quantity.Builder().setValue(0.000002).setUnit(Unit.SECOND).build())
                                        .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testMappingMillisecondToSecond() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(3d).setUnit(Unit.MILLISECOND).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                                org.hamcrest.Matchers.contains(dataBuilder
                                        .setValue(new Quantity.Builder().setValue(0.003).setUnit(Unit.SECOND).build())
                                        .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testMappingIdentity() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(4d).setUnit(Unit.SECOND).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                                org.hamcrest.Matchers.contains(dataBuilder
                                        .setValue(new Quantity.Builder().setValue(4d).setUnit(Unit.SECOND).build())
                                        .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));

    }

    @Test
    public void testMappingUnchanged() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(5d).setUnit(Unit.MINUTE).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                                org.hamcrest.Matchers.contains(dataBuilder
                                        .setValue(new Quantity.Builder().setValue(5d).setUnit(Unit.MINUTE).build())
                                        .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testMappingBitsToBytes() {
        final Sink sink = _sinkBuilder.build();

        final AggregatedData.Builder dataBuilder = TestBeanFactory.createAggregatedDataBuilder();
        sink.recordAggregateData(Collections.singletonList(dataBuilder
                .setValue(new Quantity.Builder().setValue(8d).setUnit(Unit.BIT).build())
                .build()));
        Mockito.verify(_target).recordAggregateData((List<AggregatedData>) Matchers.argThat(
                org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.<AggregatedData>iterableWithSize(1),
                        org.hamcrest.Matchers.contains(dataBuilder
                                .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.BYTE).build())
                                .build()))),
                Matchers.eq(Collections.<Condition>emptyList()));
    }

    @Test
    public void testClose() {
        final Sink sink = _sinkBuilder.build();
        sink.close();
        Mockito.verify(_target).close();
    }

    @Test
    public void testToString() {
        final String asString = _sinkBuilder.build().toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }

    private final Sink _target = Mockito.mock(Sink.class);
    private final UnitMappingSink.Builder _sinkBuilder = new UnitMappingSink.Builder()
            .setName("UnitMappingSinkTest")
            .setMap(ImmutableMap.of(
                    Unit.NANOSECOND,
                    Unit.SECOND,
                    Unit.MICROSECOND,
                    Unit.SECOND,
                    Unit.MILLISECOND,
                    Unit.SECOND,
                    Unit.BIT,
                    Unit.BYTE))
            .setSink(_target);
}
