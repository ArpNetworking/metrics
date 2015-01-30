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

import com.arpnetworking.tsdcore.sinks.ConsoleSink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.Sets;

import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Tests the TSAggregation class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TSAggregationTest {

    @Test
    public void testContruct() {
        final Period period = new Period(0, 5, 0, 0);
        final Set<Statistic> stats = Sets.newHashSet();
        final TSAggregation agg = new TSAggregation(
                "test metric",
                period,
                new ConsoleSink.Builder()
                        .setName("console_sink")
                        .build(),
                "testHost",
                "testService",
                "cluster",
                stats);
        Assert.assertNotNull(agg);
    }
}
