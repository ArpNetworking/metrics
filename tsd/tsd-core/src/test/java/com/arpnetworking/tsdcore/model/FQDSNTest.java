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
package com.arpnetworking.tsdcore.model;

import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.TP50Statistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.arpnetworking.utility.test.BuildableEqualsAndHashCodeTester;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the <code>FQDSN</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class FQDSNTest {

    @Test
    public void testBuilder() {
        final String expectedCluster = "MyCluster";
        final String expectedService = "MyService";
        final String expectedMetric = "MyMetric";
        final Statistic expectedStatistic = new TP99Statistic();

        final FQDSN fqdsn = new FQDSN.Builder()
                .setCluster(expectedCluster)
                .setService(expectedService)
                .setMetric(expectedMetric)
                .setStatistic(expectedStatistic)
                .build();


        Assert.assertEquals(expectedCluster, fqdsn.getCluster());
        Assert.assertEquals(expectedService, fqdsn.getService());
        Assert.assertEquals(expectedMetric, fqdsn.getMetric());
        Assert.assertEquals(expectedStatistic, fqdsn.getStatistic());
    }

    @Test
    public void testEqualsAndHashCode() {
        BuildableEqualsAndHashCodeTester.assertEqualsAndHashCode(
                new FQDSN.Builder()
                        .setCluster("MyClusterA")
                        .setService("MyServiceA")
                        .setMetric("MyMetricA")
                        .setStatistic(new TP99Statistic()),
                new FQDSN.Builder()
                        .setCluster("MyClusterB")
                        .setService("MyServiceB")
                        .setMetric("MyMetricB")
                        .setStatistic(new TP50Statistic()));
    }

    @Test
    public void testToString() {
        final String asString = new FQDSN.Builder()
                .setCluster("MyClusterA")
                .setService("MyServiceA")
                .setMetric("MyMetricA")
                .setStatistic(new TP99Statistic())
                .build()
                .toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }
}
