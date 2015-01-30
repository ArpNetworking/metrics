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
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.base.MoreObjects;
import com.google.common.collect.EvictingQueue;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract publisher to send data to a server via Vertx <code>NetSocket</code>.
 *
 * This class is best described as 3 separate parts:
 * <ul>
 *     <li>The public interface</li>
 *     <li>The connect loop</li>
 *     <li>The send loop</li>
 * </ul>
 *
 * <p>
 *     The job of the public interface is to isolate the vertx event loop that sits at the
 *     heart of the sink.  The public interface, therefore provides the thread safety
 *     to the other two components.
 *
 *     Notably, the main way it interacts with the vertx event loop is by dispatching runnables
 *     to it.
 * </p>
 * <p>
 *     The connect loop runs on the vertx event loop and is tasked with maintaining the
 *     connection to the upstream server.  This is done by calling connectToServer.
 *     When an error is detected on the socket, the callback fires and again calls
 *     connectToServer.  If the connection fails, connectToServer is called in a vertx
 *     setTimer call, thus making it a loop.
 * </p>
 * <p>
 *     The send loop also runs on the vertx event loop and is tasked with sending the queued
 *     data to the connected socket.  If a connected socket does not exist, the loop will "sleep"
 *     by re-scheduling itself with the vertx setTimer call. The main function for this loop is
 *     consumeLoop.
 * </p>
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public abstract class VertxSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        dispatch(new Handler<Void>() {
                @Override
                public void handle(final Void event) {
                    LOGGER.debug(getName() + ": Appending data to pending queue; size=" + data.size());
                    appendToPending(data, conditions);
                }
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        dispatch(new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                final NetSocket socket = _socket.getAndSet(null);
                if (socket != null) {
                    socket.close();
                }
            }
        });
    }

    /**
     * Sends a <code>Buffer</code> of bytes to the socket if the client is connected.
     *
     * @param data the data to send
     */
    protected void sendRawData(final Buffer data) {
        dispatch(new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                final NetSocket socket = _socket.get();
                if (socket != null) {
                    socket.write(data);
                } else {
                    LOGGER.warn(getName() + ": Could not write data to socket, socket is not connected");
                }
            }
        });
    }

    private void dispatch(final Handler<Void> handler) {
        if (_context != null) {
            _context.runOnContext(handler);
        } else {
            _vertx.runOnContext(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
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

    /**
     * This function need only be called once, now in the constructor.
     */
    //TODO(barp): Move to a start/stop model for Sinks [MAI-257]
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
            // Don't try to connect too frequently
            final long currentTime = System.currentTimeMillis();
            if (currentTime - _lastConnectionAttempt < _currentReconnectWait) {
                LOGGER.debug(getName() + ": Not attempting connection");
                _connecting.set(false);
                return;
            }

            // Attempt to connect
            LOGGER.info(getName() + ": Connecting to server; attempt=" + _connectionAttempt
                    + " address=" + _serverAddress + " port=" + _serverPort);
            _lastConnectionAttempt = System.currentTimeMillis();
            _client.connect(
                    _serverPort,
                    _serverAddress,
                    new ConnectionHandler());
    }

    private Handler<Void> createSocketCloseHandler(final NetSocket socket) {
        return new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                if (socket != null) {
                    socket.close();
                }
                LOGGER.warn(getName() + ": Server socket closed; forcing reconnect attempt");
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
                LOGGER.warn(getName() + ": Server socket exception", event);
            }
        };
    }

    private void appendToPending(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        for (final AggregatedData datum : data) {
            // TODO(vkoskela): This should support sending conditions [MAI-452]
            // TODO(vkoskela): This should be configurable in the base [MAI-91]
            // Don't aggregate metrics < 1 minute
            if (!datum.getPeriod().toStandardDuration().isShorterThan(Duration.standardMinutes(1))) {
                _pendingData.add(datum);
            }
        }
    }

    private void consumeLoop() {
        try {
            boolean done = false;
            NetSocket socket = _socket.get();
            while (socket != null && !done) {
                if (_pendingData.size() > 0) {
                    final AggregatedData datum = _pendingData.poll();
                    flushDatum(datum, socket);
                } else {
                    done = true;
                }
                socket = _socket.get();
            }
            if (socket == null
                    && (_lastNotConnectedNotify == null
                        || _lastNotConnectedNotify.plus(Duration.standardSeconds(30)).isBeforeNow())) {
                LOGGER.debug(getName() + ": Not connected to server. Data will be flushed when reconnected. "
                                     + "Suppressing this message for 30 seconds.");
                _lastNotConnectedNotify = DateTime.now();
            }
        } finally {
            getVertx().setTimer(100, new Handler<Long>() {
                @Override
                public void handle(final Long event) {
                    consumeLoop();
                }
            });
        }
    }

    private void flushDatum(final AggregatedData datum, final NetSocket socket) {
        // Write the serialized data
        final Buffer buffer = serialize(datum);
        LOGGER.debug("writing buffer to socket");
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("buffer = " + buffer.toString("utf-8"));
        }
        socket.write(buffer);
    }

    /**
     * Protected constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    protected VertxSink(final Builder<?, ?> builder) {
        super(builder);
        _serverAddress = builder._serverAddress;
        _serverPort = builder._serverPort.intValue();
        _vertx = VertxFactory.newVertx();
        //Calling this just so the context gets created
        if (_vertx instanceof DefaultVertx) {
            final DefaultVertx vertx = (DefaultVertx) _vertx;
            final DefaultContext context = vertx.getOrCreateContext();
            vertx.setContext(context);
            _context = context;
        } else {
            _context = null;
            LOGGER.warn("vertx instance not a DefaultVertx as expected. Threading may be incorrect.");
        }

        _client = _vertx.createNetClient()
                .setReconnectAttempts(0)
                .setConnectTimeout(5000)
                .setTCPNoDelay(true)
                .setTCPKeepAlive(true);
        _socket = new AtomicReference<>();
        _pendingData = EvictingQueue.create(builder._maxQueueSize.intValue());
        _exponentialBackoffBase = builder._exponentialBackoffBase;

        connectToServer();
        consumeLoop();
    }

    protected Vertx getVertx() {
        return _vertx;
    }

    private final String _serverAddress;
    private final int _serverPort;
    private final Vertx _vertx;
    private final NetClient _client;
    private final Context _context;
    private final AtomicReference<NetSocket> _socket;
    private final EvictingQueue<AggregatedData> _pendingData;
    private final AtomicBoolean _connecting = new AtomicBoolean(false);
    private DateTime _lastNotConnectedNotify = null;
    private volatile long _lastConnectionAttempt = 0;
    private volatile int _connectionAttempt = 1;
    private final int _exponentialBackoffBase;

    private int _currentReconnectWait = 3000;
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxSink.class);

    /**
     * Implementation of base builder pattern for <code>VertxSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public abstract static class Builder<B extends BaseSink.Builder<B, S>, S extends Sink> extends BaseSink.Builder<B, S> {

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
        protected Builder(final Class<S> targetClass) {
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
        @NotNull
        private Integer _exponentialBackoffBase = Integer.valueOf(500);
    }

    private class ConnectionHandler implements AsyncResultHandler<NetSocket> {
        @Override
        public void handle(final AsyncResult<NetSocket> event) {
            if (event.succeeded()) {
                LOGGER.info(getName() + ": Connected to server; attempt=" + _connectionAttempt
                        + ", address=" + _serverAddress + " port=" + _serverPort);
                final NetSocket socket = event.result();
                socket.exceptionHandler(createSocketExceptionHandler(socket));
                socket.endHandler(createSocketCloseHandler(socket));
                _connectionAttempt = 1;

                onConnect(socket);

                _connecting.set(false);
                _socket.set(socket);
            } else if (event.failed()) {
                LOGGER.warn(getName() + ": Error connecting to server; address=" + _serverAddress + " port="
                        + _serverPort, event.cause());
                _connectionAttempt++;
                //Calculate the next reconnect delay.  Exponential backoff formula.

                _currentReconnectWait = (((int) (Math.random()  //randomize
                        * Math.pow(1.3, Math.min(_connectionAttempt, 20)))) //1.3^x where x = min(attempt, 20)
                            +  1) //make sure we don't wait 0
                        *  _exponentialBackoffBase; //the milliseconds base
                LOGGER.info(getName() + ": waiting " + _currentReconnectWait + " ms to try again");
                getVertx().setTimer(_currentReconnectWait, new Handler<Long>() {
                    @Override
                    public void handle(final Long l) {
                        connectToServer();
                    }
                });
                _connecting.set(false);
                _socket.set(null);
            }
        }
    }
}
