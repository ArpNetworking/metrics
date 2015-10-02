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
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Histogram statistic. This is a supporting statistic and does not produce
 * a value itself. It is used by percentile statistics as a common dependency.
 * Use <code>StatisticFactory</code> for construction.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class HistogramStatistic extends BaseStatistic {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "histogram";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Accumulator<HistogramSupportingData> createCalculator() {
        return new HistogramAccumulator(this);
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

    private HistogramStatistic() { }

    private static final long serialVersionUID = 7060886488604176233L;

    /**
     * Accumulator computing the histogram of values. There is a dependency on the
     * histogram accumulator from each percentile statistic's calculator.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    /* package private */ static final class HistogramAccumulator implements Accumulator<HistogramSupportingData> {

        /**
         * Public constructor.
         *
         * @param statistic The <code>Statistic</code>.
         */
        public HistogramAccumulator(final Statistic statistic) {
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
        public Accumulator<HistogramSupportingData> accumulate(final Quantity quantity) {
            // TODO(barp): Convert to canonical unit. [NEXT]
            // Instead create and update an actual histogram.
            _data.getHistogram().recordValue(quantity.getValue());
            _data.setUnit(quantity.getUnit());
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Accumulator<HistogramSupportingData> accumulate(final CalculatedValue<HistogramSupportingData> calculatedValue) {
            _data.getHistogram().add(calculatedValue.getData().getHistogram());
            _data.setUnit(calculatedValue.getData().getUnit().or(_data._unit));
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CalculatedValue<HistogramSupportingData> calculate(final Map<Statistic, Calculator<?>> dependencies) {
            // TODO(vkoskela): Remove the sample based implementation. [NEXT]
            // Instead set the data to the underlying histogram's data.
            return new CalculatedValue.Builder<HistogramSupportingData>()
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
            return new Quantity.Builder()
                    .setValue(_data.getHistogram().getValueAtPercentile(percentile))
                    .setUnit(_data._unit.orNull())
                    .build();
        }

        private final Statistic _statistic;
        private final HistogramSupportingData _data = new HistogramSupportingData();
    }

    /**
     * Supporting data based on a histogram.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static final class HistogramSupportingData {
        /**
         * Public constructor.
         */
        public HistogramSupportingData() {
            this(new Histogram());
        }

        /**
         * Public constructor.
         *
         * @param histogram The histogram
         */
        public HistogramSupportingData(final Histogram histogram) {
            _histogram = histogram;
        }

        public Histogram getHistogram() {
            return _histogram;
        }

        public Optional<Unit> getUnit() {
            return _unit;
        }

        public void setUnit(final Optional<Unit> unit) {
            _unit = unit;
        }

        private Optional<Unit> _unit = Optional.absent();
        private final Histogram _histogram;
    }

    /**
     * A simple histogram implementation.
     */
    public static final class Histogram {

        /**
         * Records a value into the histogram.
         *
         * @param value The value of the entry.
         * @param count The number of entries at this value.
         */
        public void recordValue(final double value, final int count) {
            _data.merge(truncate(value), count, (i, j) -> i + j);
            _entriesCount += count;
        }

        /**
         * Records a value into the histogram.
         *
         * @param value The value of the entry.
         */
        public void recordValue(final double value) {
            recordValue(value, 1);
        }

        /**
         * Adds a histogram to this one.
         *
         * @param histogram The histogram to add to this one.
         */
        public void add(final Histogram histogram) {
            for (final Map.Entry<Double, Integer> entry : histogram._data.entrySet()) {
                _data.merge(entry.getKey(), entry.getValue(), (i, j) -> i + j);
            }
            _entriesCount += histogram._entriesCount;
        }

        /**
         * Gets the value of the bucket that corresponds to the percentile.
         *
         * @param percentile the percentile
         * @return The value of the bucket at the percentile.
         */
        public Double getValueAtPercentile(final double percentile) {
            final int target = (int) (_entriesCount * percentile / 100.0D);
            int accumulated = 0;
            for (final Map.Entry<Double, Integer> next : _data.entrySet()) {
                accumulated += next.getValue();
                if (accumulated > target) {
                    return next.getKey();
                }
            }
            return 0D;
        }

        public int getEntriesCount() {
            return _entriesCount;
        }

        public Set<Map.Entry<Double, Integer>> getValues() {
            return _data.entrySet();
        }

        private static double truncate(final double val) {
            final long mask = 0xffffe00000000000L;
            return Double.longBitsToDouble(Double.doubleToRawLongBits(val) & mask);
        }

        private int _entriesCount = 0;
        private final TreeMap<Double, Integer> _data = new TreeMap<>();
    }
}
