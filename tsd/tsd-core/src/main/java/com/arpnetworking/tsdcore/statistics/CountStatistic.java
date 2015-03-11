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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Counts the entries.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class CountStatistic extends BaseStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "count";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAliases() {
        return Collections.singleton("n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculate(final List<Quantity> unorderedValues) {
        return new Quantity.Builder().setValue((double) unorderedValues.size()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculateAggregations(final List<AggregatedData> aggregations) {
        double samples = 0;
        for (final AggregatedData aggregation : aggregations) {
            samples += aggregation.getPopulationSize();
        }
        return new Quantity.Builder().setValue(samples).build();
    }

    private static final long serialVersionUID = 983762187313397225L;
}
