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
package com.arpnetworking.metrics.vertx;

import com.arpnetworking.metrics.Event;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.impl.TsdEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;

import java.util.List;
import java.util.Map;

/**
 * This defines a sink that writes to the Vertx event bus.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class EventBusSink implements Sink {
    /**
     * {@inheritDoc}
     */
    @Override
    public void record(
            final Map<String, String> annotations,
            final Map<String, List<Quantity>> timerSamples,
            final Map<String, List<Quantity>> counterSamples,
            final Map<String, List<Quantity>> gaugeSamples) {
        try {
            final Event event = new TsdEvent.Builder()
                    .setAnnotations(annotations)
                    .setCounterSamples(counterSamples)
                    .setTimerSamples(timerSamples)
                    .setGaugeSamples(gaugeSamples)
                    .build();
            LOGGER.debug(String.format("Sending event to sink. Address=%s", _sinkAddress));
            _eventBus.publish(_sinkAddress, OBJECT_MAPPER.writeValueAsString(event));
        } catch (final JsonProcessingException e) {
            LOGGER.warn(
                    String.format(
                            "Failed to send event to sink. Address=%s.",
                            _sinkAddress),
                    e.getMessage());
        }
    }

    private EventBusSink(final Builder builder) {
        _eventBus = builder._eventBus;
        _sinkAddress = builder._sinkAddress;
    }

    private final EventBus _eventBus;
    private final String _sinkAddress;

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusSink.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Builder class for <code>EventBusSink</code>.
     */
    public static final class Builder {
        /**
         * Builds an instance of <code>EventBusSink</code>.
         *
         * @return An instance of <code>EventBusSink</code>.
         */
        public EventBusSink build() {
            if (_eventBus == null) {
                throw new IllegalArgumentException("EventBus cannot be null.");
            }
            if (_sinkAddress == null || _sinkAddress.isEmpty()) {
                throw new IllegalArgumentException("SinkAddress cannot be null or empty.");
            }
            return new EventBusSink(this);
        }

        /**
         * Sets the event bus attribute.
         *
         * @param value An instance of <code>EventBus</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setEventBus(final EventBus value) {
            _eventBus = value;
            return this;
        }

        /**
         * Sets the sink address attribute.
         *
         * @param value An instance of <code>String</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSinkAddress(final String value) {
            _sinkAddress = value;
            return this;
        }

        private EventBus _eventBus;
        private String _sinkAddress = DEFAULT_SINK_ADDRESS;

        private static final String DEFAULT_SINK_ADDRESS = "metrics.sink.default";
    }
}
