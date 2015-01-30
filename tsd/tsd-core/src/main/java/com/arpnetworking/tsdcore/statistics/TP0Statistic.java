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

import com.arpnetworking.tsdcore.model.Quantity;

import java.util.List;

/**
 * Top 0th percentile statistic (aka min).
 *
 * @author Brandon Arp (barp at groupon dot com)
 */

public class TP0Statistic extends TPStatistic {

    /**
     * Public constructor.
     */
    public TP0Statistic() {
        super(0d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "min";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculate(final List<Quantity> orderedValues) {
        return Double.valueOf(orderedValues.get(0).getValue()).doubleValue();
    }

    private static final long serialVersionUID = 107620025236661457L;
}
