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
 * Top 100th percentile statistic (aka max).
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TP100Statistic extends TPStatistic {

    /**
     * Public constructor.
     */
    public TP100Statistic() {
        super(100d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "max";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculate(final List<Quantity> orderedValues) {
        return Double.valueOf(orderedValues.get(orderedValues.size() - 1).getValue()).doubleValue();
    }

    private static final long serialVersionUID = 4788356950823429496L;
}
