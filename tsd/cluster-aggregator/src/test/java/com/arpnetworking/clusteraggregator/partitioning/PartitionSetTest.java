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

import com.arpnetworking.utility.partitioning.PartitionSet;
import com.arpnetworking.utility.partitioning.PartitionSetFactory;
import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for generic partition sets.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@RunWith(Parameterized.class)
public class PartitionSetTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new TestDatabasePartitionSetFactory()},
                {new TestInMemoryPartitionSetFactory()}
        });
    }

    public PartitionSetTest(final PartitionSetFactory factory) {
        _factory = factory;
    }

    @Test
    public void constructPartitionSet() {
        final PartitionSet partitionSet = _factory.create("constructPartitionSet", 10, 10);
        Assert.assertNotNull(partitionSet);
    }

    @Test
    public void incrementsPartition() {
        final PartitionSet partitionSet = _factory.create("incrementsPartition", 2, 10);
        final Optional<Integer> onePartition = partitionSet.getOrCreatePartition("one");
        final Optional<Integer> twoPartition = partitionSet.getOrCreatePartition("two");
        final Optional<Integer> threePartition = partitionSet.getOrCreatePartition("three");
        final Optional<Integer> fourPartition = partitionSet.getOrCreatePartition("four");
        Assert.assertTrue(onePartition.isPresent());
        Assert.assertEquals(1, onePartition.get().intValue());
        Assert.assertTrue(twoPartition.isPresent());
        Assert.assertEquals(1, twoPartition.get().intValue());
        Assert.assertTrue(threePartition.isPresent());
        Assert.assertEquals(2, threePartition.get().intValue());
        Assert.assertTrue(fourPartition.isPresent());
        Assert.assertEquals(2, fourPartition.get().intValue());
    }

    @Test
    public void respectsMaxCount() {
        final PartitionSet partitionSet = _factory.create("respectsMaxCount", 2, 2);
        final Optional<Integer> onePartition = partitionSet.getOrCreatePartition("one");
        final Optional<Integer> twoPartition = partitionSet.getOrCreatePartition("two");
        final Optional<Integer> threePartition = partitionSet.getOrCreatePartition("three");
        final Optional<Integer> fourPartition = partitionSet.getOrCreatePartition("four");
        final Optional<Integer> fivePartition = partitionSet.getOrCreatePartition("five");
        Assert.assertTrue(onePartition.isPresent());
        Assert.assertEquals(1, onePartition.get().intValue());
        Assert.assertTrue(twoPartition.isPresent());
        Assert.assertEquals(1, twoPartition.get().intValue());
        Assert.assertTrue(threePartition.isPresent());
        Assert.assertEquals(2, threePartition.get().intValue());
        Assert.assertTrue(fourPartition.isPresent());
        Assert.assertEquals(2, fourPartition.get().intValue());
        Assert.assertFalse(fivePartition.isPresent());
    }

    private final PartitionSetFactory _factory;
}
