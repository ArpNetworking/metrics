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
 * Takes the sum of the entries.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SumStatistic extends BaseStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculate(final List<Quantity> unorderedValues) {
        double sum = 0d;
        for (final Quantity sample : unorderedValues) {
            sum += sample.getValue();
        }
        return sum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "sum";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculateAggregations(final List<AggregatedData> aggregations) {
        double sum = 0;
        Optional<Unit> unit = Optional.absent();
        for (final AggregatedData aggregation : aggregations) {
            sum += aggregation.getValue().getValue();
            unit = unit.or(aggregation.getValue().getUnit());
        }
        return new Quantity(sum, unit);
    }

    private static final long serialVersionUID = -1534109546290882210L;
}
