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
package com.arpnetworking.tsdcore.statistics;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.collect.Lists;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for the SumStatistic class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SumStatisticTest {
    @Test
    public void testConstruction() {
        new SumStatistic();
    }

    @Test
    public void testGetName() {
        final SumStatistic stat = new SumStatistic();
        Assert.assertThat(stat.getName(), Matchers.equalTo("sum"));
    }

    @Test
    public void testCalculate() {
        final SumStatistic stat = new SumStatistic();
        final List<Double> doubleVals = Lists.newArrayList(12d, 18d, 5d);
        final List<Quantity> vals = TestBeanFactory.createSamples(doubleVals);
        final Quantity calculated = stat.calculate(vals);
        Assert.assertThat(
                calculated,
                Matchers.equalTo(
                        new Quantity.Builder()
                                .setValue(35.0)
                                .setUnit(Unit.MILLISECOND)
                                .build()));
    }

    @Test
    public void testEquality() {
        Assert.assertFalse(new SumStatistic().equals(null));
        Assert.assertFalse(new SumStatistic().equals("ABC"));
        Assert.assertTrue(new SumStatistic().equals(new SumStatistic()));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(new SumStatistic().hashCode(), new SumStatistic().hashCode());
    }
}
