/**
 * Copyright 2015 Groupon.com
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
import com.arpnetworking.tsdcore.model.CalculatedValue;
import com.arpnetworking.tsdcore.model.Quantity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Histogram statistic. This is a supporting statistic and does not produce
 * a value itself. It is used by percentile statistics as a common dependency.
 * Use <code>StatisticFactory</code> for construction.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class SamplesStatistic extends BaseStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "samples";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Calculator createCalculator() {
        return new SamplesAccumulator(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculate(final List<Quantity> values) {
        throw new UnsupportedOperationException("Unsupported operation: calculate(List<Quantity>)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity calculateAggregations(final List<AggregatedData> aggregations) {
        throw new UnsupportedOperationException("Unsupported operation: calculateAggregations(List<AggregatedData>)");
    }

    private SamplesStatistic() { }

    private static final long serialVersionUID = 7060886488604176233L;

    /**
     * Accumulator computing the histogram of values. There is a dependency on the
     * histogram accumulator from each percentile statistic's calculator.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    /* package private */ static final class SamplesAccumulator implements Accumulator<List<Quantity>> {

        /**
         * Public constructor.
         *
         * @param statistic The <code>Statistic</code>.
         */
        public SamplesAccumulator(final Statistic statistic) {
            _statistic = statistic;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Statistic getStatistic() {
            return _statistic;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Accumulator<List<Quantity>> accumulate(final Quantity quantity) {
            // TODO(vkoskela): Remove the sample based implementation. [NEXT]
            // Instead create and update an actual histogram.
            final int index = Collections.binarySearch(_data, quantity);
            _data.add(index >= 0 ? index : Math.abs(index + 1), quantity);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Accumulator<List<Quantity>> accumulate(final CalculatedValue<List<Quantity>> calculatedValue) {
            for (final Quantity datum : calculatedValue.getData()) {
                accumulate(datum);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CalculatedValue<List<Quantity>> calculate(final Map<Statistic, Calculator<?>> dependencies) {
            // TODO(vkoskela): Remove the sample based implementation. [NEXT]
            // Instead set the data to the underlying histogram's data.
            return new CalculatedValue.Builder<List<Quantity>>()
                    .setValue(new Quantity.Builder()
                            .setValue(1.0)
                            .build())
                    .setData(_data)
                    .build();
        }

        /**
         * Calculate the value at the specified percentile.
         *
         * @param percentile The desired percentile to calculate.
         * @return The value at the desired percentile.
         */
        public Quantity calculate(final double percentile) {
            final int index = (int) (Math.round((percentile / 100.0) * (_data.size() - 1)));
            return _data.get(index);
        }

        private final Statistic _statistic;
        private final List<Quantity> _data = new LinkedList<>();
    }
}
