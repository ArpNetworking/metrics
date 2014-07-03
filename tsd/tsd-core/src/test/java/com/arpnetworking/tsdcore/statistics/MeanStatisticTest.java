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

import java.util.Collections;
import java.util.List;

/**
 * Tests for the MeanStatistic class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class MeanStatisticTest {

    @Test
    public void testConstruction() {
        new MeanStatistic();
    }

    @Test
    public void testGetName() {
        final MeanStatistic stat = new MeanStatistic();
        Assert.assertThat(stat.getName(), Matchers.equalTo("mean"));
    }

    @Test
    public void testCalculate() {
        final MeanStatistic stat = new MeanStatistic();
        final List<Double> doubleVals = Lists.newArrayList(Double.valueOf(12d), Double.valueOf(20d), Double.valueOf(7d));
        final List<Quantity> vals = TestBeanFactory.createSamples(doubleVals);
        final Double calculated = stat.calculate(vals);
        Assert.assertThat(calculated, Matchers.equalTo(Double.valueOf(13d)));
    }

    @Test
    public void testCalculateWithNoEntries() {
        final MeanStatistic stat = new MeanStatistic();
        final List<Quantity> vals = Collections.emptyList();
        final Double calculated = stat.calculate(vals);
        Assert.assertThat(calculated, Matchers.equalTo(Double.valueOf(0d)));
    }

    @Test
    public void testEquality() {
        Assert.assertFalse(new MeanStatistic().equals(null));
        Assert.assertFalse(new MeanStatistic().equals("ABC"));
        Assert.assertTrue(new MeanStatistic().equals(new MeanStatistic()));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(new MeanStatistic().hashCode(), new MeanStatistic().hashCode());
    }
}
