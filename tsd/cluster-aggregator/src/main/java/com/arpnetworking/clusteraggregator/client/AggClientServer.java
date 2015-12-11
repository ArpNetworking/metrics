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
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.io.Tcp;
import akka.io.TcpMessage;
import com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import java.net.InetSocketAddress;

/**
 * TCP Server that listens for aggregation client connections.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggClientServer extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param supervisorProvider Provider to build aggregation client supervisors.
     * @param clusterConfiguration Configuration for the cluster aggregator.
     */
    @Inject
    public AggClientServer(
            @Named("agg-client-supervisor") final Provider<Props> supervisorProvider,
            final ClusterAggregatorConfiguration clusterConfiguration) {
        _supervisorProvider = supervisorProvider;
        _clusterConfiguration = clusterConfiguration;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() throws Exception {
        final ActorRef tcp = Tcp.get(getContext().system()).manager();
        LOGGER.debug()
                .setMessage("Binding to socket")
                .addData("address", _clusterConfiguration.getAggregationHost())
                .addData("port", _clusterConfiguration.getAggregationPort())
                .addContext("actor", self())
                .log();
        tcp.tell(
                TcpMessage.bind(
                        getSelf(),
                        new InetSocketAddress(_clusterConfiguration.getAggregationHost(), _clusterConfiguration.getAggregationPort()),
                        15),
                getSelf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof Tcp.Bound) {
            final Tcp.Bound bound = (Tcp.Bound) message;
            LOGGER.debug()
                    .setMessage("Successfully bound")
                    .addData("address", bound.localAddress())
                    .addContext("actor", self())
                    .log();
        } else if (message instanceof Tcp.Connected) {
            final Tcp.Connected conn = (Tcp.Connected) message;
            final ActorRef handler = getContext().actorOf(
                    _supervisorProvider.get(),
                    String.format("sup-%s:%d",
                                  conn.remoteAddress().getAddress().toString().replace("/", ""),
                                  conn.remoteAddress().getPort()));
            handler.tell(message, getSender());
        }

    }

    private final Provider<Props> _supervisorProvider;
    private final ClusterAggregatorConfiguration _clusterConfiguration;
    private static final Logger LOGGER = LoggerFactory.getLogger(AggClientServer.class);
}

