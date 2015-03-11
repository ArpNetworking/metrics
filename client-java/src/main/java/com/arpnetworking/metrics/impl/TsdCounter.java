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
import com.arpnetworking.metrics.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of <code>Counter</code>.  This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
final class TsdCounter implements Counter, Quantity {
    /**
     * Package private constructor. All <code>TsdCounter</code> instances should
     * be created through the <code>TsdMetrics</code> instance.
     * 
     * @param name The name of the counter.
     * @param isOpen Reference to state of containing <code>TsdMetrics</code>
     * instance. This is provided as a separate parameter to avoid creating a
     * cyclical dependency between <code>TsdMetrics</code> and 
     * <code>TsdCounter</code> which could cause garbage collection delays.
     */
    /* package private */static TsdCounter newInstance(final String name, final AtomicBoolean isOpen) {
        return new TsdCounter(name, isOpen, DEFAULT_LOGGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        increment(1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrement() {
        increment(-1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(final long value) {
        assertIsOpen();
        _value.addAndGet(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrement(final long value) {
        increment(-1L * value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format(
                "TsdCounter{id=%s, Name=%s, Value=%s, IsOpen=%s}",
                Integer.toHexString(System.identityHashCode(this)),
                _name,
                _value,
                _isOpen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getValue() {
        return Long.valueOf(_value.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getUnit() {
        return null;
    }

    private boolean assertIsOpen() {
        final boolean isOpen = _isOpen.get();
        if (!isOpen) {
            // This is in place of an exception
            _logger.warn(String.format("Counter manipulated after metrics instance closed; counter=%s", this));
        }
        return isOpen;
    }

    // NOTE: Package private for testing.
    /* package private */TsdCounter(final String name, final AtomicBoolean isOpen, final Logger logger) {
        _name = name;
        _isOpen = isOpen;
        _logger = logger;
        _value = new AtomicLong(0L);
    }

    private final String _name;
    private final AtomicBoolean _isOpen;
    private final Logger _logger;
    private final AtomicLong _value;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TsdCounter.class);
}
