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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for <code>TsdCounter</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdCounterTest {

    @Test
    public void testIncrement() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdCounter counter = TsdCounter.newInstance("counterName", isOpen);
        Assert.assertEquals(0L, counter.getValue().longValue());
        counter.increment();
        Assert.assertEquals(1L, counter.getValue().longValue());
        counter.increment();
        Assert.assertEquals(2L, counter.getValue().longValue());
    }

    @Test
    public void testDecrement() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdCounter counter = TsdCounter.newInstance("counterName", isOpen);
        Assert.assertEquals(0L, counter.getValue().longValue());
        counter.decrement();
        Assert.assertEquals(-1L, counter.getValue().longValue());
        counter.decrement();
        Assert.assertEquals(-2L, counter.getValue().longValue());
    }

    @Test
    public void testIncrementByValue() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdCounter counter = TsdCounter.newInstance("counterName", isOpen);
        Assert.assertEquals(0L, counter.getValue().longValue());
        counter.increment(2);
        Assert.assertEquals(2L, counter.getValue().longValue());
        counter.increment(3);
        Assert.assertEquals(5L, counter.getValue().longValue());
    }

    @Test
    public void testDecrementByValue() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdCounter counter = TsdCounter.newInstance("counterName", isOpen);
        Assert.assertEquals(0L, counter.getValue().longValue());
        counter.decrement(2);
        Assert.assertEquals(-2L, counter.getValue().longValue());
        counter.decrement(3);
        Assert.assertEquals(-5L, counter.getValue().longValue());
    }

    @Test
    public void testCombination() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final TsdCounter counter = TsdCounter.newInstance("counterName", isOpen);
        Assert.assertEquals(0L, counter.getValue().longValue());
        counter.increment();
        Assert.assertEquals(1L, counter.getValue().longValue());
        counter.decrement(3L);
        Assert.assertEquals(-2L, counter.getValue().longValue());
        counter.increment(4L);
        Assert.assertEquals(2L, counter.getValue().longValue());
        counter.decrement();
        Assert.assertEquals(1L, counter.getValue().longValue());
    }

    @Test
    public void testIncrementAfterClose() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final Logger logger = Mockito.mock(Logger.class);
        final TsdCounter counter = new TsdCounter("counterName", isOpen, logger);
        counter.increment();
        isOpen.set(false);
        Mockito.verifyZeroInteractions(logger);
        counter.increment();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testIncrementByValueAfterClose() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final Logger logger = Mockito.mock(Logger.class);
        final TsdCounter counter = new TsdCounter("counterName", isOpen, logger);
        counter.increment(2L);
        isOpen.set(false);
        Mockito.verifyZeroInteractions(logger);
        counter.increment(2L);
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testDecrementAfterClose() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final Logger logger = Mockito.mock(Logger.class);
        final TsdCounter counter = new TsdCounter("counterName", isOpen, logger);
        counter.decrement();
        isOpen.set(false);
        Mockito.verifyZeroInteractions(logger);
        counter.decrement();
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testDecrementByValueAfterClose() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final Logger logger = Mockito.mock(Logger.class);
        final TsdCounter counter = new TsdCounter("counterName", isOpen, logger);
        counter.decrement(2L);
        isOpen.set(false);
        Mockito.verifyZeroInteractions(logger);
        counter.decrement(2L);
        Mockito.verify(logger).warn(Mockito.argThat(Matchers.any(String.class)));
    }

    @Test
    public void testToString() {
        final AtomicBoolean isOpen = new AtomicBoolean(true);
        final String asString = TsdCounter.newInstance("counterName", isOpen).toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
        Assert.assertThat(asString, Matchers.containsString("counterName"));
    }
}
