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

package com.arpnetworking.clusteraggregator.client;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.contrib.pattern.ClusterSharding;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.Function;
import com.arpnetworking.clusteraggregator.AkkaCluster;
import com.arpnetworking.tsdcore.model.AggregatedData;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Supervises the connection's actors.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggClientSupervisor extends UntypedActor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AggregatedData) {
            // Route the message to the sharding region
            _shardRegion.tell(message, getSender());
        } else if (message instanceof Tcp.Connected) {
            final Tcp.Connected conn = (Tcp.Connected) message;
            final ActorRef handler = getContext().actorOf(AggClientConnection.props(getSender(), conn.remoteAddress()),
                                                          "dataHandler");
            getSender().tell(TcpMessage.register(handler, true, true), getSelf());
            getContext().watch(getSender());
        } else if (message instanceof Terminated) {
            _log.debug("detected terminated connection, releasing all resources");
            getContext().stop(getSelf());
        } else {
            _log.warning("UNHANDLED MESSAGE!!: " + message);
            unhandled(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new AllForOneStrategy(3, Duration.create(5, TimeUnit.MINUTES), new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
            public SupervisorStrategy.Directive apply(final Throwable t) throws Exception {
                _log.debug("supervisor caught exception: ", t);
                //if any of the children throw an exception, stop this actor to clean up all the resources
                //the client will need to reconnect
                return SupervisorStrategy.stop();
            }
        });
    }

    private final ActorRef _shardRegion = ClusterSharding.get(getContext().system()).shardRegion(AkkaCluster.AGG_SHARD_NAME);
    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);
}
