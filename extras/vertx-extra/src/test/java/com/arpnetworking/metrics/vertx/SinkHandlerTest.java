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
import com.arpnetworking.metrics.vertx.test.TestQuantityImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.vertx.java.core.eventbus.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests the inner handler class <code>SinkHandler</code> for <code>SinkVerticle</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class SinkHandlerTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(SINK_ADDRESS).when(_message).address();
        final List<Sink> sinks = ImmutableList.of(_mockSink);
        _handler = new SinkVerticle.SinkHandler(sinks);
    }

    @After
    public void teardown() {
        _mockSink = null;
        _message = null;
    }

    @Test
    public void testHandleWithNullMessage() {
        _handler.handle(null);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithMessageWithNullBody() {
        Mockito.doReturn(null).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithMessageWithoutAnnotations() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                TIMER_SAMPLES_KEY,
                Collections.emptyMap(),
                COUNTER_SAMPLES_KEY,
                Collections.emptyMap(),
                GAUGE_SAMPLES_KEY,
                Collections.emptyMap()));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithMessageWithoutTimerSamples() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                ANNOTATIONS_KEY,
                Collections.emptyMap(),
                COUNTER_SAMPLES_KEY,
                Collections.emptyMap(),
                GAUGE_SAMPLES_KEY,
                Collections.emptyMap()));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithMessageWithoutCounterSamples() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                TIMER_SAMPLES_KEY,
                Collections.emptyMap(),
                ANNOTATIONS_KEY,
                Collections.emptyMap(),
                GAUGE_SAMPLES_KEY,
                Collections.emptyMap()));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithMessageWithoutGaugeSamples() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                TIMER_SAMPLES_KEY,
                Collections.emptyMap(),
                COUNTER_SAMPLES_KEY,
                Collections.emptyMap(),
                ANNOTATIONS_KEY,
                Collections.emptyMap()));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }

    @Test
    public void testHandleWithEmptyMaps() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                ANNOTATIONS_KEY,
                Collections.emptyMap(),
                TIMER_SAMPLES_KEY,
                Collections.emptyMap(),
                COUNTER_SAMPLES_KEY,
                Collections.emptyMap(),
                GAUGE_SAMPLES_KEY,
                Collections.emptyMap()));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verify(_mockSink)
                .record(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test
    public void testHandleWithNonEmptyValidMaps() throws JsonProcessingException {
        final Map<String, String> annotationMap = ImmutableMap.of("someAnnotationKey", "someAnnotationValue");
        final Map<String, List<Quantity>> timerSampleMap = ImmutableMap.of(
                "timerSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(100, Unit.MEGABYTE),
                        SinkVerticle.DefaultQuantity.newInstance(40, Unit.GIGABYTE)));
        final Map<String, List<Quantity>> counterSampleMap = ImmutableMap.of(
                "counterSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(400, Unit.MILLISECOND)));
        final Map<String, List<Quantity>> gaugeSampleMap = ImmutableMap.of(
                "gaugeSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(1000, Unit.MILLISECOND),
                        SinkVerticle.DefaultQuantity.newInstance(5, Unit.MINUTE)));
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                ANNOTATIONS_KEY,
                annotationMap,
                TIMER_SAMPLES_KEY,
                timerSampleMap,
                COUNTER_SAMPLES_KEY,
                counterSampleMap,
                GAUGE_SAMPLES_KEY,
                gaugeSampleMap));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verify(_mockSink)
                .record(annotationMap, timerSampleMap, counterSampleMap, gaugeSampleMap);
    }

    @Test
    public void testHandleWithNonEmptyInvalidData() throws JsonProcessingException {
        final String messageBody = OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(
                ANNOTATIONS_KEY,
                ImmutableMap.of("someAnnotationKey", "someAnnotationValue"),
                TIMER_SAMPLES_KEY,
                ImmutableMap.of("someKey", "invalid value"),
                COUNTER_SAMPLES_KEY,
                ImmutableMap.of("another", "another invalid value"),
                GAUGE_SAMPLES_KEY,
                ImmutableMap.of("validKey", Arrays.asList(SinkVerticle.DefaultQuantity.newInstance(10, Unit.MEGABYTE)))));
        Mockito.doReturn(messageBody).when(_message).body();
        _handler.handle(_message);
        Mockito.verifyZeroInteractions(_mockSink);
    }


    private SinkVerticle.SinkHandler _handler;
    @Mock
    private Sink _mockSink;
    @Mock
    private Message<String> _message;

    private static final String SINK_ADDRESS = "sink.address";
    private static final String ANNOTATIONS_KEY = "annotations";
    private static final String TIMER_SAMPLES_KEY = "timerSamples";
    private static final String COUNTER_SAMPLES_KEY  = "counterSamples";
    private static final String GAUGE_SAMPLES_KEY = "gaugeSamples";
    private static final Map<String, String> ANNOTATION_MAP = Collections.singletonMap("someAnnotationKey", "someAnnotationValue");
    private static final Quantity QUANTITY = new TestQuantityImpl.Builder().setUnit(Unit.MEGABYTE).setValue(12).build();
    private static final Map<String, List<Quantity>> SAMPLE_MAP = Collections.singletonMap(
                    "sampleKey",
                    Collections.singletonList(SinkVerticle.DefaultQuantity.newInstance(12, Unit.MEGABYTE)));
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
