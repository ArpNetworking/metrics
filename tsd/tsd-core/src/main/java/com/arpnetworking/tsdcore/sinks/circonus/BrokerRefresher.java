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
import com.arpnetworking.tsdcore.sinks.circonus.api.BrokerListResponse;
import play.libs.F;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Actor whose responsibility it is to refresh the list of available circonus brokers.
 * This actor will schedule it's own lookups and send messages about the brokers to its parent.
 * NOTE: Lookup failures will be logged, but not propagated to the parent.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class BrokerRefresher extends UntypedActor {
    /**
     * Creates a {@link Props} in a type safe way.
     *
     * @param client The Circonus client used to access the API.
     * @return A new {@link Props}.
     */
    public static Props props(final CirconusClient client) {
        return Props.create(BrokerRefresher.class, client);
    }

    /**
     * Public constructor.
     *
     * @param client The Circonus client used to access the API.
     */
    public BrokerRefresher(final CirconusClient client) {
        _client = client;
        _brokerLookup = new UniformRandomTimeScheduler.Builder()
                .setExecutionContext(context().dispatcher())
                .setMinimumTime(FiniteDuration.apply(45, TimeUnit.MINUTES))
                .setMaximumTime(FiniteDuration.apply(75, TimeUnit.MINUTES))
                .setMessage(new LookupBrokers())
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
        _brokerLookup.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof LookupBrokers) {
            LOGGER.debug()
                    .setMessage("Starting broker lookup")
                    .addContext("actor", self())
                    .log();
            lookupBrokers();
        } else if (message instanceof BrokerLookupComplete) {
            final BrokerLookupComplete complete = (BrokerLookupComplete) message;
            LOGGER.debug()
                    .setMessage("Broker lookup complete")
                    .addData("brokers", complete.getResponse().getBrokers())
                    .addContext("actor", self())
                    .log();

            context().parent().tell(message, self());
            _brokerLookup.resume();
        } else if (message instanceof BrokerLookupFailure) {
            final BrokerLookupFailure failure = (BrokerLookupFailure) message;

            LOGGER.error()
                    .setMessage("Failed to lookup broker, trying again in 60 seconds")
                    .setThrowable(failure.getCause())
                    .addContext("actor", self())
                    .log();

            context().system().scheduler().scheduleOnce(
                    FiniteDuration.apply(60, TimeUnit.SECONDS),
                    self(),
                    new LookupBrokers(),
                    context().dispatcher(),
                    self());
            _brokerLookup.pause();
        } else {
            unhandled(message);
        }
    }

    private void lookupBrokers() {
        final F.Promise<Object> promise = _client.getBrokers()
                .<Object>map(BrokerLookupComplete::new)
                .recover(BrokerLookupFailure::new);
        Patterns.pipe(promise.wrapped(), context().dispatcher()).to(self());
    }

    private final CirconusClient _client;
    private final UniformRandomTimeScheduler _brokerLookup;
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerRefresher.class);

    /**
     * Message class used to indicate that a broker lookup has completed.
     */
    public static final class BrokerLookupComplete {
        /**
         * Public constructor.
         *
         * @param response The response.
         */
        public BrokerLookupComplete(final BrokerListResponse response) {
            _response = response;
        }

        public BrokerListResponse getResponse() {
            return _response;
        }

        private final BrokerListResponse _response;
    }

    /**
     * Message class used to indicate that a broker lookup has failed.
     */
    public static final class BrokerLookupFailure {
        /**
         * Public constructor.
         *
         * @param cause The cause.
         */
        public BrokerLookupFailure(final Throwable cause) {
            _cause = cause;
        }

        public Throwable getCause() {
            return _cause;
        }

        private final Throwable _cause;
    }

    private static class LookupBrokers { }
}
