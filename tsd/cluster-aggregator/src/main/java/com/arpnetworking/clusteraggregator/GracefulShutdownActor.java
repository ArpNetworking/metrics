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
package com.arpnetworking.clusteraggregator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.sharding.ShardRegion;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Shuts down the Akka cluster gracefully.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GracefulShutdownActor extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param shardRegion aggregator shard region
     */
    @Inject
    public GracefulShutdownActor(@Named("aggregator-shard-region") final ActorRef shardRegion) {
        _shardRegion = shardRegion;
        _cluster = Cluster.get(context().system());
        _system = context().system();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof Shutdown) {
            LOGGER.info()
                    .setMessage("Initiating graceful shutdown")
                    .addData("actor", self())
                    .log();
            context().watch(_shardRegion);
            _shardRegion.tell(ShardRegion.gracefulShutdownInstance(), self());
        } else if (message instanceof Terminated) {
            _cluster.registerOnMemberRemoved(_system::terminate);
            _cluster.leave(_cluster.selfAddress());
        }
    }

    private ActorRef _shardRegion;
    private final Cluster _cluster;
    private final ActorSystem _system;
    private static final Logger LOGGER = LoggerFactory.getLogger(GracefulShutdownActor.class);
    /**
     * Message to initiate a graceful shutdown.
     */
    public static final class Shutdown {
        private Shutdown() {}
        /**
         * Gets the singleton instance of this object.
         *
         * @return a singleton instance
         */
        public static Shutdown instance() {
            return SHUTDOWN;
        }

        private static final Shutdown SHUTDOWN = new Shutdown();
    }
}
