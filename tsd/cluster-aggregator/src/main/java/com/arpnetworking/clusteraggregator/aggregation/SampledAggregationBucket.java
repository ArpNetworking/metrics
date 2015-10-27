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

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

/**
 * Container class that holds aggregation pending records.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SampledAggregationBucket {

    /**
     * Public constructor.
     *
     * @param periodStart Start of the period for the bucket.
     */
    public SampledAggregationBucket(final DateTime periodStart) {
        _periodStart = periodStart;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    public List<AggregatedData> getAggregatedData() {
        return Collections.unmodifiableList(_aggregatedData);
    }

    /**
     * Add <code>AggregatedData</code> instance.
     *
     * @param datum The <code>AggregatedData</code> instance.
     */
    public void addAggregatedData(final AggregatedData datum) {
        _aggregatedData.add(datum);
        _isSpecified = _isSpecified || datum.isSpecified();
    }

    public boolean isSpecified() {
        return _isSpecified;
    }

    private final DateTime _periodStart;
    private final List<AggregatedData> _aggregatedData = Lists.newArrayList();
    private boolean _isSpecified = false;
}