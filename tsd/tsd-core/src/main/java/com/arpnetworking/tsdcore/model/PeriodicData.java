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

import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Contains the data for a specific period in time.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class PeriodicData {

    public Period getPeriod() {
        return _period;
    }

    public DateTime getStart() {
        return _start;
    }

    public Map<String, String> getDimensions() {
        return Collections.unmodifiableMap(_dimensions);
    }

    public Collection<AggregatedData> getData() {
        return Collections.unmodifiableCollection(_dataIndexedByFQDSN.values());
    }

    /**
     * Lookup data by <code>FQDSN</code>.
     *
     * @param fqdsn The <code>FQDSN</code>.
     * @return <code>Optional</code> <code>AggregatedData</code> instance.
     */
    public Optional<AggregatedData> getDataByFQDSN(final FQDSN fqdsn) {
        return Optional.fromNullable(_dataIndexedByFQDSN.get(fqdsn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Period", _period)
                .add("Start", _start)
                .add("Dimensions", _dimensions)
                .add("Data", _dataIndexedByFQDSN.values())
                .toString();
    }

    private PeriodicData(final Builder builder) {
        _period = builder._period;
        _start = builder._start;
        _dimensions = builder._dimensions;

        _dataIndexedByFQDSN = Maps.uniqueIndex(builder._data, INDEX_BY_FQDSN);
    }

    private final Period _period;
    private final DateTime _start;
    private final Map<String, String> _dimensions;
    private final Map<FQDSN, AggregatedData> _dataIndexedByFQDSN;

    private static final Function<AggregatedData, FQDSN> INDEX_BY_FQDSN = new Function<AggregatedData, FQDSN>() {
        @Override
        public FQDSN apply(final AggregatedData datum) {
            assert datum != null : "AggregatedData cannot be null";
            return datum.getFQDSN();
        }
    };

    /**
     * <code>Builder</code> implementation for <code>PeriodicData</code>.
     */
    public static final class Builder extends OvalBuilder<PeriodicData> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(PeriodicData.class);
        }

        /**
         * Set the period. Required. Cannot be null.
         *
         * @param value The period.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPeriod(final Period value) {
            _period = value;
            return this;
        }

        /**
         * Set the start. Required. Cannot be null.
         *
         * @param value The start.
         * @return This <code>Builder</code> instance.
         */
        public Builder setStart(final DateTime value) {
            _start = value;
            return this;
        }

        /**
         * Set the dimensions. Optional. Cannot be null. Defaults to an empty <code>Map</code>.
         *
         * @param value The dimensions.
         * @return This <code>Builder</code> instance.
         */
        public Builder setDimensions(final Map<String, String> value) {
            _dimensions = Maps.newHashMap(value);
            return this;
        }

        /**
         * Add dimension. Optional. Cannot be null. Defaults to an empty <code>Map</code>.
         *
         * @param domain The domain.
         * @param value The value.
         * @return This <code>Builder</code> instance.
         */
        public Builder addDimension(final String domain, final String value) {
            if (_dimensions == null) {
                _dimensions = Maps.newHashMap();
            }
            _dimensions.put(domain, value);
            return this;
        }

        /**
         * Set the data. Optional. Cannot be null. Defaults to an empty <code>List</code>.
         *
         * @param value The data.
         * @return This <code>Builder</code> instance.
         */
        public Builder setData(final Collection<AggregatedData> value) {
            _data = Lists.newArrayList(value);
            return this;
        }

        /**
         * Add datum. Optional. Cannot be null. Defaults to an empty <code>List</code>.
         *
         * @param value The datum.
         * @return This <code>Builder</code> instance.
         */
        public Builder addData(final AggregatedData value) {
            if (_data == null) {
                _data = Lists.newArrayList();
            }
            _data.add(value);
            return this;
        }

        @NotNull
        private Period _period;
        @NotNull
        private DateTime _start;
        @NotNull
        private Map<String, String> _dimensions = Maps.newHashMap();
        @NotNull
        private Collection<AggregatedData> _data = Lists.newArrayList();
    }
}
