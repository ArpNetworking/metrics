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

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the MedianStatistic class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class MedianStatisticTest {

    @Test
    public void testName() {
        Assert.assertEquals("median", new MedianStatistic().getName());
    }

    @Test
    public void testAliases() {
        final MedianStatistic statistic = new MedianStatistic();
        Assert.assertEquals(1, statistic.getAliases().size());
        Assert.assertEquals("tp50", Iterables.getFirst(statistic.getAliases(), null));
    }

    @Test
    public void testMedianStat() {
        final MedianStatistic tp = new MedianStatistic();
        final ArrayList<Double> vList = Lists.newArrayList();
        for (int x = 0; x < 100; ++x) {
            vList.add((double) x);
        }
        final List<Quantity> vals = TestBeanFactory.createSamples(vList);
        final Quantity calculated = tp.calculate(vals);
        Assert.assertThat(
                calculated,
                Matchers.equalTo(
                        new Quantity.Builder()
                                .setValue(50.0)
                                .setUnit(Unit.MILLISECOND)
                                .build()));
    }

    @Test
    public void testEquality() {
        Assert.assertFalse(new MedianStatistic().equals(null));
        Assert.assertFalse(new MedianStatistic().equals("ABC"));
        Assert.assertTrue(new MedianStatistic().equals(new MedianStatistic()));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(new MedianStatistic().hashCode(), new MedianStatistic().hashCode());
    }
}
