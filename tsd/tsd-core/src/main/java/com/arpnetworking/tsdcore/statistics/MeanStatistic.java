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
package com.arpnetworking.tsdcore.statistics;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Takes the mean of the entries.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class MeanStatistic extends BaseStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "mean";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculate(final List<Quantity> orderedValues) {
        // TODO(vkoskela): Statistic calculation should be allowed to either fail or not return a quantity. [MAI-?]
        if (orderedValues.size() == 0) {
            return ZERO;
        }
        double sum = 0;
        Optional<Unit> unit = Optional.absent();
        for (final Quantity sample : orderedValues) {
            sum += sample.getValue();
            unit = unit.or(sample.getUnit());
        }
        return new Quantity.Builder().setValue(sum / orderedValues.size()).setUnit(unit.orNull()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculateAggregations(final List<AggregatedData> aggregations) {
        double weighted = 0D;
        long samples = 0L;
        Optional<Unit> unit = Optional.absent();
        for (final AggregatedData aggregation : aggregations) {
            weighted += aggregation.getValue().getValue() * aggregation.getPopulationSize();
            samples += aggregation.getPopulationSize();
            unit = unit.or(aggregation.getValue().getUnit());
        }
        return new Quantity.Builder()
                .setValue(weighted / samples)
                .setUnit(unit.orNull())
                .build();
    }

    private static final Quantity ZERO = new Quantity.Builder().setValue(0.0).build();
    private static final long serialVersionUID = 2943082617025777130L;
}
