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
package com.arpnetworking.clusteraggregator.partitioning;

import com.arpnetworking.clusteraggregator.models.ebean.PartitionSet;
import com.arpnetworking.utility.Database;
import com.arpnetworking.utility.TestDatabaseFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the DatabasePartitionSet class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DatabasePartitionSetTest {
    @Test
    public void multiplePartitionSetModels() {
        final Database db = new TestDatabaseFactory().create();
        final PartitionSet partitionSet1 = PartitionSet.findOrCreate("foo", db, 10, 10);
        final PartitionSet partitionSet2 = PartitionSet.findOrCreate("foo", db, 10, 10);
        Assert.assertEquals(partitionSet1.getId(), partitionSet2.getId());
    }
}
