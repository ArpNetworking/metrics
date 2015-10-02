/**
 * Copyright 2014 Brandon Arp
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

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Collection;
import java.util.Collections;

/**
 * Abstract base class for common functionality for publishing
 * <code>AggregatedData</code>. This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public abstract class BaseSink implements Sink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final String host;
        final Period period;
        final DateTime start;
        if (data.isEmpty()) {
            return;
        } else {
            final AggregatedData datum = data.iterator().next();
            host = datum.getHost();
            period = datum.getPeriod();
            start = datum.getStart();
        }
        final PeriodicData periodicData = new PeriodicData.Builder()
                .setData(ImmutableList.copyOf(data))
                .setConditions(ImmutableList.copyOf(conditions))
                .setDimensions(ImmutableMap.of("host", host))
                .setPeriod(period)
                .setStart(start)
                .build();
        recordAggregateData(periodicData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data) {
        recordAggregateData(data, Collections.emptyList());
    }

    public String getName() {
        return _name;
    }

    public String getMetricSafeName() {
        return getName().replace("/", "_").replace(".", "_");
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("name", _name)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    /**
     * Protected constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    protected BaseSink(final Builder<?, ?> builder) {
        _name = builder._name;
    }

    private final String _name;

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final ImmutableSet<Statistic> ALL_STATISTICS = STATISTIC_FACTORY.getAllStatistics();

    /**
     * Base <code>Builder</code> implementation.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    protected abstract static class Builder<B extends Builder<B, S>, S extends Sink> extends OvalBuilder<S> {

        /**
         * Sets name. Cannot be null or empty.
         *
         * @param value The name.
         * @return This instance of <code>Builder</code>.
         */
        public final B setName(final String value) {
            _name = value;
            return self();
        }

        /**
         * Called by setters to always return appropriate subclass of
         * <code>Builder</code>, even from setters of base class.
         *
         * @return instance with correct <code>Builder</code> class type.
         */
        protected abstract B self();

        /**
         * Protected constructor for subclasses.
         *
         * @param targetClass The concrete type to be created by the builder of
         * <code>Sink</code> implementation.
         */
        protected Builder(final Class<? extends S> targetClass) {
            super(targetClass);
        }

        @NotNull
        @NotEmpty
        private String _name;
    }
}
