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
package com.arpnetworking.metrics.vertx;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.impl.TsdEvent;
import com.arpnetworking.metrics.vertx.test.TestQuantityImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertx.java.core.eventbus.EventBus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests the <code>EventBusSink</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class EventBusSinkTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullEventBus() {
        new EventBusSink.Builder()
            .setEventBus(null)
            .setSinkAddress("sinkAddress")
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullSinkAddress() {
        new EventBusSink.Builder()
            .setEventBus(Mockito.mock(EventBus.class))
            .setSinkAddress(null)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptySinkAddress() {
        new EventBusSink.Builder()
            .setEventBus(Mockito.mock(EventBus.class))
            .setSinkAddress("")
            .build();
    }

    @Test
    public void testRecordWithEmptyMaps() throws JsonProcessingException {
        final EventBus eventBus = Mockito.mock(EventBus.class);
        final Sink sink = new EventBusSink.Builder().setEventBus(eventBus).setSinkAddress("sinkAddress").build();
        sink.record(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        final String data = OBJECT_MAPPER.writeValueAsString(
                new TsdEvent.Builder()
                        .setAnnotations(Collections.emptyMap())
                        .setTimerSamples(Collections.emptyMap())
                        .setCounterSamples(Collections.emptyMap())
                        .setGaugeSamples(Collections.emptyMap())
                        .build());
        Mockito.verify(eventBus).publish("sinkAddress", data);
    }

    @Test
    public void testRecordWithNonEmptyMaps() throws JsonProcessingException {
        final EventBus eventBus = Mockito.mock(EventBus.class);
        final Sink sink = new EventBusSink.Builder().setEventBus(eventBus).setSinkAddress("sinkAddress").build();
        final Map<String, String> annotations = ImmutableMap.of(
                "someAnnotationKey1",
                "someValue1",
                "someAnnotationKey2",
                "someValue2");
        final Map<String, List<Quantity>> timerMap = ImmutableMap.of(
                "someTimerKey1",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.MILLISECOND).setValue(12).build()),
                "someTimerKey2",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.MILLISECOND).setValue(14).build()));
        final Map<String, List<Quantity>> counterMap = ImmutableMap.of(
                "someCounterKey1",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.MEGABYTE).setValue(7).build()),
                "someCounterKey2",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.KILOBYTE).setValue(15).build()));
        final Map<String, List<Quantity>> gaugeMap = ImmutableMap.of(
                "someGaugeKey1",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.MEGABYTE).setValue(1).build()),
                "someGaugeKey2",
                Collections.singletonList(
                        new TestQuantityImpl.Builder().setUnit(Unit.KILOBYTE).setValue(150).build()));
        sink.record(annotations, timerMap, counterMap, gaugeMap);
        final String data = OBJECT_MAPPER.writeValueAsString(
                new TsdEvent.Builder()
                        .setAnnotations(annotations)
                        .setTimerSamples(timerMap)
                        .setCounterSamples(counterMap)
                        .setGaugeSamples(gaugeMap)
                        .build());
        Mockito.verify(eventBus).publish("sinkAddress", data);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
