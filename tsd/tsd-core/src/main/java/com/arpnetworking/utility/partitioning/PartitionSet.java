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
package com.arpnetworking.utility.partitioning;

import com.google.common.base.Optional;

/**
 * Represents a set of partitions and a way to register and retrieve the mappings.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface PartitionSet {
    /**
     * Will return an existing partition mapping, or Optional.absent if one does not exist.
     *
     * @param key the key to be partitioned
     * @return a partition number
     */
    Optional<Integer> getExistingPartition(final String key);

    /**
     * Will return the partition mapping, creating one if possible.  Will return Optional.absent()
     * if a partition is unable to be created for the key.
     *
     * @param key the key to be partitioned
     * @return optionally, a partition number
     */
    Optional<Integer> getOrCreatePartition(final String key);
}
