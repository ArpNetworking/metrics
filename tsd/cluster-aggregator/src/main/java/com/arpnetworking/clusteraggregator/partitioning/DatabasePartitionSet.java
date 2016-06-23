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

import com.arpnetworking.clusteraggregator.models.ebean.Partition;
import com.arpnetworking.clusteraggregator.models.ebean.PartitionEntry;
import com.arpnetworking.utility.Database;
import com.arpnetworking.utility.partitioning.PartitionSet;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import com.google.common.base.Optional;

import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * A partition set that is backed by an eBean database.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class DatabasePartitionSet implements PartitionSet {
    /**
     * Public constructor.
     *
     * @param database The database to use to back the data
     * @param partitionSet The partition set model backing this instance
     */
    public DatabasePartitionSet(
            final Database database,
            final com.arpnetworking.clusteraggregator.models.ebean.PartitionSet partitionSet) {
        _database = database;
        _partitionSetBean = partitionSet;
        _maxEntriesPerPartition = partitionSet.getMaximumEntriesPerPartition();
        _maxPartitions = partitionSet.getMaximumPartitions();
        _ebean = _database.getEbeanServer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getExistingPartition(final String key) {
        final PartitionEntry partitionEntry = PartitionEntry.findByKey(key, _partitionSetBean, _database);
        if (partitionEntry != null) {
            return Optional.of(partitionEntry.getPartition().getPartitionNumber());
        }
        return Optional.absent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getOrCreatePartition(final String key) {
        try (final Transaction transaction = _database.getEbeanServer().beginTransaction()) {
            PartitionEntry partitionEntry = PartitionEntry.findByKey(key, _partitionSetBean, _database);
            if (partitionEntry != null) {
                return Optional.of(partitionEntry.getPartition().getPartitionNumber());
            }
            if (_partitionSetBean.getFull()) {
                return Optional.absent();
            }

            _partitionSetBean.lock(_database);
            _ebean.refresh(_partitionSetBean);
            // retry again after lock, it might exist now
            partitionEntry = PartitionEntry.findByKey(key, _partitionSetBean, _database);
            if (partitionEntry != null) {
                return Optional.of(partitionEntry.getPartition().getPartitionNumber());
            }
            final Integer highestPartition = _partitionSetBean.getCount();
            Partition partition = Partition.getPartition(_partitionSetBean, highestPartition, _database);

            if (partition == null || partition.getCount() >= _maxEntriesPerPartition) {
                // Make a new partition unless we're already at the max
                if (highestPartition >= _maxPartitions) {
                    _partitionSetBean.setFull(true);
                    _ebean.save(_partitionSetBean);
                    return Optional.absent();
                }
                partition = new Partition();
                partition.setPartitionNumber(highestPartition + 1);
                partition.setPartitionSet(_partitionSetBean);
                partition.setCount(0);
                _partitionSetBean.setCount(partition.getPartitionNumber());
                _ebean.save(_partitionSetBean);
            }

            partition.setCount(partition.getCount() + 1);
            _ebean.save(partition);

            createEntry(key, partition);
            transaction.commit();
            return Optional.of(partition.getPartitionNumber());
        } catch (final IOException e) {
            return Optional.absent();
        }
    }

    private void createEntry(@Nonnull final String key, final Partition partition) {
        final PartitionEntry entry = new PartitionEntry();
        entry.setKey(key);
        entry.setPartition(partition);
        _ebean.save(entry);
    }

    private final int _maxEntriesPerPartition;
    private final int _maxPartitions;
    private final Database _database;
    private final com.arpnetworking.clusteraggregator.models.ebean.PartitionSet _partitionSetBean;
    private final EbeanServer _ebean;
}
