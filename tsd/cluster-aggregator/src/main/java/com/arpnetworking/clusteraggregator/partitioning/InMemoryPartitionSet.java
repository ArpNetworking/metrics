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
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory partition manager.  This class *is not* thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class InMemoryPartitionSet implements PartitionSet {
    /**
     * Public constructor.
     *
     * @param entriesPerPartition the number of entries per partition
     * @param maxPartitions the maximum number of partitions
     */
    public InMemoryPartitionSet(final int entriesPerPartition, final int maxPartitions) {
        _entriesPerPartition = entriesPerPartition;
        _maxPartitions = maxPartitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getExistingPartition(final String key) {
        return Optional.fromNullable(_mappings.get(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getOrCreatePartition(final String key) {
        if (_currentPartition > _maxPartitions) {
            return Optional.absent();
        }
        return Optional.of(_mappings.compute(key, (k, currentVal) -> {
                    Integer partition = currentVal;
                    if (currentVal == null) {
                        partition = _currentPartition;
                        _currentPartitionCount++;
                        if (_currentPartitionCount >= _entriesPerPartition) {
                            _currentPartition++;
                            _currentPartitionCount = 0;
                        }
                    }
                    return partition;
                }));
    }

    private int _currentPartition = 1;
    private int _currentPartitionCount = 0;
    private final int _entriesPerPartition;
    private final int _maxPartitions;
    private final ConcurrentMap<String, Integer> _mappings = Maps.newConcurrentMap();
}
