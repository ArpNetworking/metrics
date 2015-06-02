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
package com.arpnetworking.metrics.vertx;

import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.metrics.Unit;

import org.vertx.java.core.shareddata.Shareable;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Metrics object that extends Vertx's <code>SharedData</code> object which allows use in a shared data map.
 *
 * @author Gil Markham (gil at groupon dot com)
 * @since 0.2.1
 */
public class SharedMetrics implements Metrics, Shareable {
    /**
     *  Constructs a new SharedMetrics object that can be added to a vertx shared data map/set.
     *  @param wrappedMetrics - Metrics object to wrap.
     */
    public SharedMetrics(final Metrics wrappedMetrics) {
        _wrappedMetrics = wrappedMetrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Counter createCounter(final String name) {
        return _wrappedMetrics.createCounter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name) {
        _wrappedMetrics.incrementCounter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name, final long value) {
        _wrappedMetrics.incrementCounter(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name) {
        _wrappedMetrics.decrementCounter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name, final long value) {
        _wrappedMetrics.decrementCounter(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetCounter(final String name) {
        _wrappedMetrics.resetCounter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final String name) {
        return _wrappedMetrics.createTimer(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTimer(final String name) {
        _wrappedMetrics.startTimer(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTimer(final String name) {
        _wrappedMetrics.stopTimer(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final TimeUnit unit) {
        _wrappedMetrics.setTimer(name, duration, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final Unit unit) {
        _wrappedMetrics.setTimer(name, duration, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value) {
        _wrappedMetrics.setGauge(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value, final Unit unit) {
        _wrappedMetrics.setGauge(name, value, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value) {
        _wrappedMetrics.setGauge(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value, final Unit unit) {
        _wrappedMetrics.setGauge(name, value, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void annotate(final String key, final String value) {
        _wrappedMetrics.annotate(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return _wrappedMetrics.isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        _wrappedMetrics.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Instant getOpenTime() {
        return _wrappedMetrics.getOpenTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Instant getCloseTime() {
        return _wrappedMetrics.getCloseTime();
    }

    private final Metrics _wrappedMetrics;
}
