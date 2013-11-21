package com.arpnetworking.tsdaggregator.aggserver;

import io.vertx.redis.RedisMod;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vert.x aggregation server class.
 *
 * @author barp
 */
public class AggregationServer extends Verticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationServer.class);
    private static final String EB_REDIS_PREFIX = "agg-redis-";
    private static final String REDIS_LAYER_NAME = "redis";
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> _knownClusters;
    private final Set<RedisInstance> _connectedRedisHosts = new ConcurrentSkipListSet<>();
    private final AtomicInteger _redisSeq = new AtomicInteger(1);
    private final KetamaRing _ketamaRing = new KetamaRing();

    public AggregationServer() {
        _knownClusters = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        deployRedis(container.config().getString("redisAddress"));
        final int port = container.config().getNumber("port").intValue();

        vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket socket) {
                LOGGER.info("Accepted connection from " + socket.remoteAddress());
                final AggregatorConnection connection =
                        new AggregatorConnection(socket, new AggregatorConnection.ClusterNameResolvedCallback() {
                            @Override
                            public void clusterNameResolved(AggregatorConnection connection, final String hostName,
                                                            final String clusterName) {
                                clusterResolved(connection, hostName, clusterName);
                            }
                        }, new AggregatorConnection.AggregationArrivedCallback() {
                            @Override
                            public void aggregationArrived(final AggregatorConnection connection,
                                                           final Messages.AggregationRecord record) {
                                aggregationRecordArrived(connection, record);
                            }
                        }
                        );
                socket.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {
                        LOGGER.debug("received " + data.length() + " bytes of data from " + socket.remoteAddress());
                        connection.dataReceived(data);
                    }
                });
            }
        }
        ).setTCPNoDelay(true).setTCPKeepAlive(true).listen(port, new AsyncResultHandler<NetServer>() {
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

    private void deployRedis(String redisAddress) {
        final String ebAddress = EB_REDIS_PREFIX + _redisSeq.getAndIncrement();
        String[] split = redisAddress.split(":");
        String host = split[0];
        int port = 6379;
        if (split.length > 1) {
            port = Integer.parseInt(split[1]);
        }

        final String normalizedAddress = host.toLowerCase() + ":" + port;

        JsonObject conf = new JsonObject();
        conf.putString("address", ebAddress);
        conf.putString("host", host);
        conf.putNumber("port", port);
        conf.putString("encoding", "UTF-8");

        container.deployVerticle(RedisMod.class.getCanonicalName(), conf, 3, new AsyncResultHandler<String>() {
            @Override
            public void handle(final AsyncResult<String> event) {
                if (event.succeeded()) {
                    RedisInstance ri = new RedisInstance(normalizedAddress, ebAddress);
                    _connectedRedisHosts.add(ri);
                    _ketamaRing.addNode(normalizedAddress, ri, REDIS_LAYER_NAME);
                    LOGGER.info("Redis module started, address = " + normalizedAddress +
                            ", bus address = " + ebAddress + ", deployment id " + event.result() + ".");
                } else {
                    LOGGER.error("Error starting redis module", event.cause());
                }
            }
        }
        );

        vertx.setTimer(5000, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                refreshClusterData();
            }
        });
    }

    private long getClusterRefreshTimerDelay() {
        //3 minutes +- 15 seconds.
        return (long) (180000 + Math.random() * 30000 - 15000);
    }

    private void refreshClusterData() {
        vertx.setTimer(getClusterRefreshTimerDelay(), new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                refreshClusterData();
            }
        });
        for (final RedisInstance redis : _connectedRedisHosts) {
            RedisUtils.getRedisSetEntries(redis, "clusters", new AsyncResultHandler<List<String>>() {
                @Override
                public void handle(final AsyncResult<List<String>> event) {
                    for (String cluster : event.result()) {
                        LOGGER.info("adding cluster " + cluster + " to known list");
                        if (!_knownClusters.containsKey(cluster)) {
                            _knownClusters.putIfAbsent(cluster, new ConcurrentSkipListSet<String>());
                        }
                        refreshClusterMembership(redis, cluster);
                    }
                }
            }, vertx.eventBus());
        }
    }

    private void refreshClusterMembership(final RedisInstance redis, final String cluster) {
        RedisUtils.getRedisSetEntries(redis, getClusterMembershipKey(cluster), new AsyncResultHandler<List<String>>() {
            @Override
            public void handle(final AsyncResult<List<String>> event) {
                final ConcurrentSkipListSet<String> set = _knownClusters.get(cluster);
                for (String host : event.result()) {
                    set.add(host);
                    LOGGER.info("adding host " + host + " to cluster " + cluster);
                }
            }
        }, vertx.eventBus());
    }

    private void clusterResolved(AggregatorConnection connection, String hostName, String clusterName) {
        registerHostAndCluster(hostName, clusterName);
    }

    private void registerHostAndCluster(final String hostName, final String clusterName) {
        EventBus eb = vertx.eventBus();
        RedisInstance hostRedis = getRedisInstanceFor(hostName);
        RedisInstance clusterRedis = getRedisInstanceFor(clusterName);

        RedisUtils.addEntryToRedisSet(hostRedis, "hosts", hostName, null, vertx.eventBus());
        RedisUtils.addEntryToRedisSet(clusterRedis, "clusters", clusterName, null, vertx.eventBus());
        RedisUtils.addEntryToRedisSet(clusterRedis, getClusterMembershipKey(clusterName), hostName, null,
                vertx.eventBus());
        updateLastSeen(hostName, hostRedis);
    }

    private RedisInstance getRedisInstanceFor(String key) {
        final KetamaRing.NodeEntry entry = _ketamaRing.hash(key, REDIS_LAYER_NAME);
        return (RedisInstance) entry.getMappedObject();
    }

    private void updateLastSeen(final String hostName, final RedisInstance hostRedis) {
        final EventBus eb = vertx.eventBus();
        final JsonObject hostLastSeen = new JsonObject().putString("command", "SET")
                .putArray("args", new JsonArray().add(getHostLastSeenKey(hostName)).add(DateTime.now().getMillis()));
        eb.send(hostRedis.getEBName(), hostLastSeen,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> event) {
                        LOGGER.info("response data = " + event.body());
                    }
                });
    }

    private String getHostLastSeenKey(final String hostName) {

        return "host." + hostName + ".lastSeen";
    }

    private String getClusterMembershipKey(final String clusterName) {
        return "cluster.members." + clusterName;
    }

    private void aggregationRecordArrived(AggregatorConnection connection, Messages.AggregationRecord record) {

    }

}
