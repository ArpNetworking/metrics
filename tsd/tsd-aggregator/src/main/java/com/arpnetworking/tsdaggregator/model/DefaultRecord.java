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
package com.arpnetworking.tsdaggregator.model;

import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import net.sf.oval.constraint.NotNull;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * Default implementation of the <code>Record</code> interface.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class DefaultRecord implements Record {

    /**
     * Public constructor.
     *
     * @param metrics the metrics
     * @param time the time stamp
     * @param annotations the annotations
     * @deprecated Use the <code>Builder</code> instead.
     */
    @Deprecated
    public DefaultRecord(final Map<String, ? extends Metric> metrics, final DateTime time, final Map<String, String> annotations) {
        _metrics = ImmutableMap.copyOf(metrics);
        _time = time;
        _annotations = ImmutableMap.copyOf(annotations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getTime() {
        return _time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ? extends Metric> getMetrics() {
        return _metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getAnnotations() {
        return _annotations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Record)) {
            return false;
        }

        final Record otherRecord = (Record) other;
        return Objects.equal(getMetrics(), otherRecord.getMetrics())
                && Objects.equal(getTime(), otherRecord.getTime())
                && Objects.equal(getAnnotations(), otherRecord.getAnnotations());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getMetrics(), getTime(), getAnnotations());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Metrics", _metrics)
                .add("Time", _time)
                .add("Annotations", _annotations)
                .toString();
    }

    // NOTE: Invoked through reflection by OvalBuilder
    @SuppressWarnings("unused")
    private DefaultRecord(final Builder builder) {
        _metrics = ImmutableMap.copyOf(builder._metrics);
        _time = builder._time;
        _annotations = ImmutableMap.copyOf(builder._annotations);
    }

    private final ImmutableMap<String, ? extends Metric> _metrics;
    private final DateTime _time;
    private final ImmutableMap<String, String> _annotations;

    /**
     * Implementation of builder pattern for <code>DefaultRecord</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<Record> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DefaultRecord.class);
        }

        /**
         * The named metrics <code>Map</code>. Cannot be null.
         *
         * @param value The named metrics <code>Map</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetrics(final Map<String, ? extends Metric> value) {
            _metrics = value;
            return this;
        }

        /**
         * The timestamp of the record. Cannot be null.
         *
         * @param value The timestamp.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setTime(final DateTime value) {
            _time = value;
            return this;
        }

        /**
         * The annotations <code>Map</code>. Cannot be null.
         *
         * @param value The annotations <code>Map</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAnnotations(final Map<String, String> value) {
            _annotations = value;
            return this;
        }

        @NotNull
        private Map<String, ? extends Metric> _metrics;
        @NotNull
        private DateTime _time;
        @NotNull
        private Map<String, String> _annotations;
    }
}
