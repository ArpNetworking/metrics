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

import java.text.DecimalFormat;
import java.util.List;

/**
 * Base class for percentile based statistics.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public abstract class TPStatistic extends BaseStatistic implements OrderedStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "tp" + FORMAT.format(_percentile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double calculate(final List<Quantity> orderedValues) {
        final int index = (int) (Math.ceil((_percentile / 100) * (orderedValues.size() - 1)));
        return Double.valueOf(orderedValues.get(index).getValue());
    }

    /**
     * Protected constructor.
     * 
     * @param percentile The percentile value to compute.
     */
    protected TPStatistic(final double percentile) {
        _percentile = percentile;
    }

    private final double _percentile;

    private static final DecimalFormat FORMAT = new DecimalFormat("##0.#");
}
