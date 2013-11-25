package com.arpnetworking.tsdaggregator.aggserver;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.redis.RedisMod;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Vert.x aggregation server class.
 *
 * @author barp
 */
public class AggregationServer extends Verticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationServer.class);
    private static final String EB_REDIS_PREFIX = "agg-redis-";
    private static final String REDIS_LAYER_NAME = "redis";
    private static final String AGG_LAYER_NAME = "agg";
    private static final ReadableDuration HOST_HEARTBEAT_TIMEOUT = Duration.standardMinutes(2);
    @Nonnull
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> _knownClusters;
    private final Set<RedisInstance> _connectedRedisHosts = new ConcurrentSkipListSet<>();
    private final AtomicInteger _redisSeq = new AtomicInteger(1);
    private final KetamaRing _ketamaRing = new KetamaRing();
    private final ConcurrentSkipListMap<String, AggServerStatus> _aggServers = new ConcurrentSkipListMap<>();
    private String _hostName;
    private int _tcpPort;
    private int _httpPort;
    private int _hazelcastPort;
    @Nullable
    private HazelcastInstance _hazelcast = null;
    private AtomicBoolean _hazelcastStarting = new AtomicBoolean(false);
    private ITopic<String> _clusterMemberTopic;
    private State _serverStatus;

    public AggregationServer() {
        _knownClusters = new ConcurrentHashMap<>();
    }

    private void startUpHazelcast() {
        @Nonnull final Config config = new Config();

        final NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(_hazelcastPort);
        networkConfig.setPublicAddress(_hostName.substring(0, _hostName.indexOf(':')));
        networkConfig.setPortAutoIncrement(true);
        config.setNetworkConfig(networkConfig);

        Join join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        final TcpIpConfig ipConfig = join.getTcpIpConfig();
        for (@Nonnull AggServerStatus server : _aggServers.values()) {
            if (server.getName().equals(_hostName)) {
                LOGGER.debug("found myself in server list, skipping");
                continue;
            }
            LOGGER.info("adding server to hazelcast connection list: " + server);
            if (server.getHeartbeatTime().isAfter(DateTime.now().minus(HOST_HEARTBEAT_TIMEOUT))) {
                String hazelcastEndpoint = server.getName();
                if (hazelcastEndpoint.indexOf(':') >= 0) {
                    String[] split = hazelcastEndpoint.split(":");
                    int port = Integer.valueOf(split[1]);
                    port += 2;
                    hazelcastEndpoint = split[0] + ":" + port;
                }
                ipConfig.addMember(hazelcastEndpoint);
            }
        }
        ipConfig.setEnabled(true);

        @Nonnull TopicConfig membershipTopic = new TopicConfig();
        membershipTopic.setGlobalOrderingEnabled(false);
        membershipTopic.setName("cluster_membership");

        @Nonnull ListenerConfig membershipListenerConfig = new ListenerConfig();
        membershipListenerConfig.setImplementation(new MessageListener<String>() {
            @Override
            public void onMessage(@Nonnull final com.hazelcast.core.Message<String> message) {
                processServerStatusChange(message.getMessageObject());
            }
        });
        membershipTopic.addMessageListenerConfig(membershipListenerConfig);
        config.addTopicConfig(membershipTopic);


        LOGGER.info("starting hazelcast classes");
        _hazelcast = Hazelcast.newHazelcastInstance(config);
        _clusterMemberTopic = _hazelcast.getTopic("cluster_membership");
        changeMyState(State.Active, true);
    }

    private void changeMyState(final State state, final boolean notifyHazelcastCluster) {
        //Get the redis instance we should use to write our heartbeat
        _serverStatus = state;
        writeMyState(notifyHazelcastCluster);
    }

    private void writeMyState(final boolean notifyHazelcastCluster) {
        if (_connectedRedisHosts.isEmpty()) {
            LOGGER.warn("not writing to state to redis, no redis instance found");
            return;
        }
        RedisInstance redis = getRedisInstanceFor(_hostName);
        Map<String, String> map = new HashMap<>();
        map.put("status", _serverStatus.toString());
        map.put("heartbeat", Long.toString(DateTime.now().getMillis()));
        RedisUtils.hmset(redis, "agg." + _hostName + ".status", map, vertx.eventBus(), new AsyncResultHandler<Void>() {
            @Override
            public void handle(final AsyncResult<Void> event) {
                if (event.succeeded() && notifyHazelcastCluster) {
                    _clusterMemberTopic.publish(_hostName);
                }
            }
        });
        RedisUtils.sadd(redis, getAggHostsKey(), _hostName, null, vertx.eventBus());
    }

    private void heartbeat() {
        vertx.setTimer(10000, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                heartbeat();
            }
        });
        if (!_connectedRedisHosts.isEmpty()) {
            writeMyState(false);
        }
    }

    private void processServerStatusChange(final String aggServer) {
        getAggServerStatus(aggServer, new AsyncResultHandler<AggServerStatus>() {
            @Override
            public void handle(final AsyncResult<AggServerStatus> event) {
                if (event.succeeded()) {
                    LOGGER.debug("updating agg server record for " + aggServer + ": " + event.result());
                    _aggServers.put(aggServer, event.result());
                } else {
                    LOGGER.warn("error getting updated data for agg server " + aggServer, event.cause());
                }
            }
        });
    }

    @Override
    public void start() {
        deployRedis(container.config().getArray("redisAddress"));
        final int port = container.config().getNumber("port").intValue();
        _hostName = container.config().getString("name") + ":" + port;
        _tcpPort = port;
        _httpPort = port + 1;
        _hazelcastPort = port + 2;
        changeMyState(State.ComingOnline, false);

        vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(@Nonnull final NetSocket socket) {
                LOGGER.info("Accepted connection from " + socket.remoteAddress());
                @Nonnull final AggregatorConnection connection =
                        new AggregatorConnection(socket, new AggregatorConnection.ClusterNameResolvedCallback() {
                            @Override
                            public void clusterNameResolved(AggregatorConnection connection,
                                                            @Nonnull final String hostName,
                                                            @Nonnull final String clusterName) {
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
                    public void handle(@Nonnull final Buffer data) {
                        LOGGER.debug("received " + data.length() + " bytes of data from " + socket.remoteAddress());
                        connection.dataReceived(data);
                    }
                });
            }
        }).setTCPNoDelay(true).setTCPKeepAlive(true).listen(port, new AsyncResultHandler<NetServer>() {
            @Override
            public void handle(@Nonnull final AsyncResult<NetServer> event) {
                if (event.succeeded()) {
                    LOGGER.info("Started aggregation server on port " + event.result().port());
                } else {
                    LOGGER.error("Aggregation server failed to bind listener", event.cause());
                }
            }
        });
        vertx.createHttpServer().setTCPKeepAlive(true).setTCPNoDelay(true)
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest event) {
                        handleHttpRequest(event);
                    }
                }).listen(port + 1, new AsyncResultHandler<HttpServer>() {
            @Override
            public void handle(final AsyncResult<HttpServer> event) {
                if (event.succeeded()) {
                    LOGGER.info("Started HTTP server on port " + (port + 1));
                } else {
                    LOGGER.error("Failed to start HTTP server, failed to bind listener", event.cause());
                }
            }
        });
        heartbeat();
        updateHostLoop();
    }

    private void handleHttpRequest(HttpServerRequest event) {
        LOGGER.debug("got http request with path " + event.path());

        if (event.path().equals("/status")) {
            JsonObject response = new JsonObject();
            final JsonObject dataObject = new JsonObject();
            final JsonArray aggServersArray = new JsonArray();
            for (Map.Entry<String, AggServerStatus> entry : _aggServers.entrySet()) {
                aggServersArray.add(new JsonObject().putString("name", entry.getValue().getName())
                        .putString("status", entry.getValue().getState().name())
                        .putString("heartbeat", entry.getValue().getHeartbeatTime().toString()));
            }
            dataObject.putArray("aggServers", aggServersArray);
            final JsonArray redisServersArray = new JsonArray();
            for (RedisInstance instance : _connectedRedisHosts) {
                redisServersArray.add(new JsonObject().putString("host", instance.getHostName())
                        .putString("ebName", instance.getEBName()));
            }
            dataObject.putArray("redisServers", redisServersArray);
            JsonObject ketamaRingObject = new JsonObject();
            final Set<String> layers = _ketamaRing.getLayers();
            JsonArray layersArray = new JsonArray();
            for (String layer : layers) {
                JsonObject layerObject = new JsonObject();
                layersArray.add(layerObject);
                layerObject.putString("name", layer);
                JsonArray entriesArray = new JsonArray();
                layerObject.putArray("entries", entriesArray);
                final Set<Map.Entry<Integer, KetamaRing.NodeEntry>> entries = _ketamaRing.getRingEntries(layer);
                for (Map.Entry<Integer, KetamaRing.NodeEntry> entry : entries) {
                    JsonObject entryObject = new JsonObject().putNumber("key", entry.getKey())
                            .putString("nodeKey", entry.getValue().getNodeKey())
                            .putString("status", entry.getValue().getStatus().name());
                    entriesArray.add(entryObject);
                }
            }
            ketamaRingObject.putArray("layers", layersArray);
            dataObject.putObject("ketamaRing", ketamaRingObject);
            response.putString("status", "success").putObject("data", dataObject);
            Buffer buffer = new Buffer();
            String responseString = response.encode();
            buffer.appendString(responseString);
            event.response().setStatusCode(200)
                    .putHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(buffer.length())).write(buffer).end();
        } else {
            event.response().setStatusCode(404).end();
        }
    }

    private void deployRedis(@Nonnull JsonArray redisAddresses) {
        for (Object redisAddressObject : redisAddresses) {
            @Nonnull String redisAddress = (String) redisAddressObject;
            @Nonnull final String ebAddress = EB_REDIS_PREFIX + _redisSeq.getAndIncrement();
            String[] split = redisAddress.split(":");
            String host = split[0];
            int port = 6379;
            if (split.length > 1) {
                port = Integer.parseInt(split[1]);
            }

            @Nonnull final String normalizedAddress = host.toLowerCase() + ":" + port;

            @Nonnull JsonObject conf = new JsonObject();
            conf.putString("address", ebAddress);
            conf.putString("host", host);
            conf.putNumber("port", port);
            conf.putString("encoding", "UTF-8");

            container.deployVerticle(RedisMod.class.getCanonicalName(), conf, 3, new AsyncResultHandler<String>() {
                @Override
                public void handle(@Nonnull final AsyncResult<String> event) {
                    if (event.succeeded()) {
                        @Nonnull RedisInstance ri = new RedisInstance(normalizedAddress, ebAddress);
                        _connectedRedisHosts.add(ri);
                        _ketamaRing.addNode(normalizedAddress, ri, REDIS_LAYER_NAME);
                        LOGGER.info("Redis module started, address = " + normalizedAddress +
                                ", bus address = " + ebAddress + ", deployment id " + event.result() + ".");
                        refreshClusterData();
                        bootstrapHazelcastCluster();
                    } else {
                        LOGGER.error("Error starting redis module", event.cause());
                    }
                }
            });
        }
    }

    private void bootstrapHazelcastCluster() {
        if (_hazelcastStarting.get()) {
            return;
        }
        _hazelcastStarting.set(true);
        LOGGER.info("attempting to start/join hazelcast cluster");
        //get the agg hosts for the cluster
        updateAllHostsStatuses(new AsyncResultHandler<Void>() {
            @Override
            public void handle(final AsyncResult<Void> event) {
                try {
                    startUpHazelcast();
                } catch (Exception e) {
                    LOGGER.warn("failed to start hazelcast cluster, trying again in 60 seconds", e);
                    _hazelcastStarting.set(false);
                    vertx.setTimer(60000, new Handler<Long>() {
                        @Override
                        public void handle(final Long event) {
                            bootstrapHazelcastCluster();
                        }
                    });
                }
            }
        });
    }

    private void updateHostLoop() {
        vertx.setTimer(60000, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                updateHostLoop();
                updateAllHostsStatuses(null);
            }
        });
    }

    private void updateAllHostsStatuses(final AsyncResultHandler<Void> resultHandler) {
        RedisUtils.getAllSMEMBERS(_connectedRedisHosts, getAggHostsKey(), vertx.eventBus(),
                new AsyncResultHandler<Set<String>>() {
                    @Override
                    public void handle(final AsyncResult<Set<String>> event) {
                        if (!event.succeeded()) {
                            _hazelcastStarting.set(false);
                            vertx.setTimer(10000, new Handler<Long>() {
                                @Override
                                public void handle(final Long event) {
                                    bootstrapHazelcastCluster();
                                }
                            });
                        } else {

                            final Set<String> aggHosts = event.result();
                            if (aggHosts.isEmpty()) {
                                LOGGER.info(
                                        "Agg server entry was empty, starting up hazelcast as founding cluster member");
                                startUpHazelcast();
                            }
                            final AtomicInteger countDown = new AtomicInteger(aggHosts.size());
                            for (final String host : aggHosts) {
                                getAggServerStatus(host, new AsyncResultHandler<AggServerStatus>() {
                                    @Override
                                    public void handle(final AsyncResult<AggServerStatus> event) {
                                        if (!event.succeeded()) {
                                            LOGGER.warn("Error getting agg server status for server " + host,
                                                    event.cause());
                                        } else {
                                            registerAndUpdateAggServer(event.result());
                                        }
                                        int val = countDown.decrementAndGet();
                                        if (val == 0 && resultHandler != null) {
                                            resultHandler.handle(new ASResult<Void>((Void) null));
                                        }
                                    }
                                });
                            }
                        }
                    }
                });

    }

    private String getAggHostsKey() {
        return "agg.hosts";
    }

    private void registerAndUpdateAggServer(final AggServerStatus aggServer) {
        LOGGER.info("updating state of agg server " + aggServer);
        AggServerStatus currentStatus = _aggServers.get(aggServer.getName());
        if (currentStatus == null) {
            _ketamaRing.addNode(aggServer.getName(), aggServer, AGG_LAYER_NAME, aggServer.getState());
        }
        _aggServers.put(aggServer.getName(), aggServer);
    }

    private void getAggServerStatus(final String server, final AsyncResultHandler<AggServerStatus> resultHandler) {
        RedisInstance redis = getRedisInstanceFor(server);
        RedisUtils.hgetall(redis, getAggServerStatusKey(server), new AsyncResultHandler<Map<String, String>>() {
            @Override
            public void handle(final AsyncResult<Map<String, String>> event) {
                if (event.succeeded()) {
                    Map<String, String> map = event.result();
                    if (map.isEmpty()) {
                        LOGGER.warn("found empty map looking up status of server " + server);
                        resultHandler
                                .handle(new ASResult<AggServerStatus>(new Exception("empty map for server " + server)));
                        return;
                    }
                    String status = map.get("status");
                    AggServerStatus aggStatus = new AggServerStatus(server, State.valueOf(status),
                            new DateTime(Long.valueOf(map.get("heartbeat"))));
                    if (aggStatus.getHeartbeatTime().isBefore(DateTime.now().minus(HOST_HEARTBEAT_TIMEOUT))) {
                        aggStatus.setState(State.PresumedDead);
                    }
                    ASResult<AggServerStatus> result = new ASResult<>(aggStatus);
                    resultHandler.handle(result);
                } else {
                    LOGGER.warn("Error getting server status for server " + server, event.cause());
                    resultHandler.handle(new ASResult<AggServerStatus>(event.cause()));
                }
            }
        }, vertx.eventBus());
    }

    private String getAggServerStatusKey(final String server) {
        return "agg." + server + ".status";
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
        for (@Nonnull final RedisInstance redis : _connectedRedisHosts) {
            RedisUtils.smembers(redis, "clusters", new AsyncResultHandler<Set<String>>() {
                @Override
                public void handle(@Nonnull final AsyncResult<Set<String>> event) {
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

    private void refreshClusterMembership(@Nonnull final RedisInstance redis, final String cluster) {
        RedisUtils.smembers(redis, getClusterMembershipKey(cluster), new AsyncResultHandler<Set<String>>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Set<String>> event) {
                final ConcurrentSkipListSet<String> set = _knownClusters.get(cluster);
                for (String host : event.result()) {
                    set.add(host);
                    LOGGER.info("adding host " + host + " to cluster " + cluster);
                }
            }

        }, vertx.eventBus());
    }

    private void clusterResolved(AggregatorConnection connection, @Nonnull String hostName,
                                 @Nonnull String clusterName) {
        registerHostAndCluster(hostName, clusterName);
    }

    private void registerHostAndCluster(@Nonnull final String hostName, @Nonnull final String clusterName) {
        EventBus eb = vertx.eventBus();
        @Nullable RedisInstance hostRedis = getRedisInstanceFor(hostName);
        @Nullable RedisInstance clusterRedis = getRedisInstanceFor(clusterName);

        RedisUtils.sadd(hostRedis, "hosts", hostName, null, eb);
        RedisUtils.sadd(clusterRedis, "clusters", clusterName, null, eb);
        RedisUtils.sadd(clusterRedis, getClusterMembershipKey(clusterName), hostName, null, eb);
        updateLastSeen(hostName, hostRedis);
    }

    @Nullable
    private RedisInstance getRedisInstanceFor(@Nonnull String key) {
        final KetamaRing.NodeEntry entry = _ketamaRing.hash(key, REDIS_LAYER_NAME);
        return (RedisInstance) entry.getMappedObject();
    }

    private void updateLastSeen(final String hostName, @Nonnull final RedisInstance hostRedis) {
        final EventBus eb = vertx.eventBus();
        final JsonObject hostLastSeen = new JsonObject().putString("command", "SET")
                .putArray("args", new JsonArray().add(getHostLastSeenKey(hostName)).add(DateTime.now().getMillis()));
        eb.send(hostRedis.getEBName(), hostLastSeen, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.info("response data = " + event.body());
            }
        });
    }

    @Nonnull
    private String getHostLastSeenKey(final String hostName) {

        return "host." + hostName + ".lastSeen";
    }

    @Nonnull
    private String getClusterMembershipKey(final String clusterName) {
        return "cluster.members." + clusterName;
    }

    private void aggregationRecordArrived(AggregatorConnection connection, Messages.AggregationRecord record) {
        //TODO(brandon): the magic with the aggregation record
    }
}
