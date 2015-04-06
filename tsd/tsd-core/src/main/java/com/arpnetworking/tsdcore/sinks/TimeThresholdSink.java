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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A sink to filter old data.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class TimeThresholdSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {

        final Collection<AggregatedData> filtered;
        if (_logOnly) {
            filtered = data;
            data.forEach(_filterPredicate::test);
        } else {
            filtered = data.stream()
                    .filter(_filterPredicate)
                    .collect(Collectors.toList());
        }
        _sink.recordAggregateData(filtered, conditions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        _sink.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("ExcludedServices", _excludedServices)
                .add("Sink", _sink)
                .add("LogOnly", _logOnly)
                .add("Threshold", _threshold)
                .toString();
    }

    private TimeThresholdSink(final Builder builder) {
        super(builder);
        _excludedServices = Sets.newConcurrentHashSet(builder._excludedServices);
        _sink = builder._sink;
        _logOnly = builder._logOnly;
        _threshold = builder._threshold;
        _logger = (AggregatedData data) ->
                LOGGER.warn(String.format("%s: Dropped stale data; threshold=%s, data=%s", getName(), _threshold, data));
        _filterPredicate = new FilterPredicate(_threshold, _logger, _excludedServices);
    }

    private final Consumer<AggregatedData> _logger;
    private final Set<String> _excludedServices;
    private final Sink _sink;
    private final boolean _logOnly;
    private final Period _threshold;
    private final FilterPredicate _filterPredicate;
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeThresholdSink.class);

    private static final class FilterPredicate implements Predicate<AggregatedData> {
        public FilterPredicate(
                final Period freshnessThreshold,
                final Consumer<AggregatedData> excludedConsumer,
                final Set<String> excludedServices) {
            _freshnessThreshold = freshnessThreshold;
            _excludedConsumer = excludedConsumer;
            _excludedServices = excludedServices;
        }

        @Override
        public boolean test(final AggregatedData aggregatedData) {
            if (!aggregatedData.getPeriodStart().plus(aggregatedData.getPeriod()).plus(_freshnessThreshold).isAfterNow()
                    && !_excludedServices.contains(aggregatedData.getFQDSN().getService())) {
                _excludedConsumer.accept(aggregatedData);
                return false;
            }
            return true;
        }

        private final Period _freshnessThreshold;
        private final Consumer<AggregatedData> _excludedConsumer;
        private final Set<String> _excludedServices;
    }

    /**
     * Base <code>Builder</code> implementation.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, TimeThresholdSink> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(TimeThresholdSink.class);
        }

        /**
         * The aggregated data sink to filter. Cannot be null.
         *
         * @param value The aggregated data sink to filter.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * Sets excluded services.  Services in this set will never have their data dropped. Optional.
         * Cannot be null. Default is no excluded services.
         *
         * @param value The excluded services.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setExcludedServices(final Set<String> value) {
            _excludedServices = value;
            return self();
        }

        /**
         * Flag to only log violations instead of dropping data. Optional. Defaults to false.
         *
         * @param value true to log violations, but still pass data
         * @return This instance of <code>Builder</code>.
         */
        public Builder setLogOnly(final Boolean value) {
            _logOnly = value;
            return self();
        }

        /**
         * The freshness threshold to log or drop data. Required. Cannot be null.
         *
         * @param value The threshold for accepted data.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setThreshold(final Period value) {
            _threshold = value;
            return self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private Set<String> _excludedServices = Collections.emptySet();
        @NotNull
        private Sink _sink;
        @NotNull
        private Period _threshold;
        @NotNull
        private Boolean _logOnly = false;
    }
}
