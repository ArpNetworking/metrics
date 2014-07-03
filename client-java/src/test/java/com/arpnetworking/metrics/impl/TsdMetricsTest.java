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
package com.arpnetworking.metrics.impl;

import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.test.MetricMatcher;
import com.arpnetworking.metrics.test.MockitoHelper;
import com.arpnetworking.metrics.test.QuantityMatcher;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.collection.IsMapWithSize;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests for <code>TsdMetrics</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdMetricsTest {

    @Test
    public void testEmptySingleSink() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testEmptyMultipleSinks() {
        final Sink sink1 = Mockito.mock(Sink.class, "TsdMetricsTest.testEmptyMultipleSinks.sink1");
        final Sink sink2 = Mockito.mock(Sink.class, "TsdMetricsTest.testEmptyMultipleSinks.sink2");
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink1, sink2);
        metrics.close();

        Mockito.verify(sink1).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));

        Mockito.verify(sink2).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testCounterOnly() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.incrementCounter("counter");
        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "counter",
                                QuantityMatcher.match(1))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testTimerOnly() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.setTimer("timer", 1L, TimeUnit.MILLISECONDS);
        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timer",
                                QuantityMatcher.match(1))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testGaugeOnly() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.setGauge("gauge", 3.14);
        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "gauge",
                                QuantityMatcher.match(3.14, null))));
    }

    @Test
    public void testTimerCounterGauge() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.incrementCounter("counter");
        metrics.setTimer("timer", 1L, TimeUnit.MILLISECONDS);
        metrics.setGauge("gauge", 3.14);
        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timer",
                                QuantityMatcher.match(1))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "counter",
                                QuantityMatcher.match(1))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "gauge",
                                QuantityMatcher.match(3.14, null))));
    }

    @Test
    public void testIsOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        Assert.assertTrue(metrics.isOpen());
        metrics.close();
        Assert.assertFalse(metrics.isOpen());
    }

    @Test
    public void testCreateCounterNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        final Counter counter = metrics.createCounter("counter-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
        Assert.assertNotNull(counter);
    }

    @Test
    public void testIncrementCounterNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.incrementCounter("counter-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testResetCounterNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.resetCounter("counter-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testSetGaugeDoubleNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.setGauge("gauge-closed", 3.14);
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testSetGaugeLongNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.setGauge("gauge-closed", 10L);
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testCreateTimerNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        final Timer timer = metrics.createTimer("timer-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
        Assert.assertNotNull(timer);
    }

    @Test
    public void testSetTimerNotOpenTimeUnit() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.setTimer("timer-closed", 1L, TimeUnit.MILLISECONDS);
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStartTimerNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.startTimer("timer-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStopTimerNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.stopTimer("timer-closed");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testAnnotateNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        metrics.annotate("key", "value");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testCloseNotOpen() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.close();
        Mockito.verifyZeroInteractions(logger);
        metrics.close();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStartTimerAlreadyStarted() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.startTimer("timer-already-started");
        metrics.startTimer("timer-already-started");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStopTimerNotStarted() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.stopTimer("timer-not-started");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStopTimerAlreadyStopped() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final Sink sink = Mockito.mock(Sink.class);
        final TsdMetrics metrics = createTsdMetrics(logger, sink);
        metrics.startTimer("timer-already-stopped");
        metrics.stopTimer("timer-already-stopped");
        Mockito.verifyZeroInteractions(logger);
        metrics.stopTimer("timer-already-stopped");
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testCloseTryWithResource() {
        final Sink sink = Mockito.mock(Sink.class);
        try (final TsdMetrics metrics = createTsdMetrics(sink)) {
            metrics.incrementCounter("testCloseTryWithResource");
        }

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "testCloseTryWithResource",
                                QuantityMatcher.match(1))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testTimerMetrics() throws ParseException, InterruptedException {
        final Sink sink = Mockito.mock(Sink.class);
        final Date earliestStartDate = new Date();
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);

        metrics.setTimer("timerA", 100L, TimeUnit.MILLISECONDS);
        metrics.startTimer("timerB");
        metrics.stopTimer("timerB");
        metrics.startTimer("timerC");
        metrics.stopTimer("timerC");
        metrics.startTimer("timerC");
        metrics.stopTimer("timerC");
        metrics.startTimer("timerD");
        metrics.stopTimer("timerD");
        metrics.setTimer("timerD", 1L, TimeUnit.MILLISECONDS);

        Thread.sleep(10);
        metrics.close();
        final Date latestEndDate = new Date();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArgumentCaptor<Map<String, String>> captureAnnotations =
                ArgumentCaptor.forClass((Class) Map.class);

        Mockito.verify(sink).record(
                captureAnnotations.capture(),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timerA",
                                QuantityMatcher.match(100, Unit.MILLISECOND),
                                "timerB",
                                QuantityMatcher.match(Matchers.any(Number.class), Unit.NANOSECOND),
                                "timerC",
                                QuantityMatcher.match(Matchers.any(Number.class), Unit.NANOSECOND),
                                QuantityMatcher.match(Matchers.any(Number.class), Unit.NANOSECOND),
                                "timerD",
                                QuantityMatcher.match(Matchers.any(Number.class), Unit.NANOSECOND),
                                QuantityMatcher.match(1, Unit.MILLISECOND))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));

        final Map<String, String> annotations = captureAnnotations.getValue();
        Assert.assertThat(annotations, IsMapWithSize.aMapWithSize(2));
        assertTimestamps(earliestStartDate, latestEndDate, annotations);
    }

    @Test
    public void testCounterMetrics() throws ParseException, InterruptedException {
        final Sink sink = Mockito.mock(Sink.class);
        final Date earliestStartDate = new Date();
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);

        metrics.incrementCounter("counterA");
        metrics.incrementCounter("counterB", 2L);
        metrics.decrementCounter("counterC");
        metrics.decrementCounter("counterD", 2L);
        metrics.resetCounter("counterE");
        metrics.resetCounter("counterF");
        metrics.resetCounter("counterF");
        metrics.incrementCounter("counterF");
        metrics.resetCounter("counterF");
        metrics.incrementCounter("counterF");
        metrics.incrementCounter("counterF");

        Thread.sleep(10);
        metrics.close();
        final Date latestEndDate = new Date();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArgumentCaptor<Map<String, String>> captureAnnotations =
                ArgumentCaptor.forClass((Class) Map.class);

        Mockito.verify(sink).record(
                captureAnnotations.capture(),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "counterA",
                                QuantityMatcher.match(1),
                                "counterB",
                                QuantityMatcher.match(2),
                                "counterC",
                                QuantityMatcher.match(-1),
                                "counterD",
                                QuantityMatcher.match(-2),
                                "counterE",
                                QuantityMatcher.match(0),
                                "counterF",
                                QuantityMatcher.match(0),
                                QuantityMatcher.match(1),
                                QuantityMatcher.match(2))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));

        final Map<String, String> annotations = captureAnnotations.getValue();
        Assert.assertThat(annotations, IsMapWithSize.aMapWithSize(2));
        assertTimestamps(earliestStartDate, latestEndDate, annotations);
    }

    @Test
    public void testGaugeMetrics() throws ParseException, InterruptedException {
        final Sink sink = Mockito.mock(Sink.class);
        final Date earliestStartDate = new Date();
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);

        metrics.setGauge("gaugeA", 10L);
        metrics.setGauge("gaugeB", 3.14);
        metrics.setGauge("gaugeC", 10L);
        metrics.setGauge("gaugeC", 20L);
        metrics.setGauge("gaugeD", 2.07);
        metrics.setGauge("gaugeD", 3.14);

        Thread.sleep(10);
        metrics.close();
        final Date latestEndDate = new Date();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArgumentCaptor<Map<String, String>> captureAnnotations =
                ArgumentCaptor.forClass((Class) Map.class);

        Mockito.verify(sink).record(
                captureAnnotations.capture(),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "gaugeA",
                                QuantityMatcher.match(10),
                                "gaugeB",
                                QuantityMatcher.match(3.14),
                                "gaugeC",
                                QuantityMatcher.match(10),
                                QuantityMatcher.match(20),
                                "gaugeD",
                                QuantityMatcher.match(2.07),
                                QuantityMatcher.match(3.14))));

        final Map<String, String> annotations = captureAnnotations.getValue();
        Assert.assertThat(annotations, IsMapWithSize.aMapWithSize(2));
        assertTimestamps(earliestStartDate, latestEndDate, annotations);
    }

    @Test
    public void testAnnotationMetrics() throws ParseException, InterruptedException {
        final Sink sink = Mockito.mock(Sink.class);
        final Date earliestStartDate = new Date();
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);

        metrics.annotate("foo", "bar");
        metrics.annotate("dup", "cat");
        metrics.annotate("dup", "dog");

        Thread.sleep(10);
        metrics.close();
        final Date latestEndDate = new Date();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArgumentCaptor<Map<String, String>> captureAnnotations =
                ArgumentCaptor.forClass((Class) Map.class);

        Mockito.verify(sink).record(
                captureAnnotations.capture(),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));

        final Map<String, String> annotations = captureAnnotations.getValue();
        Assert.assertThat(annotations, IsMapWithSize.aMapWithSize(4));
        Assert.assertThat(annotations, IsMapContaining.hasEntry("foo", "bar"));
        Assert.assertThat(annotations, IsMapContaining.hasEntry("dup", "dog"));
        assertTimestamps(earliestStartDate, latestEndDate, annotations);
    }

    public void testUnits() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);

        metrics.setGauge("bySize", 21L, Unit.BYTE);
        metrics.setGauge("bySize", 22L, Unit.KILOBYTE);
        metrics.setGauge("bySize", 23L, Unit.MEGABYTE);
        metrics.setGauge("bySize", 24L, Unit.GIGABYTE);

        // You should never do this but the library cannot prevent it because
        // values are combined across instances, processes and hosts:
        metrics.setGauge("mixedUnit", 3.14, Unit.BYTE);
        metrics.setGauge("mixedUnit", 2.07, Unit.SECOND);

        metrics.setTimer("withTimeUnit", 11L, TimeUnit.NANOSECONDS);
        metrics.setTimer("withTimeUnit", 12L, TimeUnit.MICROSECONDS);
        metrics.setTimer("withTimeUnit", 13L, TimeUnit.MILLISECONDS);
        metrics.setTimer("withTimeUnit", 14L, TimeUnit.SECONDS);
        metrics.setTimer("withTimeUnit", 15L, TimeUnit.MINUTES);
        metrics.setTimer("withTimeUnit", 16L, TimeUnit.HOURS);
        metrics.setTimer("withTimeUnit", 17L, TimeUnit.DAYS);

        metrics.setTimer("withTsdUnit", 1L, Unit.NANOSECOND);
        metrics.setTimer("withTsdUnit", 2L, Unit.MICROSECOND);
        metrics.setTimer("withTsdUnit", 3L, Unit.MILLISECOND);
        metrics.setTimer("withTsdUnit", 4L, Unit.SECOND);
        metrics.setTimer("withTsdUnit", 5L, Unit.MINUTE);
        metrics.setTimer("withTsdUnit", 6L, Unit.HOUR);
        metrics.setTimer("withTsdUnit", 7L, Unit.DAY);

        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "withTimeUnit",
                                QuantityMatcher.match(11, Unit.NANOSECOND),
                                QuantityMatcher.match(12, Unit.MICROSECOND),
                                QuantityMatcher.match(13, Unit.MILLISECOND),
                                QuantityMatcher.match(14, Unit.SECOND),
                                QuantityMatcher.match(15, Unit.MINUTE),
                                QuantityMatcher.match(16, Unit.HOUR),
                                QuantityMatcher.match(17, Unit.DAY),
                                "withTsdUnit",
                                QuantityMatcher.match(1, Unit.NANOSECOND),
                                QuantityMatcher.match(2, Unit.MICROSECOND),
                                QuantityMatcher.match(3, Unit.MILLISECOND),
                                QuantityMatcher.match(4, Unit.SECOND),
                                QuantityMatcher.match(5, Unit.MINUTE),
                                QuantityMatcher.match(6, Unit.HOUR),
                                QuantityMatcher.match(7, Unit.DAY))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "bySize",
                                QuantityMatcher.match(21, Unit.BYTE),
                                QuantityMatcher.match(22, Unit.KILOBYTE),
                                QuantityMatcher.match(23, Unit.MEGABYTE),
                                QuantityMatcher.match(24, Unit.GIGABYTE),
                                "mixedUnit",
                                QuantityMatcher.match(3.14, Unit.BYTE),
                                QuantityMatcher.match(2.07, Unit.SECOND))));
    }

    @Test
    public void testTimerObjects() throws InterruptedException {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        @SuppressWarnings("resource")
        final Timer timerObjectA = metrics.createTimer("timerObjectA");
        @SuppressWarnings("resource")
        final Timer timerObjectB1 = metrics.createTimer("timerObjectB");
        @SuppressWarnings("resource")
        final Timer timerObjectB2 = metrics.createTimer("timerObjectB");

        Thread.sleep(1);

        timerObjectA.close();
        timerObjectB2.close();

        Thread.sleep(1);

        timerObjectB1.close();
        metrics.close();

        // Important: The samples for timerObjectB are recorded in the order the
        // two timer objects are instantiated and not the order in which they
        // are stopped/closed.

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timerObjectA",
                                QuantityMatcher.match(Matchers.greaterThanOrEqualTo(Long.valueOf(1)), Unit.NANOSECOND),
                                "timerObjectB",
                                QuantityMatcher.match(Matchers.greaterThanOrEqualTo(Long.valueOf(2)), Unit.NANOSECOND),
                                QuantityMatcher.match(Matchers.greaterThanOrEqualTo(Long.valueOf(1)), Unit.NANOSECOND))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testSkipUnclosedTimerSample() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.createTimer("timerObjectA");
        metrics.setTimer("timerObjectA", 1, TimeUnit.SECONDS);

        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timerObjectA",
                                QuantityMatcher.match(1, Unit.SECOND))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testTimerWithoutClosedSample() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.createTimer("timerObjectB");
        metrics.setTimer("timerObjectA", 1, TimeUnit.SECONDS);

        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timerObjectA",
                                QuantityMatcher.match(1, Unit.SECOND),
                                "timerObjectB")),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    @Test
    public void testOnlyTimerWithClosedSample() {
        final Sink sink = Mockito.mock(Sink.class);
        @SuppressWarnings("resource")
        final TsdMetrics metrics = createTsdMetrics(sink);
        metrics.createTimer("timerObjectB");

        metrics.close();

        Mockito.verify(sink).record(
                MockitoHelper.<Map<String, String>>argThat(
                        Matchers.allOf(
                                Matchers.hasKey("initTimestamp"),
                                Matchers.hasKey("finalTimestamp"))),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(
                        MetricMatcher.match(
                                "timerObjectB")),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(IsMapWithSize.anEmptyMap()));
    }

    private TsdMetrics createTsdMetrics(final Sink... sinks) {
        return createTsdMetrics(createSlf4jLoggerMock(), sinks);
    }

    private TsdMetrics createTsdMetrics(final org.slf4j.Logger logger, final Sink... sinks) {
        return new TsdMetrics(
                Arrays.asList(sinks),
                logger);
    }

    private org.slf4j.Logger createSlf4jLoggerMock() {
        return Mockito.mock(org.slf4j.Logger.class);
    }

    private void assertTimestamps(
            final Date earliestStartDate,
            final Date latestEndDate,
            final Map<String, String> annotations)
            throws ParseException {

        Assert.assertTrue(annotations.containsKey("initTimestamp"));
        final Date actualStart = _iso8601Format.parse(annotations.get("initTimestamp"));
        Assert.assertTrue(earliestStartDate.getTime() <= actualStart.getTime());
        Assert.assertTrue(latestEndDate.getTime() >= actualStart.getTime());

        Assert.assertTrue(annotations.containsKey("finalTimestamp"));
        final Date actualEnd = _iso8601Format.parse(annotations.get("finalTimestamp"));
        Assert.assertTrue(latestEndDate.getTime() >= actualEnd.getTime());
        Assert.assertTrue(earliestStartDate.getTime() <= actualEnd.getTime());
    }

    // NOTE: SimpleDateFormat is not thread safe thus it is non-static
    private final SimpleDateFormat _iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
}
