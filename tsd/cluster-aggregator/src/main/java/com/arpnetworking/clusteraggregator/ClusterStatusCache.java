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

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.base.Optional;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
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
                if (_clusterState.isPresent()) {
                    getSender().tell(_clusterState.get(), getSelf());
                } else {
                    _log.warning("Cache miss; cluster state not available");
                    _cluster.sendCurrentClusterState(getSender());
                }
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

    private final Cluster _cluster;
    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);

    private Optional<ClusterEvent.CurrentClusterState> _clusterState = Optional.absent();
    @Nullable private Cancellable _pollTimer;

    private static final String POLL = "poll";

    /**
     * Request to get a cluster status.
     */
    public static final class GetRequest { }
}
