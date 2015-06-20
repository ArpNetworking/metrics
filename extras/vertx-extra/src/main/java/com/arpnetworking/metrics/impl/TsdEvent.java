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
package com.arpnetworking.metrics.impl;

import com.arpnetworking.metrics.Event;
import com.arpnetworking.metrics.Quantity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of <code>Event</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TsdEvent implements Event {

    /**
     * Public constructor.
     *
     * NOTE: This method does <b>not</b> perform a deep copy of the provided
     * data structures. Callers are expected to <b>not</b> modify these data
     * structures after passing them to this constructor. This is acceptable
     * since this class is for internal implementation only.
     *
     * @param annotations The annotations.
     * @param timerSamples The timer samples.
     * @param counterSamples The counter samples.
     * @param gaugeSamples The gauge samples.
     */
    public TsdEvent(
            final Map<String, String> annotations,
            final Map<String, List<Quantity>> timerSamples,
            final Map<String, List<Quantity>> counterSamples,
            final Map<String, List<Quantity>> gaugeSamples) {
        _annotations = annotations;
        _timerSamples = timerSamples;
        _counterSamples = counterSamples;
        _gaugeSamples = gaugeSamples;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getAnnotations() {
        return Collections.unmodifiableMap(_annotations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<Quantity>> getTimerSamples() {
        return Collections.unmodifiableMap(_timerSamples);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<Quantity>> getCounterSamples() {
        return Collections.unmodifiableMap(_counterSamples);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<Quantity>> getGaugeSamples() {
        return Collections.unmodifiableMap(_gaugeSamples);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TsdEvent)) {
            return false;
        }
        final TsdEvent otherEvent = (TsdEvent) other;
        return Objects.equals(_annotations, otherEvent._annotations)
                && Objects.equals(_counterSamples, otherEvent._counterSamples)
                && Objects.equals(_timerSamples, otherEvent._timerSamples)
                && Objects.equals(_gaugeSamples, otherEvent._gaugeSamples);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Objects.hashCode(_annotations);
        hash = hash * 31 + Objects.hashCode(_counterSamples);
        hash = hash * 31 + Objects.hashCode(_timerSamples);
        hash = hash * 31 + Objects.hashCode(_gaugeSamples);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format(
                "TsdEvent{Annotations=%s, TimerSamples=%s, CounterSamples=%s, GaugeSamples=%s}",
                _annotations,
                _timerSamples,
                _counterSamples,
                _gaugeSamples);
    }

    private TsdEvent(final Builder builder) {
        _annotations = Collections.unmodifiableMap(builder._annotations);
        _timerSamples = Collections.unmodifiableMap(builder._timerSamples);
        _gaugeSamples = Collections.unmodifiableMap(builder._gaugeSamples);
        _counterSamples = Collections.unmodifiableMap(builder._counterSamples);
    }

    private final Map<String, String> _annotations;
    private final Map<String, List<Quantity>> _timerSamples;
    private final Map<String, List<Quantity>> _counterSamples;
    private final Map<String, List<Quantity>> _gaugeSamples;

    /**
     * Builder implementation for <code>TsdEvent</code>.
     */
    public static final class Builder {

        /**
         * Builds an instance of <code>TsdEvent</code>.
         *
         * @return An instance of <code>TsdEvent</code>.
         */
        public Event build() {
            if (_annotations == null) {
                throw new IllegalArgumentException("Annotations cannot be null.");
            }
            if (_timerSamples == null) {
                throw new IllegalArgumentException("TimerSamples cannot be null.");
            }
            if (_counterSamples == null) {
                throw new IllegalArgumentException("CounterSamples cannot be null.");
            }
            if (_gaugeSamples == null) {
                throw new IllegalArgumentException("GaugeSamples cannot be null.");
            }
            return new TsdEvent(this);
        }

        /**
         * Sets the annotations.
         *
         * @param value A <code>Map</code> for annotations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAnnotations(final Map<String, String> value) {
            _annotations = Collections.unmodifiableMap(value);
            return this;
        }

        /**
         * Sets the timer samples.
         *
         * @param value A <code>Map</code> for timer samples.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setTimerSamples(final Map<String, List<Quantity>> value) {
            _timerSamples = Collections.unmodifiableMap(value);
            return this;
        }

        /**
         * Sets the counter samples.
         *
         * @param value A <code>Map</code> for counter samples.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCounterSamples(final Map<String, List<Quantity>> value) {
            _counterSamples = Collections.unmodifiableMap(value);
            return this;
        }

        /**
         * Sets the gauge samples.
         *
         * @param value A <code>Map</code> for gauge samples.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setGaugeSamples(final Map<String, List<Quantity>> value) {
            _gaugeSamples = Collections.unmodifiableMap(value);
            return this;
        }

        private Map<String, String> _annotations;
        private Map<String, List<Quantity>> _timerSamples;
        private Map<String, List<Quantity>> _counterSamples;
        private Map<String, List<Quantity>> _gaugeSamples;
    }
}
