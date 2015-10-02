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
import com.arpnetworking.clusteraggregator.configuration.EmitterConfiguration;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.sinks.MultiSink;
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
     * @param config Config describing the sinks to write to
     * @return A new <code>Props</code>.
     */
    public static Props props(final EmitterConfiguration config) {
        return Props.create(Emitter.class, config);
    }

    /**
     * Public constructor.
     *
     * @param config Config describing the sinks to write to
     */
    public Emitter(final EmitterConfiguration config) {
        _sink = new MultiSink.Builder()
                .setName("EmitterMultiSink")
                .setSinks(config.getSinks())
                .build();
        LOGGER.info()
                .setMessage("Emitter starting up")
                .addData("sink", _sink)
                .log();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AggregatedData) {
            final AggregatedData data = (AggregatedData) message;
            LOGGER.debug()
                    .setMessage("Emitting data to sink")
                    .addData("data", message)
                    .log();
            _sink.recordAggregateData(Collections.singletonList(data));
        } else {
            unhandled(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        super.postStop();
        _sink.close();
    }

    private final Sink _sink;
    private static final Logger LOGGER = LoggerFactory.getLogger(Emitter.class);
}
