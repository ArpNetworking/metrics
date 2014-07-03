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

import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.Sets;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;
import java.util.Set;

/**
 * Class representing a metric and a set of aggregations.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TSData {
    private final Set<TSAggregation> _aggregations = Sets.newHashSet();
    private final int _nAggregations;

    /**
     * Instantiates a new TS data.
     *
     * @param metricName the metric name
     * @param aggregations the aggregations
     * @param listener the listener
     * @param hostName the host name
     * @param serviceName the service name
     * @param statistics the statistics
     */
    public TSData(final String metricName, final Set<Period> aggregations, final Sink listener,
            final String hostName, final String serviceName, final Set<Statistic> statistics) {
        for (final Period period : aggregations) {
            _aggregations.add(new TSAggregation(metricName, period, listener, hostName, serviceName, statistics));
        }
        // Note _aggregations holds a TSAggregation for each requested time period. Each TSAggregation computes
        // what ever statistics are defined in statistics, therefore, the total number aggregations maintained in
        // this TSData is nPeriods * nStats or:
        _nAggregations = aggregations.size() * statistics.size();
    }

    /**
     * Add a metric to the aggregations.
     *
     * @param data the data
     * @param time the time
     */
    public void addMetric(final List<Quantity> data, final DateTime time) {
        for (final TSAggregation aggregation : _aggregations) {
            for (final Quantity val : data) {
                aggregation.addSample(val, time);
            }
        }
    }

    /**
     * Check for the need to rotate the aggregations.
     *
     * @param rotateFactor the rotate factor
     */
    public void checkRotate(final double rotateFactor) {
        for (final TSAggregation agg : _aggregations) {
            agg.checkRotate(rotateFactor);
        }
    }

    /**
     * Close the aggregations.
     */
    public void close() {
        for (final TSAggregation aggregation : _aggregations) {
            aggregation.close();
        }
    }

    /**
     * N aggregations.
     *
     * @return the int
     */
    // nStatistics * nAggregationPeriods
    public int nAggregations() {
        return _nAggregations;
    }
}
