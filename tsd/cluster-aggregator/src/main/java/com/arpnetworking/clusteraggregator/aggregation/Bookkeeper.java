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

package com.arpnetworking.clusteraggregator.aggregation;

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import com.arpnetworking.clusteraggregator.AggregatorLifecycle;
import com.arpnetworking.clusteraggregator.bookkeeper.persistence.BookkeeperPersistence;
import com.arpnetworking.clusteraggregator.models.BookkeeperData;
import com.arpnetworking.clusteraggregator.models.MetricsRequest;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Responsible for recording metrics about metrics to a persistent store.
 *
 * Accepts the following messages:
 *     MetricsRequest: Replies with the cached bookkeeper data
 *     AggregatedData: Stores the relevant information from the data into the persistence object
 *
 * Internal-only messages:
 *     "UPDATE": Triggers an update of the cached data from the persistence object
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Bookkeeper extends UntypedActor {
    /**
     * Creates a <code>Props</code> for building a Bookkeeper actor in Akka.
     *
     * @param persistence The persistence provider.
     * @return A new <code>Props</code>.
     */
    public static Props props(final BookkeeperPersistence persistence) {
        return Props.create(Bookkeeper.class, persistence);
    }

    /**
     * Public constructor.
     *
     * @param persistence The persistence provider.
     */
    public Bookkeeper(final BookkeeperPersistence persistence) {
        LOGGER.info()
                .setMessage("Bookkeeper starting up")
                .addContext("actor", self())
                .log();
        _persistence = persistence;
        _updateTimer = getContext().system().scheduler().schedule(
                Duration.Zero(),
                FiniteDuration.apply(10, TimeUnit.MINUTES),
                getSelf(),
                new Update(),
                getContext().dispatcher(),
                getSelf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        if (_updateTimer != null) {
            _updateTimer.cancel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof MetricsRequest) {
            getSender().tell(_data, getSelf());
        } else if (message instanceof Update && getSelf().equals(getSender())) {
            _persistence.getBookkeeperData().onComplete(new OnComplete<BookkeeperData>() {
                @Override
                public void onComplete(final Throwable failure, final BookkeeperData success) {
                    if (failure != null) {
                        LOGGER.error()
                                .setMessage("Error getting bookkeeper data")
                                .setThrowable(failure)
                                .addContext("actor", self())
                                .log();
                    } else {
                        _data = success;
                    }
                }
            }, getContext().dispatcher());
        } else if (message instanceof AggregatorLifecycle.NotifyAggregatorStarted) {
            final AggregatorLifecycle.NotifyAggregatorStarted started = (AggregatorLifecycle.NotifyAggregatorStarted) message;
            _persistence.insertMetric(started.getAggregatedData());
        } else {
            unhandled(message);
        }
    }

    private BookkeeperData _data = null;
    private final Cancellable _updateTimer;
    private final BookkeeperPersistence _persistence;
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationRouter.class);

    private static final class Update {}
}
