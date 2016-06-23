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

/**
 * Factory to create an InMemoryPartitionSet.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TestInMemoryPartitionSetFactory implements PartitionSetFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionSet create(final String namespace, final int maxEntriesPerPartition, final int maxPartitions) {
        return new InMemoryPartitionSet(maxEntriesPerPartition, maxPartitions);
    }
}
