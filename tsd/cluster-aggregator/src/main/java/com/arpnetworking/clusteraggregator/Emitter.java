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

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.sinks.Sink;

import java.util.Collections;

/**
 * Holds the sinks and emits to them.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Emitter extends UntypedActor {
    /**
     * Creates a <code>Props</code> for construction in Akka.
     *
     * @param sink Sink to write the aggregated data output to.
     * @return A new <code>Props</code>.
     */
    public static Props props(final Sink sink) {
        return Props.create(Emitter.class, sink);
    }

    /**
     * Public constructor.
     *
     * @param sink Sink to write the aggregated data output to.
     */
    public Emitter(final Sink sink) {
        _sink = sink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AggregatedData) {
            final AggregatedData data = (AggregatedData) message;
            _log.debug("Emitting data to sink: " + message);
            _sink.recordAggregateData(Collections.singletonList(data));
        } else {
            unhandled(message);
        }
    }

    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);
    private final Sink _sink;
}
