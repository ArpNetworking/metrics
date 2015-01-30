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
import com.google.common.collect.Lists;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Tests for {@link StatisticFactory}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@RunWith(JUnitParamsRunner.class)
public class StatisticFactoryTest {
    private Object[] args() {
        return a(
                a(l("mean", "Mean"), MeanStatistic.class),
                a(l("sum", "Sum"), SumStatistic.class),
                a(l("N", "n", "count", "Count"), CountStatistic.class),
                a(l("p0", "Tp0", "P0", "min", "Min"), TP0Statistic.class),
                a(l("p50", "Tp50", "P50", "median", "Median"), TP50Statistic.class),
                a(l("p90", "Tp90", "P90"), TP90Statistic.class),
                a(l("p99", "Tp99", "P99"), TP99Statistic.class),
                a(l("p99.9", "Tp99.9", "P99.9"), TP99p9Statistic.class),
                a(l("p100", "Tp100", "P100", "max", "Max"), TP100Statistic.class)
        );
    }

    private Object[] a(final Object... objects) {
        return objects;
    }

    @SafeVarargs
    public final <T> List<T> l(final T... objects) {
        return Lists.newArrayList(objects);
    }

    @Test
    @Parameters(method = "args")
    public void createStatistic(final List<String> names, final Class clazz) {
        final StatisticFactory factory = new StatisticFactory();

        for (final String name : names) {
            final Optional<Statistic> statistic = factory.createStatistic(name);
            Assert.assertTrue(statistic.isPresent());
            Assert.assertTrue(clazz.isInstance(statistic.get()));
        }
    }

    @Test
    public void noStatistic() {
        final StatisticFactory factory = new StatisticFactory();
        final Optional<Statistic> statistic = factory.createStatistic("notARealStatistic");
        Assert.assertFalse(statistic.isPresent());
    }
}
