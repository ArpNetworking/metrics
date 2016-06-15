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
package com.arpnetworking.clusteraggregator.models.ebean;

import com.arpnetworking.utility.Database;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.annotation.UpdatedTimestamp;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Model that holds the aggregate partitions for a {@link PartitionSet}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Entity
@Table(name = "partition_set", schema = "clusteragg")
public final class PartitionSet {
    /**
     * Finds or creates a partition set. NOTE: if the partition set is found, the values for
     * maximumEntriesPerPartition and maximumPartitions are used from the model and *NOT*
     * from the parameters.  Once the model is created, these values cannot change.
     *
     * @param name the name of the partition
     * @param database the database backing the data
     * @param maximumEntriesPerPartition the maximum number of entries in a partition
     * @param maximumPartitions the maximum number of partitions in the partition set
     * @return a partition set with the given name
     */
    public static PartitionSet findOrCreate(
            final String name,
            final Database database,
            final int maximumEntriesPerPartition,
            final int maximumPartitions) {
        PartitionSet partitionSet = database.getEbeanServer().find(PartitionSet.class).where().eq("name", name).findUnique();
        if (partitionSet != null) {
            return partitionSet;
        } else {
            try {
                final PartitionSet newSet = new PartitionSet();
                newSet.setName(name);
                newSet.setCount(0);
                newSet.setMaximumEntriesPerPartition(maximumEntriesPerPartition);
                newSet.setMaximumPartitions(maximumPartitions);
                newSet.setFull(false);
                database.getEbeanServer().save(newSet);
                return newSet;
            } catch (final PersistenceException ex) {
                partitionSet = database.getEbeanServer().find(PartitionSet.class).where().eq("name", name).findUnique();
                // This is safe because we will never delete.  In essence, we're looking for a unique constraint
                // violation by looking for the record we expect is there.
                if (partitionSet == null) {
                    throw ex;
                }
                return partitionSet;
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long value) {
        id = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Timestamp value) {
        createdAt = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        name = value;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Timestamp value) {
        updatedAt = value;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long value) {
        version = value;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer value) {
        count = value;
    }

    public Integer getMaximumPartitions() {
        return maximumPartitions;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(final Boolean value) {
        full = value;
    }


    public Integer getMaximumEntriesPerPartition() {
        return maximumEntriesPerPartition;
    }

    /**
     * Locks the record with a select for update.
     *
     * @param database the database backing the data
     */
    public void lock(final Database database) {
        if (database.getEbeanServer().currentTransaction() == null) {
            throw new IllegalStateException("Must be in a transaction before locking");
        }
        database.getEbeanServer().find(PartitionSet.class).setForUpdate(true).where().eq("id", id).findUnique();
    }

    private void setMaximumPartitions(final Integer value) {
        maximumPartitions = value;
    }

    private void setMaximumEntriesPerPartition(final Integer value) {
        maximumEntriesPerPartition = value;
    }

    private PartitionSet() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @CreatedTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    @UpdatedTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "name")
    private String name;

    @Column(name = "count")
    private Integer count;

    @Column(name = "maximum_partitions")
    private Integer maximumPartitions;

    @Column(name = "maximum_entries_per_partition")
    private Integer maximumEntriesPerPartition;

    @Column(name = "is_full")
    private Boolean full;
}
