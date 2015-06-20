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
import com.arpnetworking.tsdcore.limiter.legacy.LegacyMetricsLimiter;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.utility.OvalBuilder;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;
import java.util.Map;

/**
 * Limits the number of unique aggregations that will be emitted by tracking
 * aggregations in a persisted data store.
 *
 * This implementation is a wrapper around the legacy <code>DefaultMetricsLimiter</code>
 * and its associated <code>MetricsLimiterStateManager</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DefaultMetricsLimiter implements MetricsLimiter {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean offer(final AggregatedData data, final DateTime time) {
        return _legacyLimiter.offer(data, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void launch() {
        _legacyLimiter = new LegacyMetricsLimiter.Builder()
                .setAgeOutThreshold(_ageOutThreshold.toStandardDuration())
                .setEnableStateAutoWriter(Boolean.TRUE)
                .setLoggingInterval(_stateFlushInterval.toStandardDuration())
                .setMaxAggregations(_maximumAggregations)
                .setStateManagerBuilder(
                        new com.arpnetworking.tsdcore.limiter.legacy.MetricsLimiterStateManager.Builder()
                                .setStateFile(_stateFile.toPath())
                                .setStateFileFlushInterval(_stateFlushInterval.toStandardDuration()))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        _legacyLimiter.close();
        _legacyLimiter = null;
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public synchronized Map<String, Object> toLogValue() {
        return LogValueMapFactory.of(
                "id", Integer.toHexString(System.identityHashCode(this)),
                "class", this.getClass(),
                "AgeOutThreshold", _ageOutThreshold,
                "LegacyLimiter", _legacyLimiter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private DefaultMetricsLimiter(final Builder builder) {
        _maximumAggregations = builder._maximumAggregations;
        _stateFile = builder._stateFile;
        _stateFlushInterval = builder._stateFlushInterval;
        _ageOutThreshold = builder._ageOutThreshold;
    }

    private final long _maximumAggregations;
    private final File _stateFile;
    private final Period _stateFlushInterval;
    private final Period _ageOutThreshold;

    private LegacyMetricsLimiter _legacyLimiter;

    /**
     * Builder for a MetricsLimiter.
     */
    public static final class Builder extends OvalBuilder<DefaultMetricsLimiter> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DefaultMetricsLimiter.class);
        }

        /**
         * Set the maximum aggregations. Required. Cannot be null; must be
         * greater than or equal to zero.
         *
         * @param value The maximum aggregations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaxAggregations(final Long value) {
            _maximumAggregations = value;
            return this;
        }

        /**
         * Set the state file. Required. Cannot be null or empty.
         *
         * @param value The state file.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFile(final File value) {
            _stateFile = value;
            return this;
        }

        /**
         * Set the state flush interval. Optional. The default is five minutes.
         * Cannot be null.
         *
         * @param value The state flush interval.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFlushInterval(final Period value) {
            _stateFlushInterval = value;
            return this;
        }

        /**
         * Set the age out threshold. Optional. The default is seven days.
         * Cannot be null.
         *
         * @param value The age out threshold.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAgeOutThreshold(final Period value) {
            _ageOutThreshold = value;
            return this;
        }

        @NotNull
        @Min(0)
        private Long _maximumAggregations;
        @NotNull
        @NotEmpty
        private File _stateFile;
        @NotNull
        private Period _stateFlushInterval = Period.minutes(5);
        @NotNull
        private Period _ageOutThreshold = Period.days(7);
    }
}
