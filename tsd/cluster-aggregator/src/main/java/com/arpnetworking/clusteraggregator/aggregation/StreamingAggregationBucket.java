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
package com.arpnetworking.clusteraggregator.aggregation;

import com.arpnetworking.clusteraggregator.models.CombinedMetricData;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.CalculatedValue;
import com.arpnetworking.tsdcore.statistics.Accumulator;
import com.arpnetworking.tsdcore.statistics.Calculator;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Container class that holds aggregation pending records.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StreamingAggregationBucket {

    /**
     * Public constructor.
     *
     * @param periodStart Start of the period for the bucket.
     */
    public StreamingAggregationBucket(final DateTime periodStart) {
        _periodStart = periodStart;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    /**
     * Computes all of the statistics in the bucket.
     *
     * @return a Map of the statistics.
     */
    @SuppressWarnings("unchecked")
    public Map<Statistic, CalculatedValue<?>> compute() {
        final Map<Statistic, CalculatedValue<?>> values = Maps.newHashMap();
        for (final Map.Entry<Statistic, Calculator<?>> entry : _data.entrySet()) {
            final Calculator<?> calculatorEntry = entry.getValue();
            final Statistic statistic = entry.getKey();
            try {
                values.put(statistic, calculatorEntry.calculate(_data));
            // CHECKSTYLE.OFF: IllegalCatch - Try to calculate anything that we can
            } catch (final Exception exception) {
            // CHECKSTYLE.ON: IllegalCatch
                LOGGER.error()
                        .setMessage("Error while calculating statistic")
                        .addData("statistic", statistic)
                        .addData("allStatistics", _data)
                        .setThrowable(exception)
                        .log();
            }
        }
        return values;
    }

    /**
     * Looks up a {@link Statistic} to see if it is user specified.
     *
     * @param statistic the statistic to lookup
     * @return true if the statistic is user specified, otherwise false
     */
    public boolean isSpecified(final Statistic statistic) {
        return _specified.getOrDefault(statistic, false);
    }

    /**
     * Add <code>AggregatedData</code> instance.
     *
     * @param datum The <code>AggregatedData</code> instance.
     */
    @SuppressWarnings("unchecked")
    public void update(final CombinedMetricData datum) {
        for (final Map.Entry<Statistic, CombinedMetricData.StatisticValue> entry : datum.getCalculatedValues().entrySet()) {
            final Statistic statistic = entry.getKey();
            try {
                final CombinedMetricData.StatisticValue statisticValue = entry.getValue();

                Calculator<?> calculatorEntry = _data.get(statistic);
                if (calculatorEntry == null) {
                    calculatorEntry = statistic.createCalculator();
                    _data.put(statistic, calculatorEntry);
                    _specified.put(statistic, statisticValue.getUserSpecified());
                }

                if (calculatorEntry instanceof Accumulator) {
                    final Accumulator accumulator = (Accumulator) calculatorEntry;
                    accumulator.accumulate(statisticValue.getValue());
                }
                final Boolean isSpecified = _specified.get(statistic);
                if (!isSpecified && statisticValue.getUserSpecified()) {
                    _specified.put(statistic, statisticValue.getUserSpecified());
                }
                // CHECKSTYLE.OFF: IllegalCatch - We want to make sure we catch all the exceptions
            } catch (final Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                LOGGER.warn().setMessage("unable to update bucket with data")
                        .addData("statistic", statistic)
                        .addData("metric", datum.getMetricName())
                        .setThrowable(e)
                        .log();
            }
        }
    }

    private final DateTime _periodStart;
    private final Map<Statistic, Calculator<?>> _data = Maps.newHashMap();
    private final Map<Statistic, Boolean> _specified = Maps.newHashMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingAggregationBucket.class);
}
