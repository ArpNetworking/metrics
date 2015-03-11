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

import com.arpnetworking.clusteraggregator.models.BookkeeperData;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple tests for the data class {@link BookkeeperDataTest}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class BookkeeperDataTest {
    @Test
    public void buildAndCheckFields() {
        Assert.assertEquals(1, _data.getClusters());
        Assert.assertEquals(2, _data.getMetrics());
        Assert.assertEquals(3, _data.getServices());
        Assert.assertEquals(4, _data.getStatistics());
    }

    @Test
    public void toStringRendersFields() {
        final String asString = _data.toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }

    private final BookkeeperData _data = new BookkeeperData.Builder()
            .setClusters(1L)
            .setMetrics(2L)
            .setServices(3L)
            .setStatistics(4L)
            .build();
}
