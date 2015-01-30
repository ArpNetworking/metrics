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

package com.arpnetworking.clusteraggregator;

import com.arpnetworking.tsdcore.statistics.CountStatistic;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.SumStatistic;
import com.arpnetworking.tsdcore.statistics.TP0Statistic;
import com.arpnetworking.tsdcore.statistics.TP100Statistic;
import com.arpnetworking.tsdcore.statistics.TP50Statistic;
import com.arpnetworking.tsdcore.statistics.TP90Statistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.arpnetworking.tsdcore.statistics.TP99p9Statistic;
import com.google.common.base.Optional;

/**
 * Creates statistics.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StatisticFactory {
    /**
     * Creates a statistic from a name.
     *
     * @param statistic The name of the desired statistic.
     * @return A new <code>Statistic</code>.
     */
    public Optional<Statistic> createStatistic(final String statistic) {
        if (statistic.equalsIgnoreCase("tp99")
                || statistic.equalsIgnoreCase("p99")) {
            return Optional.<Statistic>of(new TP99Statistic());
        } else if (statistic.equalsIgnoreCase("tp99.9")
                || statistic.equalsIgnoreCase("p99.9")) {
            return Optional.<Statistic>of(new TP99p9Statistic());
        } else if (statistic.equalsIgnoreCase("tp90")
                || statistic.equalsIgnoreCase("p90")) {
            return Optional.<Statistic>of(new TP90Statistic());
        } else if (statistic.equalsIgnoreCase("tp0")
                || statistic.equalsIgnoreCase("p0")
                || statistic.equalsIgnoreCase("min")) {
            return Optional.<Statistic>of(new TP0Statistic());
        } else if (statistic.equalsIgnoreCase("tp100")
                || statistic.equalsIgnoreCase("p100")
                || statistic.equalsIgnoreCase("max")) {
            return Optional.<Statistic>of(new TP100Statistic());
        } else if (statistic.equalsIgnoreCase("tp50")
                || statistic.equalsIgnoreCase("p50")
                || statistic.equalsIgnoreCase("median")) {
            return Optional.<Statistic>of(new TP50Statistic());
        } else if (statistic.equalsIgnoreCase("mean")) {
            return Optional.<Statistic>of(new MeanStatistic());
        } else if (statistic.equalsIgnoreCase("sum")) {
            return Optional.<Statistic>of(new SumStatistic());
        } else if (statistic.equalsIgnoreCase("n")
                || statistic.equalsIgnoreCase("count")) {
            return Optional.<Statistic>of(new CountStatistic());
        }
        return Optional.absent();
    }
}
