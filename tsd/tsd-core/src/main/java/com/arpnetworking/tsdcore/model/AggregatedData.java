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
package com.arpnetworking.tsdcore.model;

import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;

/**
 * Serves as a data class for storing data for aggregated values after
 * computation.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggregatedData {

    public Period getPeriod() {
        return _period;
    }

    public Statistic getStatistic() {
        return _statistic;
    }

    public String getService() {
        return _service;
    }

    public String getHost() {
        return _host;
    }

    public double getValue() {
        return _value;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    public String getMetric() {
        return _metric;
    }

    public List<Quantity> getSamples() {
        return _samples;
    }

    public long getPopulationSize() {
        return _populationSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final AggregatedData other = (AggregatedData) object;

        return Double.compare(other._value, _value) == 0
                && Objects.equal(_host, other._host)
                && Objects.equal(_metric, other._metric)
                && Objects.equal(_period, other._period)
                && Objects.equal(_periodStart, other._periodStart)
                && Objects.equal(_service, other._service)
                && Objects.equal(_statistic, other._statistic)
                && Long.compare(_populationSize, other._populationSize) == 0
                && Objects.equal(_samples, other._samples);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(
                getStatistic(),
                getService(),
                getHost(),
                getMetric(),
                Double.valueOf(getValue()),
                getPeriodStart(),
                getPeriod(),
                getSamples(),
                Long.valueOf(getPopulationSize()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("Statistic", _statistic)
                .add("Service", _service)
                .add("Host", _host)
                .add("Metric", _metric)
                .add("Value", _value)
                .add("PeriodStart", _periodStart)
                .add("Period", _period)
                .add("Samples", _samples)
                .add("PopulationSize", _populationSize)
                .toString();
    }

    private AggregatedData(final Builder builder) {
        _statistic = builder._statistic;
        _service = builder._service;
        _host = builder._host;
        _metric = builder._metric;
        _value = builder._value.doubleValue();
        _periodStart = builder._periodStart;
        _period = builder._period;
        _samples = ImmutableList.copyOf(builder._samples);
        _populationSize = builder._populationSize.longValue();
    }

    private final Statistic _statistic;
    private final String _service;
    private final String _host;
    private final String _metric;
    private final double _value;
    private final DateTime _periodStart;
    private final Period _period;
    private final long _populationSize;
    private final ImmutableList<Quantity> _samples;

    /**
     * Implementation of builder pattern for <code>AggregatedData</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<AggregatedData> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(AggregatedData.class);
        }

        /**
         * The <code>Statistic</code> instance. Cannot be null.
         *
         * @param value The <code>Statistic</code> instance.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStatistic(final Statistic value) {
            _statistic = value;
            return this;
        }

        /**
         * The service. Cannot be null or empty.
         *
         * @param value The service.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setService(final String value) {
            _service = value;
            return this;
        }

        /**
         * The host. Cannot be null or empty.
         *
         * @param value The host.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
            return this;
        }

        /**
         * The metric. Cannot be null or empty.
         *
         * @param value The metric.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetric(final String value) {
            _metric = value;
            return this;
        }

        /**
         * The value. Cannot be null.
         *
         * @param value The value.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setValue(final Double value) {
            _value = value;
            return this;
        }

        /**
         * The period start. Cannot be null.
         *
         * @param value The period start.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPeriodStart(final DateTime value) {
            _periodStart = value;
            return this;
        }

        /**
         * The period. Cannot be null.
         *
         * @param value The period.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPeriod(final Period value) {
            _period = value;
            return this;
        }

        /**
         * The samples. Cannot be null.
         *
         * @param value The samples.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSamples(final List<Quantity> value) {
            _samples = value;
            return this;
        }

        /**
         * The population size. Cannot be null.
         *
         * @param value The samples.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPopulationSize(final Long value) {
            _populationSize = value;
            return this;
        }

        @NotNull
        private Statistic _statistic;
        @NotNull
        @NotEmpty
        private String _service;
        @NotNull
        @NotEmpty
        private String _host;
        @NotNull
        @NotEmpty
        private String _metric;
        @NotNull
        private Double _value;
        @NotNull
        private DateTime _periodStart;
        @NotNull
        private Period _period;
        @NotNull
        private List<Quantity> _samples;
        @NotNull
        @Min(value = 0)
        private Long _populationSize;
    }
}
