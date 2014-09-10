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

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.test.MetricMatcher;
import com.arpnetworking.metrics.test.QuantityMatcher;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsMapWithSize;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

/**
 * Tests for concurrent usage of <code>TsdMetrics</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(TsdMetrics.class)
public class TsdMetricsConcurrencyTest {

    @Test
    public void testConcurrentIncrementCounter() {
        runOnce(new TestConcurrentIncrementCounter());
    }

    @Test
    public void testConcurrentResetCounter() {
        runOnce(new TestConcurrentResetCounter());
    }

    @Test
    public void testConcurrentSetTimer() {
        runOnce(new TestConcurrentSetTimer());
    }

    @Test
    public void testConcurrentSetGaugeDouble() {
        runOnce(new TestConcurrentSetGaugeDouble());
    }

    @Test
    public void testConcurrentSetGaugeLong() {
        runOnce(new TestConcurrentSetGaugeLong());
    }

    private static void runOnce(final MultithreadedTestCase test) {
        try {
            TestFramework.runOnce(test);
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (final Throwable e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new RuntimeException(e);
        }
    }

    private static final class TestConcurrentIncrementCounter extends TestSequentialGetOrCreate {

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread1() throws InterruptedException {
            _metricsSpy.incrementCounter("testConcurrentIncrementCounter");
        }

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread2() throws InterruptedException {
            waitForTick(1);
            _metricsSpy.incrementCounter("testConcurrentIncrementCounter");
        }

        public TestConcurrentIncrementCounter() {
            super(
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    EXPECTED,
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap());
        }

        private static final Matcher<Map<? extends String, ? extends List<Quantity>>> EXPECTED = MetricMatcher.match(
                "testConcurrentIncrementCounter",
                QuantityMatcher.match(2));
    }

    private static final class TestConcurrentResetCounter extends TestSequentialGetOrCreate {

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread1() throws InterruptedException {
            _metricsSpy.resetCounter("testConcurrentResetCounter");
        }

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread2() throws InterruptedException {
            waitForTick(1);
            _metrics.resetCounter("testConcurrentResetCounter");
        }

        public TestConcurrentResetCounter() {
            super(
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    EXPECTED,
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap());
        }

        private static final Matcher<Map<? extends String, ? extends List<Quantity>>> EXPECTED = MetricMatcher.match(
                "testConcurrentResetCounter",
                QuantityMatcher.match(0),
                QuantityMatcher.match(0));
    }

    private static final class TestConcurrentSetTimer extends TestSequentialGetOrCreate {

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread1() throws InterruptedException {
            _metricsSpy.setTimer("testConcurrentSetTimer", 600, TimeUnit.MILLISECONDS);
        }

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread2() throws InterruptedException {
            waitForTick(1);
            _metrics.setTimer("testConcurrentSetTimer", 500, TimeUnit.MILLISECONDS);
        }

        public TestConcurrentSetTimer() {
            super(
                    EXPECTED,
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap());
        }

        private static final Matcher<Map<? extends String, ? extends List<Quantity>>> EXPECTED = MetricMatcher.match(
                "testConcurrentSetTimer",
                QuantityMatcher.match(500),
                QuantityMatcher.match(600));
    }

    private static final class TestConcurrentSetGaugeDouble extends TestSequentialGetOrCreate {

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread1() throws InterruptedException {
            _metricsSpy.setGauge("testConcurrentSetGaugeDouble", 2.0);
        }

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread2() throws InterruptedException {
            waitForTick(1);
            _metrics.setGauge("testConcurrentSetGaugeDouble", 1.0);
        }

        public TestConcurrentSetGaugeDouble() {
            super(
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    EXPECTED);
        }

        private static final Matcher<Map<? extends String, ? extends List<Quantity>>> EXPECTED = MetricMatcher.match(
                "testConcurrentSetGaugeDouble",
                QuantityMatcher.match(1.0),
                QuantityMatcher.match(2.0));
    }

    private static final class TestConcurrentSetGaugeLong extends TestSequentialGetOrCreate {

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread1() throws InterruptedException {
            _metricsSpy.setGauge("testConcurrentSetGaugeLong", 4L);
        }

        // NOTE: Invoked reflectively
        @SuppressWarnings("unused")
        public void thread2() throws InterruptedException {
            waitForTick(1);
            _metrics.setGauge("testConcurrentSetGaugeLong", 3L);
        }

        public TestConcurrentSetGaugeLong() {
            super(
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    IsMapWithSize.<String, List<Quantity>>anEmptyMap(),
                    EXPECTED);
        }

        private static final Matcher<Map<? extends String, ? extends List<Quantity>>> EXPECTED = MetricMatcher.match(
                "testConcurrentSetGaugeLong",
                QuantityMatcher.match(3),
                QuantityMatcher.match(4));
    }

    private abstract static class TestSequentialGetOrCreate extends MultithreadedTestCase {

        @Override
        public void initialize() {
            PowerMockito.doAnswer(new Answer<Object>() {

                // CHECKSTYLE.OFF: IllegalThrow
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // CHECKSTYLE.ON: IllegalThrow
                    waitForTick(_tick.getAndIncrement());
                    return invocation.callRealMethod();
                }

            }).when(_metricsSpy).<Object>getOrCreate(
                    Matchers.<ConcurrentMap<String, Object>>any(),
                    Matchers.<String>any(),
                    Matchers.any());
        }

        @Override
        public void finish() {
            _metrics.close();

            @SuppressWarnings({ "unchecked", "rawtypes" })
            final ArgumentCaptor<Map<String, List<Quantity>>> actualTimers = ArgumentCaptor.forClass((Class) Map.class);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final ArgumentCaptor<Map<String, List<Quantity>>> actualCounters = ArgumentCaptor.forClass((Class) Map.class);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final ArgumentCaptor<Map<String, List<Quantity>>> actualGauges = ArgumentCaptor.forClass((Class) Map.class);

            Mockito.verify(_sink).record(
                    Mockito.anyMapOf(String.class, String.class),
                    actualTimers.capture(),
                    actualCounters.capture(),
                    actualGauges.capture());

            Assert.assertThat(actualTimers.getValue(), _expectedTimers);
            Assert.assertThat(actualCounters.getValue(), _expectedCounters);
            Assert.assertThat(actualGauges.getValue(), _expectedGauges);
        }

        protected TestSequentialGetOrCreate(
                final Matcher<Map<? extends String, ? extends List<Quantity>>> expectedTimers,
                final Matcher<Map<? extends String, ? extends List<Quantity>>> expectedCounters,
                final Matcher<Map<? extends String, ? extends List<Quantity>>> expectedGauges) {
            _expectedTimers = expectedTimers;
            _expectedCounters = expectedCounters;
            _expectedGauges = expectedGauges;
        }

        private final AtomicInteger _tick = new AtomicInteger(2);
        private final Sink _sink = Mockito.mock(Sink.class);

        private final Matcher<Map<? extends String, ? extends List<Quantity>>> _expectedTimers;
        private final Matcher<Map<? extends String, ? extends List<Quantity>>> _expectedCounters;
        private final Matcher<Map<? extends String, ? extends List<Quantity>>> _expectedGauges;

        protected final TsdMetrics _metrics = new TsdMetrics(Collections.<Sink>singletonList(_sink));
        protected final TsdMetrics _metricsSpy = PowerMockito.spy(_metrics);
    }
}
