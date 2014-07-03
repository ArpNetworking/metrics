/**
 * Copyright 2014 Brandon Arp
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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.base.Objects;
import com.google.common.collect.EvictingQueue;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract publisher to send data to a server via Vertx <code>NetSocket</code>.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public abstract class VertxSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final List<AggregatedData> data) {
        LOGGER.debug(getName() + ": Writing aggregated data; size=" + data.size());

        connectToServer();
        flushPending();
        for (final AggregatedData datum : data) {
            flushDatum(datum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        final NetSocket socket = _socket.getAndSet(null);
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("ServerAddress", _serverAddress)
                .add("ServerPort", _serverPort)
                .toString();
    }

    /**
     * Perform tasks when the connection is first established. This method is
     * invoked while holding a lock on the socket.
     *
     * @param socket The <code>NetSocket</code> instance that was connected.
     */
    protected abstract void onConnect(final NetSocket socket);

    /**
     * Serialize the <code>AggregatedData</code> instance for transmission to
     * the server.
     *
     * @param datum The <code>AggregateData</code> instance to serialized.
     * @return The <code>Buffer</code> containing the serialized representation.
     */
    protected abstract Buffer serialize(final AggregatedData datum);

    private void connectToServer() {
        // Check if already connected
        if (_socket.get() != null) {
            return;
        }

        // Block if already connecting
        final boolean isConnecting = _connecting.getAndSet(true);
        if (isConnecting) {
            LOGGER.debug(getName() + ": Already connecting, not attempting another connection at this time");
            return;
        }
        synchronized (_connectLock) {
            // Don't try to connect too frequently
            final long currentTime = System.currentTimeMillis();
            if (currentTime - _lastConnectionAttempt < RECONNECT_INTERVAL_IN_MILLISECONDS) {
                LOGGER.debug(getName() + ": Not attempting connection");
                _connecting.set(false);
                return;
            }

            // Attempt to connect
            LOGGER.info(getName() + ": Connecting to server; address=" + _serverAddress + " port=" + _serverPort);
            _lastConnectionAttempt = System.currentTimeMillis();
            _client.connect(
                    _serverPort,
                    _serverAddress,
                    new AsyncResultHandler<NetSocket>() {
                        @Override
                        public void handle(final AsyncResult<NetSocket> event) {
                            if (event.succeeded()) {
                                LOGGER.info(getName() + ": Connected to server; address=" + _serverAddress + " port=" + _serverPort);
                                final NetSocket socket = event.result();
                                socket.exceptionHandler(createSocketExceptionHandler(socket));
                                socket.closeHandler(createSocketCloseHandler(socket));

                                onConnect(socket);

                                _connecting.set(false);
                                _socket.set(socket);
                            } else if (event.failed()) {
                                LOGGER.warn(getName() + ": Error connecting to server; address=" + _serverAddress + " port="
                                        + _serverPort, event.cause());
                                _connecting.set(false);
                                _socket.set(null);
                            }
                        }
                    });
        }
    }

    private Handler<Void> createSocketCloseHandler(final NetSocket socket) {
        return new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                if (socket != null) {
                    socket.close();
                }
                LOGGER.error(getName() + ": Server socket closed; forcing reconnect attempt", event);
                _socket.set(null);
                _lastConnectionAttempt = 0;
                connectToServer();
            }
        };
    }

    private Handler<Throwable> createSocketExceptionHandler(final NetSocket socket) {
        return new Handler<Throwable>() {
            @Override
            public void handle(final Throwable event) {
                if (socket != null) {
                    socket.close();
                }
                LOGGER.error(getName() + ": Server socket exception; forcing reconnect attempt", event);
                _socket.set(null);
                _lastConnectionAttempt = 0;
                connectToServer();
            }
        };
    }

    private void flushPending() {
        AggregatedData datum;
        synchronized (_pendingData) {
            while ((datum = _pendingData.poll()) != null) {
                flushDatum(datum);
            }
        }
    }

    private void flushDatum(final AggregatedData datum) {
        // Don't aggregate metrics < 1 minute
        // TODO(vkoskela): This should be configurable in the base [MAI-91]
        if (datum.getPeriod().toStandardDuration().isShorterThan(org.joda.time.Duration.standardMinutes(1))) {
            return;
        }

        final NetSocket socket = _socket.get();
        if (socket == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getName() + ": Not connected to server, buffering data");
            }
            synchronized (_pendingData) {
                if (_pendingData.remainingCapacity() == 0) {
                    LOGGER.warn(getName() + ": Buffer full; discarding data from buffer");
                }
                _pendingData.add(datum);
            }
            return;
        }

        // Write the serialized data
        final Buffer buffer = serialize(datum);
        synchronized (socket) {
            socket.write(buffer);
        }
    }

    /**
     * Protected constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    protected VertxSink(final Builder<?> builder) {
        super(builder);
        _serverAddress = builder._serverAddress;
        _serverPort = builder._serverPort.intValue();
        _vertx = VertxFactory.newVertx();
        _client = _vertx.createNetClient()
                .setReconnectAttempts(1)
                .setReconnectInterval(3000L)
                .setConnectTimeout(5000)
                .setTCPNoDelay(true)
                .setTCPKeepAlive(true);
        _socket = new AtomicReference<NetSocket>();
        _pendingData = EvictingQueue.create(builder._maxQueueSize.intValue());
    }

    private final String _serverAddress;
    private final int _serverPort;
    private final Vertx _vertx;
    private final NetClient _client;
    private final AtomicReference<NetSocket> _socket;
    private final EvictingQueue<AggregatedData> _pendingData;
    private final AtomicBoolean _connecting = new AtomicBoolean(false);
    private final Object _connectLock = new Object();
    private volatile long _lastConnectionAttempt = 0;

    private static final long RECONNECT_INTERVAL_IN_MILLISECONDS = 3000;
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxSink.class);

    /**
     * Implementation of base builder pattern for <code>VertxSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public abstract static class Builder<B extends BaseSink.Builder<B>> extends BaseSink.Builder<B> {

        /**
         * The server host name. Cannot be null or empty.
         *
         * @param value The aggregation server host name.
         * @return This instance of <code>Builder</code>.
         */
        public B setServerAddress(final String value) {
            _serverAddress = value;
            return self();
        }

        /**
         * The server port. Cannot be null; must be between 1 and 65535.
         *
         * @param value The server port.
         * @return This instance of <code>Builder</code>.
         */
        public B setServerPort(final Integer value) {
            _serverPort = value;
            return self();
        }

        /**
         * The maximum queue size. Cannot be null. Default is 512.
         *
         * @param value The maximum queue size.
         * @return This instance of <code>Builder</code>.
         */
        public B setMaxQueueSize(final Integer value) {
            _maxQueueSize = value;
            return self();
        }

        /**
         * Protected constructor for subclasses.
         *
         * @param targetClass The concrete type to be created by the builder of
         * <code>AggregatedDataSink</code> implementation.
         */
        protected Builder(final Class<? extends Sink> targetClass) {
            super(targetClass);
        }

        @NotNull
        @NotEmpty
        private String _serverAddress;
        @NotNull
        @Range(min = 1, max = 65535)
        private Integer _serverPort;
        @NotNull
        @Min(value = 0)
        private Integer _maxQueueSize = Integer.valueOf(512);
    }
}
