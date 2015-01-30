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

import java.util.List;

/**
 * Container class that holds aggregation pending records.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggregationBucket {

    /**
     * Public constructor.
     *
     * @param periodStart Start of the period for the bucket.
     */
    public AggregationBucket(final DateTime periodStart) {
        _periodStart = periodStart;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    public List<AggregatedData> getAggregatedData() {
        return _aggregatedData;
    }

    private final DateTime _periodStart;
    private final List<AggregatedData> _aggregatedData = Lists.newArrayList();
}
