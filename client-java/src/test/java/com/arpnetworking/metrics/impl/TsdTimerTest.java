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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for <code>TsdTimer</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdTimerTest {

    @Test
    public void testClose() throws InterruptedException {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final long minimumTimeInMilliseconds = 100;
        final long startTime = System.nanoTime();
        TsdTimer timer;
        try (final TsdTimer resourceTimer = TsdTimer.newInstance("timerName", isOpen)) {
            // Without the assert compilation will log a warning
            assert resourceTimer != null;
            timer = resourceTimer;
            Thread.sleep(minimumTimeInMilliseconds);
            resourceTimer.stop();
        }
        final long elapsedTimeInNanoseconds = System.nanoTime() - startTime;
        Assert.assertThat(
                (Long) timer.getValue(),
                Matchers.greaterThanOrEqualTo(Long.valueOf(TimeUnit.NANOSECONDS.convert(
                        minimumTimeInMilliseconds,
                        TimeUnit.MILLISECONDS))));
        Assert.assertThat((Long) timer.getValue(), Matchers.lessThanOrEqualTo(Long.valueOf(elapsedTimeInNanoseconds)));
    }

    @Test
    public void testStop() throws InterruptedException {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final long minimumTimeInMilliseconds = 100;
        final long startTime = System.nanoTime();
        try (final TsdTimer timer = TsdTimer.newInstance("timerName", isOpen)) {
            // Without the assert compilation will log a warning
            assert timer != null;
            Thread.sleep(minimumTimeInMilliseconds);
            timer.stop();

            final long elapsedTimeInNanoseconds = System.nanoTime() - startTime;
            Assert.assertThat(
                    (Long) timer.getValue(),
                    Matchers.greaterThanOrEqualTo(Long.valueOf(TimeUnit.NANOSECONDS.convert(
                            minimumTimeInMilliseconds,
                            TimeUnit.MILLISECONDS))));
            Assert.assertThat((Long) timer.getValue(), Matchers.lessThanOrEqualTo(Long.valueOf(elapsedTimeInNanoseconds)));
        }
    }

    @Test
    public void testAlreadyStopped() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final Logger logger = createSlf4jLoggerMock();
        @SuppressWarnings("resource")
        final TsdTimer timer = new TsdTimer("timerName", isOpen, logger);
        timer.close();
        Mockito.verifyZeroInteractions(logger);
        timer.close();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testStopAfterMetricsClosed() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        @SuppressWarnings("resource")
        final TsdTimer timer = new TsdTimer("timerName", isOpen, logger);
        isOpen.set(false);
        Mockito.verify(logger, Mockito.never()).warn(Mockito.argThat(Matchers.any(String.class)));
        timer.close();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testGetElapsedAfterStop() {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        @SuppressWarnings("resource")
        final TsdTimer timer = new TsdTimer("timerName", isOpen, logger);
        Mockito.verify(logger, Mockito.never()).warn(Mockito.argThat(Matchers.any(String.class)));
        timer.getValue();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testIsStopped() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdTimer timer = TsdTimer.newInstance("timerName", isOpen);
        Assert.assertFalse(timer.isStopped());
        timer.stop();
        Assert.assertTrue(timer.isStopped());
    }

    @Test
    public void testToString() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final String asString = TsdTimer.newInstance("timerName", isOpen).toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
        Assert.assertThat(asString, Matchers.containsString("timerName"));
    }

    @Test
    public void testConstructor() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdTimer timer = TsdTimer.newInstance("timerName", isOpen);
        Assert.assertFalse(timer.isStopped());
    }

    private Logger createSlf4jLoggerMock() {
        return Mockito.mock(org.slf4j.Logger.class);
    }
}
