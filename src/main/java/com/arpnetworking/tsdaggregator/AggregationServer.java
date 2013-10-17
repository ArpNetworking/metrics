package com.arpnetworking.tsdaggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vert.x aggregation server class.
 *
 * @author barp
 */
public class AggregationServer extends Verticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationServer.class);
    private final ConcurrentHashMap<String, ArrayList<AggregatorConnection>> _clusterConnections;

    public AggregationServer() {
        _clusterConnections = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        final int port = container.config().getNumber("port").intValue();
        vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket socket) {
                LOGGER.info("Accepted connection from " + socket.remoteAddress());
                final AggregatorConnection connection =
                        new AggregatorConnection(socket, new AggregatorConnection.ClusterNameResolvedCallback() {
                            @Override
                            public void clusterNameResolved(final String clusterName, AggregatorConnection connection) {
                                ArrayList<AggregatorConnection> connections = _clusterConnections.get(clusterName);
                                if (connections == null) {
                                    ArrayList<AggregatorConnection> newList = new ArrayList<>();
                                    connections = _clusterConnections.putIfAbsent(clusterName, newList);
                                    if (connections == null) {
                                        connections = newList;
                                    }
                                }
                                connections.add(connection);
                            }
                        });
                socket.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {
                        LOGGER.info("received " + data.length() + " bytes of data from " + socket.remoteAddress());
                        connection.dataReceived(data);
                    }
                });
            }
        }
        ).setTCPNoDelay(true).setTCPKeepAlive(true).listen(port, new Handler<AsyncResult<NetServer>>() {
            @Override
            public void handle(final AsyncResult<NetServer> event) {
                if (event.succeeded()) {
                    LOGGER.info("Started aggregation server on port " + event.result().port());
                } else {
                    LOGGER.error("Aggregation server failed to bind listener", event.cause());
                }
            }
        });
    }
}
