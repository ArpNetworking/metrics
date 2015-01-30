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
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.OrderedStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.SampleUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Holds samples, delegates calculations to statistics, and emits the calculated
 * statistics.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TSAggregation {

    /**
     * Public constructor.
     *
     * @param metric The name of the metric.
     * @param period The period of the aggregation.
     * @param listener The destination for completed aggregations.
     * @param hostName The name of the host.
     * @param serviceName The name of the service.
     * @param cluster The name of the cluster.
     * @param statistics The <code>Set</code> of statistics to compute.
     */
    public TSAggregation(
            final String metric,
            final Period period,
            final Sink listener,
            final String hostName,
            final String serviceName,
            final String cluster,
            final Set<Statistic> statistics) {
        _metric = metric;
        _period = period;
        addStatistics(statistics, _orderedStatistics, _unorderedStatistics);
        _hostName = hostName;
        _serviceName = serviceName;
        _listener = listener;
        _cluster = cluster;
    }

    private void addStatistics(final Set<Statistic> stats, final Set<Statistic> orderedStatsSet,
            final Set<Statistic> unorderedStatsSet) {
        for (final Statistic s : stats) {
            addStatistic(s, orderedStatsSet, unorderedStatsSet);
        }
    }

    private void addStatistic(final Statistic s, final Set<Statistic> orderedStatsSet,
            final Set<Statistic> unorderedStatsSet) {
        if (s instanceof OrderedStatistic) {
            orderedStatsSet.add(s);
        } else {
            unorderedStatsSet.add(s);
        }
    }

    /**
     * Add a sample to this aggregation.
     *
     * @param value The sample to add.
     * @param time The timestamp the sample was generated at.
     */
    public void addSample(final Quantity value, final DateTime time) {
        rotateAggregation(time);
        if (time.isBefore(_periodStart)) {
            LOGGER.trace("Not adding sample due to it being before the current agg period");
        }
        _samples.add(value);
        _numberOfSamples++;
        LOGGER.trace("Added sample to aggregation: time = " + time);
    }

    /**
     * Check for a rotation and rotate if necessary.
     *
     * @param rotateFactor The fraction of a period to wait beyond the end of a
     * period before rotating to the next period.
     */
    public void checkRotate(final double rotateFactor) {
        final Duration rotateDuration = Duration.millis(
                (long) (_period.toDurationFrom(_periodStart).getMillis() * rotateFactor));
        rotateAggregation(DateTime.now().minus(new Duration(rotateDuration)));
    }

    private void rotateAggregation(final DateTime time) {
        LOGGER.trace("Checking roll. Period is " + _period + ", Roll time is " + _periodStart.plus(_period));
        if (time.isAfter(_periodStart.plus(_period))) {
            //Calculate the start of the new aggregation
            LOGGER.debug("We're rolling");
            DateTime startPeriod = time.hourOfDay().roundFloorCopy();
            while (!(startPeriod.isBefore(time) && startPeriod.plus(_period).isAfter(time))
                    && (!startPeriod.equals(time))) {
                startPeriod = startPeriod.plus(_period);
            }
            LOGGER.debug("New start period is " + startPeriod);
            emitAggregations();
            _periodStart = startPeriod;
            _numberOfSamples = 0;
            _samples.clear();
        }
    }

    /**
     * Closes and emits the aggregation.
     */
    public void close() {
        emitAggregations();
    }

    private void emitAggregations() {
        if (_samples.size() == 0) {
            LOGGER.debug("Not emitting aggregations due to 0 samples");
            return;
        }
        LOGGER.debug("Emitting aggregations; " + _samples.size() + " samples");
        // Copy the samples list to share it with AggregatedData instances
        final List<Quantity> localSamples = Lists.newArrayList(_samples);
        // Unify the units on the sample list to compute aggregates
        final List<Quantity> unifiedSamples = SampleUtils.unifyUnits(localSamples);

        // Compute aggregates for statistics not requiring ordered samples
        final ArrayList<AggregatedData> aggregates = Lists.newArrayList();
        for (final Statistic stat : _unorderedStatistics) {
            final double value = stat.calculate(unifiedSamples);
            final AggregatedData data = new AggregatedData.Builder()
                    .setFQDSN(new FQDSN.Builder()
                        .setStatistic(stat)
                        .setMetric(_metric)
                        .setService(_serviceName)
                        .setCluster(_cluster)
                        .build())
                    .setHost(_hostName)
                    .setValue(new Quantity.Builder()
                            .setValue(Double.valueOf(value))
                            .setUnit(unifiedSamples.get(0).getUnit().orNull())
                            .build())
                    .setStart(_periodStart)
                    .setPeriod(_period)
                    .setSamples(localSamples)
                    .setPopulationSize(Long.valueOf(_numberOfSamples))
                    .build();
            aggregates.add(data);
        }

        // Compute aggregates for statistics requiring ordered samples
        if (_orderedStatistics.size() > 0) {
            final List<Quantity> sortedAndUnifiedSamples = Lists.newArrayList(unifiedSamples);
            Collections.sort(sortedAndUnifiedSamples);
            for (final Statistic stat : _orderedStatistics) {
                final double value = stat.calculate(sortedAndUnifiedSamples);
                final AggregatedData data = new AggregatedData.Builder()
                        .setFQDSN(new FQDSN.Builder()
                                .setStatistic(stat)
                                .setMetric(_metric)
                                .setService(_serviceName)
                                .setCluster(_cluster)
                                .build())
                        .setHost(_hostName)
                        .setValue(new Quantity.Builder()
                                .setValue(Double.valueOf(value))
                                .setUnit(unifiedSamples.get(0).getUnit().orNull())
                                .build())
                        .setStart(_periodStart)
                        .setPeriod(_period)
                        .setSamples(localSamples)
                        .setPopulationSize(Long.valueOf(_numberOfSamples))
                        .build();
                aggregates.add(data);
            }
        }
        LOGGER.debug("Writing " + aggregates.size() + " aggregation records");
        _listener.recordAggregateData(aggregates);
    }

    private final Period _period;
    private final ArrayList<Quantity> _samples = Lists.newArrayList();
    private final Set<Statistic> _orderedStatistics = Sets.newHashSet();
    private final Set<Statistic> _unorderedStatistics = Sets.newHashSet();
    private final String _metric;
    private final String _hostName;
    private final String _serviceName;
    private final String _cluster;
    private final Sink _listener;
    private long _numberOfSamples = 0;
    private DateTime _periodStart = new DateTime(0);

    private static final Logger LOGGER = Logger.getLogger(TSAggregation.class);
}
