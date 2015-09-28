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

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import com.arpnetworking.clusteraggregator.models.ShardAllocation;
import com.arpnetworking.utility.ParallelLeastShardAllocationStrategy;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import scala.compat.java8.JFunction;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Caches the cluster state.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ClusterStatusCache extends UntypedActor {
    /**
     * Creates a {@link akka.actor.Props} for use in Akka.
     *
     * @param cluster The cluster to reference.
     * @return A new {@link akka.actor.Props}
     */
    public static Props props(final Cluster cluster) {
        return Props.create(ClusterStatusCache.class, cluster);
    }

    /**
     * Public constructor.
     *
     * @param cluster {@link akka.cluster.Cluster} whose state is cached
     */
    public ClusterStatusCache(final Cluster cluster) {
        _cluster = cluster;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() {
        final Scheduler scheduler = getContext()
                .system()
                .scheduler();
        _pollTimer = scheduler.schedule(
                Duration.apply(0, TimeUnit.SECONDS),
                Duration.apply(10, TimeUnit.SECONDS),
                getSelf(),
                POLL,
                getContext().system().dispatcher(),
                getSelf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        if (_pollTimer != null) {
            _pollTimer.cancel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (!getSender().equals(getSelf())) {
            // Public messages
            if (message instanceof ClusterEvent.CurrentClusterState) {
                _clusterState = Optional.of((ClusterEvent.CurrentClusterState) message);
            } else if (message instanceof GetRequest) {
                sendResponse(getSender());
            } else if (message instanceof ParallelLeastShardAllocationStrategy.RebalanceNotification) {
                final ParallelLeastShardAllocationStrategy.RebalanceNotification rebalanceNotification =
                        (ParallelLeastShardAllocationStrategy.RebalanceNotification) message;
                _rebalanceState = Optional.of(rebalanceNotification);
            } else {
                unhandled(message);
            }
        } else {
            // Private messages
            if (message.equals(POLL)) {
                _cluster.sendCurrentClusterState(getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    private void sendResponse(final ActorRef sender) {
        final StatusResponse response = new StatusResponse(
                _clusterState.or(_cluster.state()),
                _rebalanceState);
        sender.tell(response, self());
    }

    private static String hostFromActorRef(final ActorRef shardRegion) {
        return shardRegion.path()
                .address()
                .host()
                .getOrElse(JFunction.func(() -> "localhost"));
    }

    private final Cluster _cluster;
    private Optional<ClusterEvent.CurrentClusterState> _clusterState = Optional.absent();
    @Nullable
    private Cancellable _pollTimer;
    private Optional<ParallelLeastShardAllocationStrategy.RebalanceNotification> _rebalanceState = Optional.absent();

    private static final String POLL = "poll";

    /**
     * Request to get a cluster status.
     */
    public static final class GetRequest implements Serializable {
        private static final long serialVersionUID = 2804853560963013618L;
    }

    /**
     * Response to a cluster status request.
     */
    public static final class StatusResponse implements Serializable {

        /**
         * Public constructor.
         *
         * @param clusterState the cluster state
         * @param rebalanceNotification the last rebalance data
         */
        public StatusResponse(
                final ClusterEvent.CurrentClusterState clusterState,
                final Optional<ParallelLeastShardAllocationStrategy.RebalanceNotification> rebalanceNotification) {
            _clusterState = clusterState;

            if (rebalanceNotification.isPresent()) {
                final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = rebalanceNotification.get();

                // There may be a shard joining the cluster that is not in the currentAllocations list yet, but will
                // have pending rebalances to it.  Compute the set of all shard regions by unioning the current allocation list
                // with the destinations of the rebalances.
                final Set<ActorRef> allRefs = Sets.union(
                        notification.getCurrentAllocations().keySet(),
                        Sets.newHashSet(notification.getPendingRebalances().values()));

                final Map<String, ActorRef> pendingRebalances = notification.getPendingRebalances();

                final Map<ActorRef, Set<String>> currentAllocations = notification.getCurrentAllocations();

                _allocations = Optional.of(
                        allRefs.stream()
                                .map(shardRegion -> computeShardAllocation(pendingRebalances, currentAllocations, shardRegion))
                                .collect(Collectors.toList()));
            } else {
                _allocations = Optional.absent();
            }
        }

        private ShardAllocation computeShardAllocation(
                final Map<String, ActorRef> pendingRebalances,
                final Map<ActorRef, Set<String>> currentAllocations,
                final ActorRef shardRegion) {
            // Setup the map of current shard allocations
            final Set<String> currentShards = currentAllocations.getOrDefault(shardRegion, Collections.emptySet());


            // Setup the list of incoming shard allocations
            final Map<ActorRef, Collection<String>> invertPending = Multimaps
                    .invertFrom(Multimaps.forMap(pendingRebalances), ArrayListMultimap.create())
                    .asMap();
            final Set<String> incomingShards = Sets.newHashSet(invertPending.getOrDefault(shardRegion, Collections.emptyList()));

            // Setup the list of outgoing shard allocations
            final Set<String> outgoingShards = Sets.intersection(currentShards, pendingRebalances.keySet()).immutableCopy();

            // Remove the outgoing shards from the currentShards list
            currentShards.removeAll(outgoingShards);

            return new ShardAllocation.Builder()
                    .setCurrentShards(currentShards)
                    .setIncomingShards(incomingShards)
                    .setOutgoingShards(outgoingShards)
                    .setHost(hostFromActorRef(shardRegion))
                    .setShardRegion(shardRegion)
                    .build();
        }

        public ClusterEvent.CurrentClusterState getClusterState() {
            return _clusterState;
        }

        public Optional<List<ShardAllocation>> getAllocations() {
            return _allocations;
        }

        private final ClusterEvent.CurrentClusterState _clusterState;
        private final Optional<List<ShardAllocation>> _allocations;
        private static final long serialVersionUID = 603308359721162702L;
    }
}
