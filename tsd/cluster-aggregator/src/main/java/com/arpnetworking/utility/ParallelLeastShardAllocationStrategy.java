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
package com.arpnetworking.utility;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.cluster.sharding.ShardCoordinator;
import akka.dispatch.Futures;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import scala.collection.JavaConversions;
import scala.collection.immutable.IndexedSeq;
import scala.concurrent.Future;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Implementation of the least shard allocation strategy that seeks to parallelize shard rebalancing.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ParallelLeastShardAllocationStrategy extends ShardCoordinator.AbstractShardAllocationStrategy {

    /**
     * Public constructor.
     *
     * @param maxParallel number of allocations to start in parallel
     * @param rebalanceThreshold difference in number of shards required to cause a rebalance
     * @param notify the {@link akka.actor.ActorSelection} selection to notify of changes
     */
    public ParallelLeastShardAllocationStrategy(
            final int maxParallel,
            final int rebalanceThreshold,
            final Optional<ActorSelection> notify) {
        _maxParallel = maxParallel;
        _rebalanceThreshold = rebalanceThreshold;
        _notify = notify;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<ActorRef> allocateShard(
            final ActorRef requester,
            final String shardId,
            final Map<ActorRef, IndexedSeq<String>> currentShardAllocations) {
        // If we already decided where this goes, return the destination
        if (_pendingRebalances.containsKey(shardId)) {
            return Futures.successful(_pendingRebalances.get(shardId));
        }

        // Otherwise default to giving it to the shard with the least amount of shards
        return Futures.successful(currentShardAllocations
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().size()))
                .findFirst()
                .get()
                .getKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Set<String>> rebalance(
            final Map<ActorRef, IndexedSeq<String>> currentShardAllocations,
            final Set<String> rebalanceInProgress) {
        // Only keep the rebalances that are in progress
        _pendingRebalances.keySet().retainAll(rebalanceInProgress);

        // Build a friendly set of current allocations
        // Sort the set by "effective shards after rebalance"
        final TreeSet<RegionShardAllocations> allocations =
                new TreeSet<>(Comparator.comparingInt(RegionShardAllocations::getEffectiveShardCount));

        for (final Map.Entry<ActorRef, IndexedSeq<String>> entry : currentShardAllocations.entrySet()) {
            allocations.add(
                    new RegionShardAllocations(
                            entry.getKey(),
                            // Only count the shards that are not currently rebalancing
                            JavaConversions.setAsJavaSet(entry.getValue().<String>toSet())
                                    .stream()
                                    .filter(e -> !rebalanceInProgress.contains(e))
                                    .collect(Collectors.toSet())));
        }

        final Set<String> toRebalance = Sets.newHashSet();

        for (int x = 0; x < _maxParallel - rebalanceInProgress.size(); x++) {
            // Note: the poll* functions remove the item from the set
            final RegionShardAllocations leastShards = allocations.pollFirst();
            final RegionShardAllocations mostShards = allocations.pollLast();


            // Make sure that we have more than 1 region
            if (mostShards == null) {
                LOGGER.debug()
                        .setMessage("Cannot rebalance shards, less than 2 shard regions found.")
                        .log();
                break;
            }

            // Make sure that the difference is enough to warrant a rebalance
            if (mostShards.getEffectiveShardCount() - leastShards.getEffectiveShardCount() < _rebalanceThreshold) {
                LOGGER.debug()
                        .setMessage("Not rebalancing any (more) shards, shard region with most shards already balanced with least")
                        .addData("most", mostShards.getEffectiveShardCount())
                        .addData("least", leastShards.getEffectiveShardCount())
                        .addData("rebalanceThreshold", _rebalanceThreshold)
                        .log();
                break;
            }

            final String rebalanceShard = Iterables.get(mostShards.getShards(), 0);

            // Now we take a shard from mostShards and give it to leastShards
            mostShards.removeShard(rebalanceShard);
            leastShards.incrementIncoming();
            toRebalance.add(rebalanceShard);
            _pendingRebalances.put(rebalanceShard, leastShards.getRegion());

            // Put them back in the list with their new counts
            allocations.add(mostShards);
            allocations.add(leastShards);
        }

        // Transform the currentShardAllocations to a Map<ActorRef, Set<String>> from the
        // Scala representation
        final Map<ActorRef, Set<String>> currentAllocations = Maps.transformValues(
                currentShardAllocations,
                e -> Sets.newHashSet(JavaConversions.seqAsJavaList(e)));

        final RebalanceNotification notification = new RebalanceNotification(
                currentAllocations,
                rebalanceInProgress,
                _pendingRebalances);
        LOGGER.debug()
                .setMessage("Broadcasting rebalance info")
                .addData("target", _notify)
                .addData("shardAllocations", notification)
                .log();
        if (_notify.isPresent()) {
            _notify.get().tell(notification, ActorRef.noSender());
        }
        return Futures.successful(toRebalance);
    }

    private Map<String, ActorRef> _pendingRebalances = Maps.newHashMap();

    private final int _maxParallel;
    private final int _rebalanceThreshold;
    private final Optional<ActorSelection> _notify;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelLeastShardAllocationStrategy.class);

    /**
     * Notification message that contains rebalance status.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static final class RebalanceNotification implements Serializable {
        /**
         * Public constructor.
         *
         * @param currentAllocations current allocations
         * @param inflightRebalances shards that are currently in the process of rebalancing
         * @param pendingRebalances current and pending rebalances and their destination
         */
        public RebalanceNotification(
                final Map<ActorRef, Set<String>> currentAllocations,
                final Set<String> inflightRebalances,
                final Map<String, ActorRef> pendingRebalances) {
            _currentAllocations = ImmutableMap.copyOf(currentAllocations);
            _inflightRebalances = ImmutableSet.copyOf(inflightRebalances);
            _pendingRebalances = ImmutableMap.copyOf(pendingRebalances);
            _timestamp = DateTime.now();
        }

        public Map<ActorRef, Set<String>> getCurrentAllocations() {
            return _currentAllocations;
        }

        public Set<String> getInflightRebalances() {
            return _inflightRebalances;
        }

        public DateTime getTimestamp() {
            return _timestamp;
        }

        public Map<String, ActorRef> getPendingRebalances() {
            return _pendingRebalances;
        }

        private final ImmutableMap<ActorRef, Set<String>> _currentAllocations;
        private final ImmutableSet<String> _inflightRebalances;
        private final ImmutableMap<String, ActorRef> _pendingRebalances;
        private final DateTime _timestamp;

        private static final long serialVersionUID = 1L;
    }

    private static final class RegionShardAllocations {
        private RegionShardAllocations(final ActorRef region, final Set<String> shards) {
            _region = region;
            _shards = Sets.newHashSet(shards);
        }

        public ActorRef getRegion() {
            return _region;
        }

        public Set<String> getShards() {
            return Collections.unmodifiableSet(_shards);
        }

        public int getEffectiveShardCount() {
            return _shards.size() + _incomingShardsCount;
        }

        public void removeShard(final String shard) {
            _shards.remove(shard);
        }

        public void incrementIncoming() {
            _incomingShardsCount++;
        }

        private int _incomingShardsCount = 0;

        private final ActorRef _region;
        private final Set<String> _shards;
    }
}
