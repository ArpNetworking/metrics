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
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.metrics.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of <code>Timer</code>. This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
final class TsdTimer implements Timer, Quantity {
    /**
     * Package private constructor. All <code>TsdTimer</code> instances should
     * be created through the <code>TsdMetrics</code> instance.
     * 
     * @param name The name of the timer.
     * @param isOpen Reference to state of containing <code>TsdMetrics</code>
     * instance. This is provided as a separate parameter to avoid creating a
     * cyclical dependency between <code>TsdMetrics</code> and 
     * <code>TsdTimer</code> which could cause garbage collection delays.
     */
    /* package private */static TsdTimer newInstance(final String name, final AtomicBoolean isOpen) {
        return new TsdTimer(name, isOpen, DEFAULT_LOGGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (_isStopped.getAndSet(true)) {
            _logger.warn(String.format("Timer closed/stopped multiple times; timer=%s", this));
        } else if (!_isOpen.get()) {
            _logger.warn(String.format("Timer manipulated after metrics instance closed; timer=%s", this));
        } else {
            _elapsedTime = System.nanoTime() - _startTime;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getValue() {
        if (!_isStopped.get()) {
            _logger.warn(String.format("Timer access before it is closed/stopped; timer=%s", this));
        }
        return Long.valueOf(_elapsedTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getUnit() {
        return Unit.NANOSECOND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format(
                "TsdTimer{id=%s, Name=%s, StartTime=%s, ElapsedTime=%s, IsStopped=%s, IsOpen=%s}",
                Integer.toHexString(System.identityHashCode(this)),
                _name,
                _startTime,
                _elapsedTime,
                _isStopped,
                _isOpen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStopped() {
        return _isStopped.get();
    }

    // NOTE: Package private for testing
    TsdTimer(final String name, final AtomicBoolean isOpen, final Logger logger) {
        _name = name;
        _isOpen = isOpen;
        _logger = logger;
        _startTime = System.nanoTime();
        _isStopped = new AtomicBoolean(false);
    }

    private final String _name;
    private final AtomicBoolean _isOpen;
    private final Logger _logger;
    private final long _startTime;
    private long _elapsedTime = 0;
    private final AtomicBoolean _isStopped;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TsdTimer.class);
}
