package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.*;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Publisher to send data to a carbon server.
 *
 * @author barp
 */
public class CarbonPublisher extends Verticle implements AggregationPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonPublisher.class);
    private final AtomicBoolean _connected = new AtomicBoolean(false);
    private final AtomicBoolean _connectionInProgress = new AtomicBoolean(false);
    private final String _carbonServerHost;
    private final String _hostName;
    private final String _clusterName;
    private final int _carbonServerPort;
    private final NetClient _client;
    private final ConcurrentLinkedQueue<AggregatedData[]> _pending = new ConcurrentLinkedQueue<AggregatedData[]>();
    @Nullable
    private NetSocket _socket = null;
    private final Vertx _vertx;

    public CarbonPublisher(@Nonnull String carbonServer, String hostName, String clusterName) {
        _vertx = VertxFactory.newVertx();
        String[] split = carbonServer.split(":");
        _carbonServerHost = split[0];
        if (split.length > 1) {
            _carbonServerPort = Integer.parseInt(split[1]);
        } else {
            _carbonServerPort = 2003;
        }

        _hostName = hostName;
        _clusterName = clusterName;

        _client = _vertx.createNetClient();

        _client.setReconnectAttempts(3).setReconnectInterval(3000L).setConnectTimeout(5000).setTCPNoDelay(true).setTCPKeepAlive(true);
        connectToAggServer();
    }

    private void connectToAggServer() {
        _connectionInProgress.set(true);
        LOGGER.info("attempting to connect to carbon server at " + _carbonServerHost + ":" + _carbonServerPort);
        _client.connect(_carbonServerPort, _carbonServerHost, new AsyncResultHandler<NetSocket>() {
            @Override
            public void handle(@Nonnull final AsyncResult<NetSocket> event) {
                if (event.succeeded()) {
                    LOGGER.info("connected to carbon server at " + _carbonServerHost + ":" + _carbonServerPort);
                    _socket = event.result();
                    _socket.exceptionHandler(createSocketExceptionHandler(_socket));
                    _socket.closeHandler(createSocketCloseHandler(_socket));
                    _connected.set(true);
                    _connectionInProgress.set(false);
                    flushPending();
                } else if (event.failed()) {
                    LOGGER.warn("failed to connect to carbon server at " + _carbonServerHost + ":" + _carbonServerPort);
                    _connectionInProgress.set(false);
                    _connected.set(false);
                    connectToAggServer();
                }
            }
        });
    }

    @Nullable
    private Handler<Void> createSocketCloseHandler(final NetSocket socket) {
        return new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                _socket = null;
                _connected.set(false);
                LOGGER.error("carbon server publisher socket closed, attempting to reestablish connection", event);
                connectToAggServer();
            }
        };
    }

    @Nullable
    private Handler<Throwable> createSocketExceptionHandler(final NetSocket socket) {
        return new Handler<Throwable>() {
            @Override
            public void handle(final Throwable event) {
                _socket = null;
                _connected.set(false);
                LOGGER.error("error on carbon server publisher socket, attempting to reestablish connection", event);
                connectToAggServer();
            }
        };
    }

    @Override
    public void recordAggregation(final AggregatedData[] data) {
        _pending.offer(data);
        flushPending();
    }

    private void flushPending() {
        if (!_connected.get()) {
            LOGGER.warn("not connected, cannot flush pending data");
            return;
        }
        AggregatedData[] dataChunk;
        while (_connected.get() && (dataChunk = _pending.poll()) != null) {
            for (@Nonnull AggregatedData data : dataChunk) {
                //Don't aggregate metrics < 1 minute
                if (data.getPeriod().toStandardDuration().isShorterThan(org.joda.time.Duration.standardMinutes(1))) {
                    continue;
                }

                String metric = String.format("%s.%s.%s.%s.%s.%s %f %d%n", _clusterName, _hostName, data.getService(), data.getMetric(),
                        data.getPeriod().toString(), data.getStatistic().getName(), data.getValue(),
                        data.getPeriodStart().toInstant().getMillis() / 1000);

                _socket.write(metric);
            }
        }
    }

    @Override
    public void close() {
        _socket.close();
    }
}
