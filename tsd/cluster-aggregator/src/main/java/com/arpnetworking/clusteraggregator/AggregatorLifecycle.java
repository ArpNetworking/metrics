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
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * Lifecycle monitoring for {@link com.arpnetworking.clusteraggregator.aggregation.AggregationRouter} actors.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggregatorLifecycle extends UntypedActor {
    /**
     * Creates a {@link Props} for an AggregatorLifecycle.
     *
     * @return A new {@link Props}
     */
    public static Props props() {
        return Props.create(AggregatorLifecycle.class);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        //TODO(barp): handle actor shutdown/timeout messages [MAI-442]
        if (message instanceof Subscribe) {
            _subscribers.add(((Subscribe) message).getSubscriber());
        } else if (message instanceof NotifyAggregatorStarted) {
            for (final ActorRef subscriber : _subscribers) {
                subscriber.tell(message, getSelf());
            }
        } else {
            unhandled(message);
        }
    }

    private final List<ActorRef> _subscribers = Lists.newArrayList();

    /**
     * Message to subscribe to lifecycle events.
     */
    public static final class Subscribe {
        /**
         * Public constructor.
         *
         * @param subscriber subscriber for the state changes
         *
         */
        public Subscribe(final ActorRef subscriber) {
            _subscriber = subscriber;
        }

        public ActorRef getSubscriber() {
            return _subscriber;
        }

        private final ActorRef _subscriber;
    }

    /**
     * Message indicating that a new aggregator has started.
     */
    public static final class NotifyAggregatorStarted implements Serializable {
        /**
         * Public constructor.
         *
         * @param aggregatedData the aggregatedData
         */
        public NotifyAggregatorStarted(final AggregatedData aggregatedData) {
            _aggregatedData = aggregatedData;
        }

        public AggregatedData getAggregatedData() {
            return _aggregatedData;
        }

        private final AggregatedData _aggregatedData;
        private static final long serialVersionUID = 1943920959991388190L;
    }
}
