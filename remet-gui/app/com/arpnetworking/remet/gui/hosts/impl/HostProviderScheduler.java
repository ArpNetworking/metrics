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

package com.arpnetworking.remet.gui.hosts.impl;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Actor that runs as a singleton on the cluster.  Responsible for creating the HostProvider.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HostProviderScheduler extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param hostProviderProps A <code>Props</code> that can build the desired host provider.
     * @param initialDelay the initial delay
     * @param interval the tick interval
     */
    public HostProviderScheduler(
        final Props hostProviderProps,
        final FiniteDuration initialDelay,
        final FiniteDuration interval) {
        final ActorRef hostProvider = getContext().actorOf(hostProviderProps);

        _tickScheduler = getContext().system().scheduler().schedule(
            initialDelay,
            interval,
            hostProvider,
            "tick",
            getContext().system().dispatcher(),
            getSelf());
    }

    /**
     * Creates a <code>Props</code> for a <code>HostProviderScheduler</code>.
     *
     * @param hostProviderProps the host provider props
     * @param initialDelay the initial delay
     * @param interval the interval
     * @return the props
     */
    public static Props props(
        final Props hostProviderProps,
        final FiniteDuration initialDelay,
        final FiniteDuration interval) {
        return Props.create(HostProviderScheduler.class, hostProviderProps, initialDelay, interval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() throws Exception {
        super.preStart();
        LOGGER.info()
                .setMessage("Starting HostProviderScheduler")
                .addData("actor", self().toString())
                .log();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        unhandled(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        _tickScheduler.cancel();
        super.postStop();
    }

    private final Cancellable _tickScheduler;

    private static final Logger LOGGER = LoggerFactory.getLogger(HostProviderScheduler.class);
}
