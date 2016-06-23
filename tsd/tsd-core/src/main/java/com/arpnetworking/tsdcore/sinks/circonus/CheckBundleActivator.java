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
package com.arpnetworking.tsdcore.sinks.circonus;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import com.arpnetworking.akka.UniformRandomTimeScheduler;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundle;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import play.libs.F;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Actor responsible for keeping check bundle metrics active.
 * A parent actor is responsible for telling the refresher about check bundles.
 * Once notified of the existence of a check bundle, the refresher will use the
 * Circonus API to continually lookup the check bundle and set any metrics to
 * the active state.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class CheckBundleActivator extends UntypedActor {
    /**
     * Creates a {@link Props} in a type safe way.
     *
     * @param client The Circonus client used to access the API.
     * @return A new {@link Props}.
     */
    public static Props props(final CirconusClient client) {
        return Props.create(CheckBundleActivator.class, client);
    }

    /**
     * Public constructor.
     *
     * @param client The Circonus client used to access the API.
     */
    public CheckBundleActivator(final CirconusClient client) {
        _client = client;
        _dispatcher = context().dispatcher();
        _refresher = new UniformRandomTimeScheduler.Builder()
                .setExecutionContext(context().dispatcher())
                .setMinimumTime(FiniteDuration.apply(10, TimeUnit.MINUTES))
                .setMaximumTime(FiniteDuration.apply(20, TimeUnit.MINUTES))
                .setMessage(new RefreshBundles())
                .setScheduler(context().system().scheduler())
                .setSender(self())
                .setTarget(self())
                .build();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        super.postStop();
        _refresher.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof NotifyCheckBundle) {
            final NotifyCheckBundle notification = (NotifyCheckBundle) message;
            _checkBundleCids.add(notification.getCheckBundle().getCid());
        } else if (message instanceof RefreshBundles) {
            startCheckBundleRefresh();
        } else if (message instanceof CheckBundleRefreshComplete) {
            final CheckBundleRefreshComplete bundle = (CheckBundleRefreshComplete) message;
            LOGGER.debug()
                    .setMessage("Check bundle updated")
                    .addData("cid", bundle.getCheckBundle().getCid())
                    .addData("bundle", bundle.getCheckBundle())
                    .addContext("actor", self())
                    .log();
            context().parent().tell(message, self());
            refreshNextBundle();
        } else if (message instanceof CheckBundleRefreshFailure) {
            final CheckBundleRefreshFailure failure = (CheckBundleRefreshFailure) message;
            LOGGER.error()
                    .setMessage("Failed to update check bundle")
                    .setThrowable(failure.getCause())
                    .addContext("actor", self())
                    .log();
            refreshNextBundle();
        } else if (message instanceof CheckBundleDisabled) {
            final CheckBundleDisabled disabled = (CheckBundleDisabled) message;
            LOGGER.debug()
                    .setMessage("Found disabled check bundle. Removing from the update list")
                    .addData("cid", disabled.getCid())
                    .addContext("actor", self())
                    .log();
            _checkBundleCids.remove(disabled.getCid());
        } else {
            unhandled(message);
        }
    }

    private void startCheckBundleRefresh() {
        if (_pendingCheckBundleRefresh.isEmpty()) {
            _pendingCheckBundleRefresh.addAll(_checkBundleCids);
        } else {
            LOGGER.warn()
                    .setMessage("Refresh not yet completed, skipping this refresh round")
                    .addData("pendingQueueSize", _pendingCheckBundleRefresh.size())
                    .addContext("actor", self())
                    .log();
        }

        refreshNextBundle();
    }

    private void refreshNextBundle() {
        if (!_pendingCheckBundleRefresh.isEmpty()) {
            final String cid = _pendingCheckBundleRefresh.poll();
            refreshCheckBundle(cid);
        }
    }

    private void refreshCheckBundle(final String cid) {
        final F.Promise<Object> requestPromise = _client.getCheckBundle(cid)
                .flatMap(
                        response -> {
                            final List<Map<String, String>> metrics = response.getMetrics();
                            boolean needsUpdate = false;
                            for (final Map<String, String> metric : metrics) {
                                if ("available".equalsIgnoreCase(metric.get("status"))) {
                                    metric.put("status", "active");
                                    needsUpdate = true;
                                }
                                if (metric.get("name").endsWith("/" + HISTOGRAM_STATISTIC.getName())
                                        && !"histogram".equalsIgnoreCase(metric.get("type"))) {
                                    metric.put("type", "histogram");
                                    needsUpdate = true;
                                }
                            }
                            if ("disabled".equals(response.getStatus())) {
                                return F.Promise.<Object>pure(new CheckBundleDisabled(cid));
                            } else if (needsUpdate) {
                                return _client.updateCheckBundle(response).map(CheckBundleRefreshComplete::new, _dispatcher);
                            } else {
                                return F.Promise.<Object>pure(new CheckBundleRefreshComplete(response));
                            }
                        },
                        _dispatcher)
                .recover(CheckBundleRefreshFailure::new);
        Patterns.pipe(requestPromise.wrapped(), _dispatcher).to(self());
    }

    private final CirconusClient _client;
    private final Queue<String> _pendingCheckBundleRefresh = Queues.newArrayDeque();
    private final Set<String> _checkBundleCids = Sets.newHashSet();
    private final ExecutionContextExecutor _dispatcher;
    private final UniformRandomTimeScheduler _refresher;
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckBundleActivator.class);
    private static final Statistic HISTOGRAM_STATISTIC = new StatisticFactory().getStatistic("histogram");

    private static final class RefreshBundles { }

    private static final class CheckBundleRefreshFailure {
        private CheckBundleRefreshFailure(final Throwable cause) {
            _cause = cause;
        }

        public Throwable getCause() {
            return _cause;
        }

        private final Throwable _cause;
    }

    private static final class CheckBundleDisabled {
        private CheckBundleDisabled(final String cid) {
            _cid = cid;
        }

        public String getCid() {
            return _cid;
        }

        private final String _cid;
    }

    /**
     * Message class used to notify self and the CirconusSinkActor about an updated checkbundle.
     */
    /* package private */ static final class CheckBundleRefreshComplete {
        private CheckBundleRefreshComplete(final CheckBundle checkBundle) {
            _checkBundle = checkBundle;
        }

        public CheckBundle getCheckBundle() {
            return _checkBundle;
        }

        private final CheckBundle _checkBundle;
    }

    /**
     * Message class used to notify the refresher about a check bundle.
     */
    public static final class NotifyCheckBundle {
        /**
         * Public constructor.
         *
         * @param checkBundle The check bundle.
         */
        public NotifyCheckBundle(final CheckBundle checkBundle) {
            _checkBundle = checkBundle;
        }

        public CheckBundle getCheckBundle() {
            return _checkBundle;
        }

        private final CheckBundle _checkBundle;
    }


}
