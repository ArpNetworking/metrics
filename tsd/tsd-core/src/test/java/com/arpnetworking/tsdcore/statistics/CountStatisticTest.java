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
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for the CountStatistic class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class CountStatisticTest {
    @Test
    public void testConstruction() {
        new CountStatistic();
    }

    @Test
    public void testGetName() {
        final CountStatistic stat = new CountStatistic();
        Assert.assertThat(stat.getName(), Matchers.equalTo("count"));
    }

    @Test
    public void testCalculate() {
        final CountStatistic stat = new CountStatistic();
        final List<Double> doubleVals = Lists.newArrayList(Double.valueOf(12d), Double.valueOf(18d), Double.valueOf(5d));
        final List<Quantity> vals = TestBeanFactory.createSamples(doubleVals);
        final Double calculated = stat.calculate(vals);
        Assert.assertThat(calculated, Matchers.equalTo(Double.valueOf(3d)));
    }

    @Test
    public void testEquality() {
        Assert.assertFalse(new CountStatistic().equals(null));
        Assert.assertFalse(new CountStatistic().equals("ABC"));
        Assert.assertTrue(new CountStatistic().equals(new CountStatistic()));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(new CountStatistic().hashCode(), new CountStatistic().hashCode());
    }
}
