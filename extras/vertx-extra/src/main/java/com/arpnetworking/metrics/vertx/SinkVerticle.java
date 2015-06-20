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
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.impl.TsdEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An abstract verticle responsible for writing the metrics data to the targeted sink. This verticle subscribes to the
 * vertx event bus to receive the metrics data. This approach lets the client have multiple verticles write to the same
 * sink without having to share the <code>MetricsFactory</code> instance. Alternatively, in cases where the verticles
 * can share an instance of <code>MetricsFactory</code>, they can do so by defining an instance of the
 * <code>SharedMetricsFactory</code> in the shared data space of vertx.
 *
 * Implementations of this class should define the implementation for the <code>createSinks()</code> method, that
 * returns a not null <code>List</code> of sinks. The client may choose to override the implementation of the method
 * <code>initializeHandler()</code>. Implementations of this class should be deployed as a worker verticle
 * since writing to a sink is a blocking operation. The config for this verticle should contain the "sinkAddress" key.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public abstract class SinkVerticle extends Verticle {

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        _sinks = new ArrayList<>(createSinks());
        _handler = initializeHandler();
        _sinkAddress = container.config().getString("sinkAddress", DEFAULT_SINK_ADDRESS);

        vertx.eventBus().registerHandler(_sinkAddress, _handler);
    }

    /**
     * Initializes the member sinks with a list of sinks to write to.
     *
     * @return A <code>List</code> of sinks.
     */
    protected abstract List<Sink> createSinks();

    /**
     * Initializes the member handler with an appropriate message handler. The default implementation is to initialize
     * with the <code>SinkHandler</code> instance.
     *
     * @return An instance of <code>Handler&lt;Message&lt;String&gt;&gt;</code>.
     */
    protected Handler<Message<String>> initializeHandler() {
        return new SinkHandler(_sinks);
    }

    protected String _sinkAddress;
    protected List<Sink> _sinks;
    protected Handler<Message<String>> _handler;

    private static final String DEFAULT_SINK_ADDRESS = "metrics.sink.default";

    /**
     * Event bus message handler class for <code>SinkVerticle</code>.
     */
    protected static class SinkHandler implements Handler<Message<String>> {

        /**
         * Public constructor.
         *
         * @param sinks A <code>List</code> of sinks.
         */
        public SinkHandler(final List<Sink> sinks) {
            _sinks = sinks;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void handle(final Message<String> message) {
            if (message == null || message.body() == null) {
                LOGGER.warn("Null message received.");
                return;
            }
            try {
                final TsdEvent.Builder eventBuilder = OBJECT_MAPPER.readValue(message.body(), TsdEvent.Builder.class);
                final Event event = eventBuilder.build();
                for (final Sink sink: _sinks) {
                    sink.record(
                        event.getAnnotations(),
                        event.getTimerSamples(),
                        event.getCounterSamples(),
                        event.getGaugeSamples());
                }
            } catch (final IOException | IllegalArgumentException e) {
                LOGGER.warn("Message is not in expected format.", e.getMessage());
            }
        }

        protected final List<Sink> _sinks;

        private static final Logger LOGGER = LoggerFactory.getLogger(SinkHandler.class);
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        static {
            final SimpleModule module = new SimpleModule();
            module.addAbstractTypeMapping(Quantity.class, DefaultQuantity.class);
            OBJECT_MAPPER.registerModule(module);
        }
    }

    /**
     * Default implementation of <code>Quantity</code> for deserialization purposes.
     */
    public static class DefaultQuantity implements Quantity {

        /**
         * Default constructor.
         */
        public DefaultQuantity() {}

        /**
         * Static factory method.
         *
         * @param value An instance of <code>Number</code>.
         * @param unit An instance of <code>Unit</code>.
         * @return An instance of <code>Quantity</code>.
         */
        public static Quantity newInstance(final Number value, final Unit unit) {
            return new DefaultQuantity(value, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Number getValue() {
            return _value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Unit getUnit() {
            return _unit;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof DefaultQuantity)) {
                return false;
            }

            final DefaultQuantity otherQuantity = (DefaultQuantity) other;
            return Objects.equals(getUnit(), otherQuantity.getUnit())
                   && Objects.equals(getValue(), otherQuantity.getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + _value.hashCode();
            hash = hash * 31 + _unit.hashCode();
            return hash;
        }

        private DefaultQuantity(final Number value, final Unit unit) {
            _value = value;
            _unit = unit;
        }

        @JsonProperty("value")
        private Number _value;
        @JsonProperty("unit")
        private Unit _unit;
    }
}
