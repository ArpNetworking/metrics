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
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.MemberStatus;
import akka.dispatch.OnComplete;
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import akka.remote.AssociationErrorEvent;
import akka.util.Timeout;
import com.arpnetworking.clusteraggregator.models.BookkeeperData;
import com.arpnetworking.clusteraggregator.models.MetricsRequest;
import com.arpnetworking.clusteraggregator.models.PeriodMetrics;
import com.arpnetworking.clusteraggregator.models.StatusResponse;
import com.arpnetworking.utility.CastMapper;
import com.arpnetworking.utility.CollectFutureBuilder;
import org.joda.time.Period;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;
import scala.util.Failure;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Periodically polls the cluster status and caches the result.
 *
 * Accepts the following messages:
 *     STATUS: Replies with a StatusResponse message containing the service status data
 *     HEALTH: Replies with a boolean value
 *     ClusterEvent.CurrentClusterState: Updates the cached state of the cluster
 *
 * Internal-only messages:
 *     POLL: Triggers an update of the cluster data.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Status extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param metricsBookkeeper Where to get the status metrics from.
     * @param cluster The instance of the Clustering extension.
     * @param clusterStatusCache The actor holding the cached cluster status.
     * @param localMetrics The actor holding the local node metrics.
     */
    public Status(
            final ActorRef metricsBookkeeper,
            final Cluster cluster,
            final ActorRef clusterStatusCache,
            final ActorRef localMetrics) {

        _metricsBookkeeper = metricsBookkeeper;
        _cluster = cluster;
        _clusterStatusCache = clusterStatusCache;
        _localMetrics = localMetrics;
        context().system().eventStream().subscribe(self(), AssociationErrorEvent.class);
    }

    /**
     * Creates a <code>Props</code> for use in Akka.
     *
     * @param bookkeeper Where to get the status metrics from.
     * @param cluster The instance of the Clustering extension.
     * @param clusterStatusCache The actor holding the cached cluster status.
     * @param localMetrics The actor holding the local node metrics.
     * @return A new <code>Props</code>.
     */
    public static Props props(
            final ActorRef bookkeeper,
            final Cluster cluster,
            final ActorRef clusterStatusCache,
            final ActorRef localMetrics) {

        return Props.create(Status.class, bookkeeper, cluster, clusterStatusCache, localMetrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        final ActorRef sender = getSender();
        if (message instanceof StatusRequest) {
            processStatusRequest(sender);
        } else if (message instanceof AssociationErrorEvent) {
            final AssociationErrorEvent error = (AssociationErrorEvent) message;
            if (error.cause().getMessage().contains("quarantined this system")) {
                _quarantined = true;
            }
        } else if (message instanceof HealthRequest) {
            final ExecutionContextExecutor executor = getContext().dispatcher();
            final Future<ClusterStatusCache.StatusResponse> stateFuture = Patterns
                    .ask(
                            _clusterStatusCache,
                            new ClusterStatusCache.GetRequest(),
                            Timeout.apply(3, TimeUnit.SECONDS))
                    .map(CAST_MAPPER, executor);
            stateFuture.onComplete(
                    new OnComplete<ClusterStatusCache.StatusResponse>() {
                        @Override
                        public void onComplete(final Throwable failure, final ClusterStatusCache.StatusResponse success) {
                            final boolean healthy = _cluster.readView().self().status() == MemberStatus.up() && !_quarantined;
                            sender.tell(healthy, getSelf());
                        }
                    },
                    executor);
        } else {
            unhandled(message);
        }
    }

    private void processStatusRequest(final ActorRef sender) {
        final ExecutionContextExecutor executor = getContext().dispatcher();
        // Call the bookkeeper
        final Future<BookkeeperData> bookkeeperFuture = Patterns.ask(
                _metricsBookkeeper,
                new MetricsRequest(),
                Timeout.apply(3, TimeUnit.SECONDS))
                .map(new CastMapper<>(), executor)
                .recover(new AsNullRecovery<>(), executor);
        final Future<ClusterStatusCache.StatusResponse> clusterStateFuture =
                Patterns.ask(
                        _clusterStatusCache,
                        new ClusterStatusCache.GetRequest(),
                        Timeout.apply(3, TimeUnit.SECONDS))
                .map(CAST_MAPPER, executor)
                .recover(new AsNullRecovery<>(), executor);

        final Future<Map<Period, PeriodMetrics>> localMetricsFuture =
                Patterns.ask(
                        _localMetrics,
                        new MetricsRequest(),
                        Timeout.apply(3, TimeUnit.SECONDS))
                .map(new CastMapper<>(), executor)
                .recover(new AsNullRecovery<>(), executor);

        final Future<StatusResponse> future = new CollectFutureBuilder<StatusResponse>()
                .addFuture(bookkeeperFuture)
                .addFuture(clusterStateFuture)
                .addFuture(localMetricsFuture)
                .map(new AbstractFunction0<StatusResponse>() {
                    @Override
                    public StatusResponse apply() {
                        return new StatusResponse.Builder()
                                .setClusterMetrics(bookkeeperFuture.value().get().get())
                                .setClusterState(clusterStateFuture.value().get().get())
                                .setLocalMetrics(localMetricsFuture.value().get().get())
                                .setLocalAddress(_cluster.selfAddress())
                                .build();
                    }
                })
                .build(executor);
        future.onComplete(
                new OnComplete<StatusResponse>() {
                    @Override
                    public void onComplete(final Throwable failure, final StatusResponse success) {
                        if (failure != null) {
                            sender.tell(new Failure<StatusResponse>(failure), getSelf());
                        } else {
                            sender.tell(success, getSelf());
                        }
                    }
                },
                executor);
    }

    private boolean _quarantined = false;

    private final ActorRef _metricsBookkeeper;
    private final Cluster _cluster;
    private final ActorRef _clusterStatusCache;
    private final ActorRef _localMetrics;

    private static final CastMapper<Object, ClusterStatusCache.StatusResponse> CAST_MAPPER = new CastMapper<>();

    private static class AsNullRecovery<T> extends Recover<T> {
        @Override
        public T recover(final Throwable failure) {
            return null;
        }
    }

    /**
     * Represents a health check request.
     */
    public static final class HealthRequest {}

    /**
     * Represents a status request.
     */
    public static final class StatusRequest {}
}
