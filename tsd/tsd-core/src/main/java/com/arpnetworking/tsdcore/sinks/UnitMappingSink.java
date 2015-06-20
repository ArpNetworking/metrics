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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of <code>Sink</code> which maps values in one unit to another.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class UnitMappingSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final List<AggregatedData> mappedData = Lists.newArrayListWithExpectedSize(data.size());
        for (final AggregatedData datum : data) {
            final Quantity value = datum.getValue();
            if (value.getUnit().isPresent()) {
                final Unit fromUnit = value.getUnit().get();
                final Unit toUnit = _map.get(fromUnit);
                if (toUnit != null) {
                    mappedData.add(
                            AggregatedData.Builder.<AggregatedData, AggregatedData.Builder>clone(datum)
                                    .setValue(new Quantity.Builder()
                                            .setValue(toUnit.convert(value.getValue(), fromUnit))
                                            .setUnit(toUnit)
                                            .build())
                                    .build());
                } else {
                    mappedData.add(datum);
                }
            } else {
                mappedData.add(datum);
            }
        }
        final List<Condition> mappedConditions = Lists.newArrayListWithExpectedSize(conditions.size());
        for (final Condition condition : conditions) {
            final Quantity threshold = condition.getThreshold();
            if (threshold.getUnit().isPresent()) {
                final Unit fromUnit = threshold.getUnit().get();
                final Unit toUnit = _map.get(fromUnit);
                if (toUnit != null) {
                    mappedConditions.add(
                            Condition.Builder.<Condition, Condition.Builder>clone(condition)
                                    .setThreshold(new Quantity.Builder()
                                            .setValue(toUnit.convert(threshold.getValue(), fromUnit))
                                            .setUnit(toUnit)
                                            .build())
                                    .build());
                } else {
                    mappedConditions.add(condition);
                }
            } else {
                mappedConditions.add(condition);
            }
        }
        _sink.recordAggregateData(mappedData, mappedConditions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        _sink.close();
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "super", super.toLogValue(),
                "Sink", _sink,
                "Map", _map);
    }

    private UnitMappingSink(final Builder builder) {
        super(builder);
        _map = Maps.newHashMap(builder._map);
        _sink = builder._sink;
    }

    private final Map<Unit, Unit> _map;
    private final Sink _sink;

    /**
     * Implementation of builder pattern for <code>UnitMappingSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, UnitMappingSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(UnitMappingSink.class);
        }

        /**
         * The map of unit to unit. Cannot be null.
         *
         * @param value The map of unit to unit.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMap(final Map<Unit, Unit> value) {
            _map = value;
            return self();
        }

        /**
         * The sink to wrap. Cannot be null.
         *
         * @param value The sink to wrap.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private Map<Unit, Unit> _map;
        @NotNull
        private Sink _sink;
    }
}
