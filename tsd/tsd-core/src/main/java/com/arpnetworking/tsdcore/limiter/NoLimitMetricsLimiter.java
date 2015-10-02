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
package com.arpnetworking.tsdcore.limiter;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;

import org.joda.time.DateTime;

/**
 * Implementation of <code>MetricsLimiter</code> which does not impose a limit
 * or track the number of metrics being created.
 *
 * @author Ville Koskela (ville at groupon dot com)
 */
public final class NoLimitMetricsLimiter implements MetricsLimiter {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean offer(
            final AggregatedData data,
            final DateTime time) {
        if (!_isRunning) {
            throw new IllegalStateException("Not launched!");
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void launch() {
        _isRunning = true;
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        _isRunning = false;
        // Nothing to do
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("isRunning", _isRunning)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private volatile boolean _isRunning = false;
}
