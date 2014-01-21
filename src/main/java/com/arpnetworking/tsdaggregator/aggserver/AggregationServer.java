package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
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
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.List;
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
    private static final ReadableDuration HOST_HEARTBEAT_TIMEOUT = Duration.standardMinutes(2);
    @Nonnull
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<String, ServerStatus>> _knownClusters;
    private final Set<RedisInstance> _connectedRedisHosts = new ConcurrentSkipListSet<RedisInstance>();
    private final AtomicInteger _redisSeq = new AtomicInteger(1);
    private final KetamaRing _ketamaRing = new KetamaRing();
    private final ConcurrentSkipListMap<String, ServerStatus> _aggServers = new ConcurrentSkipListMap<String, ServerStatus>();
    //Aggregation records
    private final ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>>> _registeredMetrics =
            new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>>>();
    private String _hostName;
    private int _tcpPort;
    private int _httpPort;
    private int _hazelcastPort;
    @Nullable
    private HazelcastInstance _hazelcast = null;
    @Nonnull
    private AtomicBoolean _hazelcastStarting = new AtomicBoolean(false);
    private ITopic<String> _clusterMemberTopic;
    private State _serverStatus;

    public AggregationServer() {
        _knownClusters = new ConcurrentHashMap<String, ConcurrentSkipListMap<String, ServerStatus>>();
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
        for (@Nonnull ServerStatus server : _aggServers.values()) {
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
        Map<String, String> map = Maps.newHashMap();
        map.put("status", _serverStatus.toString());
        map.put("heartbeat", Long.toString(DateTime.now().getMillis()));
        RedisUtils.hmset(redis, "agg." + _hostName + ".status", map, vertx.eventBus(), new AsyncResultHandler<Void>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Void> event) {
                if (event.succeeded() && notifyHazelcastCluster) {
                    _clusterMemberTopic.publish(_hostName);
                }
            }
        });
        RedisUtils.sadd(redis, getAggHostsKey(), _hostName, vertx.eventBus(), null);
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

    private void processServerStatusChange(@Nonnull final String aggServer) {
        getAggServerStatus(aggServer, new AsyncResultHandler<ServerStatus>() {
            @Override
            public void handle(@Nonnull final AsyncResult<ServerStatus> event) {
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
                            public void clusterNameResolved(AggregatorConnection connection, @Nonnull final String hostName,
                                                            @Nonnull final String clusterName)
                            {
                                clusterResolved(hostName, clusterName);
                            }
                        }, new AggregatorConnection.AggregationArrivedCallback() {
                            @Override
                            public void aggregationArrived(@Nonnull final AggregatorConnection connection,
                                                           @Nonnull final Messages.AggregationRecord record)
                            {
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
        vertx.createHttpServer().setTCPKeepAlive(true).setTCPNoDelay(true).requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(@Nonnull final HttpServerRequest event) {
                handleHttpRequest(event);
            }
        }).listen(port + 1, new AsyncResultHandler<HttpServer>() {
            @Override
            public void handle(@Nonnull final AsyncResult<HttpServer> event) {
                if (event.succeeded()) {
                    LOGGER.info("Started HTTP server on port " + (port + 1));
                } else {
                    LOGGER.error("Failed to start HTTP server, failed to bind listener", event.cause());
                }
            }
        });
        heartbeat();
        updateHostLoop();
        updateMetricsLoop();
    }

    private void handleHttpRequest(@Nonnull HttpServerRequest event) {
        LOGGER.debug("got http request with path " + event.path());

        if (event.path().equals("/status")) {
            JsonObject response = getStatusJson();
            Buffer buffer = new Buffer();
            String responseString = response.encode();
            buffer.appendString(responseString);
            event.response().setStatusCode(200).end(buffer);
        } else {
            event.response().setStatusCode(404).end();
        }
    }

    private JsonObject getStatusJson() {
        JsonObject response = new JsonObject();
        final JsonObject dataObject = new JsonObject();
        final JsonArray aggServersArray = new JsonArray();
        for (Map.Entry<String, ServerStatus> entry : _aggServers.entrySet()) {
            aggServersArray.add(new JsonObject().putString("name", entry.getValue().getName())
                    .putString("status", entry.getValue().getState().name())
                    .putString("heartbeat", entry.getValue().getHeartbeatTime().toString()));
        }
        dataObject.putArray("aggServers", aggServersArray);
        final JsonArray clustersArray = new JsonArray();
        for (Map.Entry<String, ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>>> entry : _registeredMetrics
                .entrySet())
        {
            final JsonObject clusterObject = new JsonObject();
            clusterObject.putString("name", entry.getKey());

            final JsonArray servicesArray = new JsonArray();
            for (Map.Entry<String, ConcurrentSkipListMap<String, Metric>> mapEntry : entry.getValue().entrySet()) {
                JsonObject serviceObject = new JsonObject();
                serviceObject.putString("name", mapEntry.getKey());

                final JsonArray metricsArray = new JsonArray();
                for (Map.Entry<String, Metric> metric : mapEntry.getValue().entrySet()) {
                    metricsArray.add(metric.getKey());
                }
                serviceObject.putArray("metrics", metricsArray);
            }

            clusterObject.putArray("services", servicesArray);

            final JsonArray hostsArray = new JsonArray();
            for (Map.Entry<String, ServerStatus> statusEntry : _knownClusters.get(entry.getKey()).entrySet()) {
                hostsArray.add(statusEntry.getKey());
            }
            clusterObject.putArray("hosts", hostsArray);


            clustersArray.addObject(clusterObject);
        }
        dataObject.putArray("metrics", clustersArray);

        final JsonArray redisServersArray = new JsonArray();
        for (RedisInstance instance : _connectedRedisHosts) {
            redisServersArray.add(new JsonObject().putString("host", instance.getHostName()).putString("ebName", instance.getEBName()));
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
                JsonObject entryObject =
                        new JsonObject().putNumber("key", entry.getKey()).putString("nodeKey", entry.getValue().getNodeKey())
                                .putString("status", entry.getValue().getStatus().name());
                entriesArray.add(entryObject);
            }
        }
        ketamaRingObject.putArray("layers", layersArray);
        dataObject.putObject("ketamaRing", ketamaRingObject);
        response.putString("status", "success").putObject("data", dataObject);
        return response;
    }

    private void deployRedis(@Nonnull JsonArray redisAddresses) {
        for (Object redisAddressObject : redisAddresses) {
            @Nonnull String redisAddress = ( String ) redisAddressObject;
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
                        _ketamaRing.addNode(normalizedAddress, ri, KetamaLayers.REDIS.getVal());
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

    private void updateMetricsLoop() {
        //Random interval between 2 and 3 minutes
        vertx.setTimer(( long ) (Math.random() * 120000 + 60000), new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                updateMetricsLoop();
                updateMetrics();
            }
        });
    }

    private void updateMetrics() {
        LOGGER.info("Beginning metrics update sweep");
        //Walk through each of the clusters and services looking for metrics
        for (Map.Entry<String, ConcurrentSkipListMap<String, ServerStatus>> clustersEntry : _knownClusters.entrySet()) {
            final String cluster = clustersEntry.getKey();
            for (Map.Entry<String, ConcurrentSkipListMap<String, ServerStatus>> servicesEntry : _knownClusters.entrySet()) {
                final String service = servicesEntry.getKey();
                //Get all the metric names for the service
                getMetricsForClusterService(cluster, service, new AsyncResultHandler<List<String>>() {
                    @Override
                    public void handle(final AsyncResult<List<String>> event) {
                        if (event.succeeded()) {
                            final List<String> metricNames = event.result();
                            for (String metricName : metricNames) {
                                //Hash the metric string to see if this host should process it
                                String hashKey = "metrics." + cluster + "." + service + "." + metricName;
                                final KetamaRing.NodeEntry aggServer = _ketamaRing.hash(hashKey, KetamaLayers.AGG.getVal());
                                if (aggServer.getNodeKey().equals(_hostName)) {
                                    LOGGER.info("Found a metric I should be calculating: cluster=" + cluster + ", service=" + service +
                                            ", metric=" + metricName);
                                    if (_registeredMetrics.containsKey(cluster) && _registeredMetrics.get(cluster).containsKey(service)) {
                                        //We already have the metrics, but we might need to put them in to the to-calculate list.
                                    } else {
                                        //Need to lookup the metric info
                                    }
                                } else {
                                    LOGGER.info(
                                            "Found a metric for someone else: cluster=" + cluster + ", service=" + service + ", metric=" +
                                                    metricName + ", owned by=" + aggServer.getNodeKey());
                                }
                            }
                        } else {
                            LOGGER.warn("Error looking up metrics for cluster " + cluster + ", service " + service, event.cause());
                        }
                    }
                });
            }
        }
    }

    private void getMetricsForClusterService(@Nonnull final String cluster, @Nonnull final String service,
                                             @Nonnull final AsyncResultHandler<List<String>> resulthandler)
    {
        final String key = "metrics." + cluster + "." + service + ".metrics";

        RedisInstance redis = getRedisInstanceFor(key);
        EventBus bus = vertx.eventBus();
        RedisUtils.zrange(redis, key, 0, -1, bus, new AsyncResultHandler<List<String>>() {
            @Override
            public void handle(final AsyncResult<List<String>> event) {
                if (resulthandler != null) {
                    if (event.succeeded()) {
                        resulthandler.handle(new ASResult<List<String>>(event.result()));
                    } else {
                        resulthandler.handle(new ASResult<List<String>>(event.cause()));
                    }
                }
            }
        });
    }

    private void updateAllHostsStatuses(@Nullable final AsyncResultHandler<Void> resultHandler) {
        RedisUtils.getAllSMEMBERS(_connectedRedisHosts, getAggHostsKey(), vertx.eventBus(), new AsyncResultHandler<Set<String>>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Set<String>> event) {
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
                        LOGGER.info("Agg server entry was empty, starting up hazelcast as founding cluster member");
                        startUpHazelcast();
                    }
                    final AtomicInteger countDown = new AtomicInteger(aggHosts.size());
                    for (final String host : aggHosts) {
                        getAggServerStatus(host, new AsyncResultHandler<ServerStatus>() {
                            @Override
                            public void handle(@Nonnull final AsyncResult<ServerStatus> event) {
                                if (!event.succeeded()) {
                                    LOGGER.warn("Error getting agg server status for server " + host, event.cause());
                                } else {
                                    registerAndUpdateAggServer(event.result());
                                }
                                int val = countDown.decrementAndGet();
                                if (val == 0 && resultHandler != null) {
                                    resultHandler.handle(new ASResult<Void>(( Void ) null));
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Nonnull
    private String getAggHostsKey() {
        return "agg.hosts";
    }

    private void registerAndUpdateAggServer(@Nonnull final ServerStatus aggServer) {
        LOGGER.info("updating state of agg server " + aggServer);
        ServerStatus currentStatus = _aggServers.get(aggServer.getName());
        if (currentStatus == null) {
            _ketamaRing.addNode(aggServer.getName(), aggServer, KetamaLayers.AGG.getVal(), aggServer.getState());
        }
        _aggServers.put(aggServer.getName(), aggServer);
    }

    private void getAggServerStatus(@Nonnull final String server, @Nonnull final AsyncResultHandler<ServerStatus> resultHandler) {
        RedisInstance redis = getRedisInstanceFor(server);
        if (redis == null) {
            final String msg = "unable to get agg server status for server " + server + ".  No redis instance found for key.";
            LOGGER.warn(msg);
            resultHandler.handle(new ASResult<ServerStatus>(new IllegalStateException(msg)));
            return;
        }
        RedisUtils.hgetall(redis, getAggServerStatusKey(server), vertx.eventBus(), new AsyncResultHandler<Map<String, String>>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Map<String, String>> event) {
                if (event.succeeded()) {
                    Map<String, String> map = event.result();
                    if (map.isEmpty()) {
                        LOGGER.warn("found empty map looking up status of server " + server);
                        resultHandler.handle(new ASResult<ServerStatus>(new Exception("empty map for server " + server)));
                        return;
                    }
                    String status = map.get("status");
                    ServerStatus aggStatus =
                            new ServerStatus(server, State.valueOf(status), new DateTime(Long.valueOf(map.get("heartbeat"))));
                    if (aggStatus.getHeartbeatTime().isBefore(DateTime.now().minus(HOST_HEARTBEAT_TIMEOUT))) {
                        aggStatus.setState(State.PresumedDead);
                    }
                    ASResult<ServerStatus> result = new ASResult<ServerStatus>(aggStatus);
                    resultHandler.handle(result);
                } else {
                    LOGGER.warn("Error getting server status for server " + server, event.cause());
                    resultHandler.handle(new ASResult<ServerStatus>(event.cause()));
                }
            }
        });
    }

    @Nonnull
    private String getAggServerStatusKey(final String server) {
        return "agg." + server + ".status";
    }

    private long getClusterRefreshTimerDelay() {
        //3 minutes +- 15 seconds.
        return ( long ) (180000 + Math.random() * 30000 - 15000);
    }

    private void refreshClusterData() {
        vertx.setTimer(getClusterRefreshTimerDelay(), new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                refreshClusterData();
            }
        });
        for (@Nonnull final RedisInstance redis : _connectedRedisHosts) {
            RedisUtils.smembers(redis, "clusters", vertx.eventBus(), new AsyncResultHandler<Set<String>>() {
                @Override
                public void handle(@Nonnull final AsyncResult<Set<String>> event) {
                    for (String cluster : event.result()) {
                        LOGGER.info("adding cluster " + cluster + " to known list");
                        if (!_knownClusters.containsKey(cluster)) {
                            _knownClusters.putIfAbsent(cluster, new ConcurrentSkipListMap<String, ServerStatus>());
                        }
                        refreshClusterMembership(redis, cluster);
                    }
                }
            });
        }
    }

    private void refreshClusterMembership(@Nonnull final RedisInstance redis, final String cluster) {
        RedisUtils.smembers(redis, getClusterMembershipKey(cluster), vertx.eventBus(), new AsyncResultHandler<Set<String>>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Set<String>> event) {
                final ConcurrentSkipListMap<String, ServerStatus> map = _knownClusters.get(cluster);
                for (final String host : event.result()) {
                    final ServerStatus status = map.get(host);
                    if (status == null) {
                        map.putIfAbsent(host, new ServerStatus(host, State.Active, DateTime.now()));
                        //If there is already an entry present, that's good enough, we'll update the heartbeat later
                    }
                    LOGGER.info("adding host " + host + " to cluster " + cluster);
                    getHostLastSeen(host, getRedisInstanceFor(host), new AsyncResultHandler<DateTime>() {
                        @Override
                        public void handle(@Nonnull final AsyncResult<DateTime> event) {
                            if (event.succeeded()) {
                                ServerStatus status = map.get(host);
                                status.setHeartbeatTime(event.result());
                                if (event.result().isBefore(DateTime.now().minus(HOST_HEARTBEAT_TIMEOUT))) {
                                    status.setState(State.PresumedDead);
                                } else {
                                    status.setState(State.Active);
                                }
                                LOGGER.info("new host entry: " + status);
                            } else {
                                LOGGER.warn("Unable to get host heartbeat for host " + host, event.cause());
                            }
                        }
                    });
                }
            }

        });
    }

    private void clusterResolved(@Nonnull String hostName, @Nonnull String clusterName) {
        registerHostAndCluster(hostName, clusterName);
    }

    private void registerHostAndCluster(@Nonnull final String hostName, @Nonnull final String clusterName) {
        EventBus eb = vertx.eventBus();
        @Nullable RedisInstance hostRedis = getRedisInstanceFor(hostName);
        @Nullable RedisInstance clusterRedis = getRedisInstanceFor(clusterName);

        RedisUtils.sadd(hostRedis, "hosts", hostName, eb, null);
        RedisUtils.sadd(clusterRedis, "clusters", clusterName, eb, null);
        RedisUtils.sadd(clusterRedis, getClusterMembershipKey(clusterName), hostName, eb, null);
        updateLastSeen(hostName, hostRedis);
    }

    private void registerMetric(@Nonnull final String cluster, @Nonnull final String serviceName, @Nonnull final String metricName,
                                @Nullable final AsyncResultHandler<Metric> callback)
    {
        EventBus eb = vertx.eventBus();

        String key = "metrics." + cluster + "." + serviceName + ".metrics";
        @Nullable RedisInstance metricsRedis = getRedisInstanceFor(key);
        if (metricsRedis == null) {
            final String msg = "unable to register metric " + metricName + ".  No redis instance found for key.";
            LOGGER.warn(msg);
            if (callback != null) {
                callback.handle(new ASResult<Metric>(new IllegalStateException(msg)));
            }
            return;
        }
        int metricsHash = _ketamaRing.getHashCodeFor(metricName);
        RedisUtils.zadd(metricsRedis, key, metricsHash, metricName, eb, new AsyncResultHandler<Integer>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Integer> event) {
                if (event.succeeded()) {
                    final Metric metric = new Metric(cluster, serviceName, metricName);
                    _registeredMetrics.get(cluster).get(serviceName).put(metricName, metric);
                    if (callback != null) {
                        callback.handle(new ASResult<Metric>(metric));
                    }
                } else {
                    if (callback != null) {
                        callback.handle(new ASResult<Metric>(event.cause()));
                    }
                }
            }
        });
    }

    private void registerAggregationForMetric(@Nonnull final Metric metric, @Nonnull final String aggregation,
                                              @Nullable final AsyncResultHandler<String> callback)
    {
        EventBus eb = vertx.eventBus();
        String serverKey = getServerKeyForMetric(metric);
        RedisInstance metricRedis = getRedisInstanceFor(serverKey);
        if (metricRedis == null) {
            final String msg = "unable to add aggregation to metric " + metric + ".  No redis instance found for key.";
            LOGGER.warn(msg);
            if (callback != null) {
                callback.handle(new ASResult<String>(new IllegalStateException(msg)));
            }
            return;
        }
        String key = serverKey + ".aggs";
        RedisUtils.sadd(metricRedis, key, aggregation, eb, new AsyncResultHandler<Integer>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Integer> event) {
                if (event.succeeded()) {
                    if (callback != null) {
                        callback.handle(new ASResult<String>(aggregation));
                    }
                } else {
                    if (callback != null) {
                        callback.handle(new ASResult<String>(event.cause()));
                    }
                }
            }
        });
    }

    private String getServerKeyForMetric(final Metric metric) {
        return "metrics." + metric.getCluster() + "." + metric.getService() + "." + metric.getName();
    }

    private void registerPeriodForMetric(@Nonnull final Metric metric, @Nonnull final String period,
                                         @Nullable final AsyncResultHandler<String> callback)
    {
        EventBus eb = vertx.eventBus();
        String serverKey = getServerKeyForMetric(metric);
        RedisInstance metricRedis = getRedisInstanceFor(serverKey);
        if (metricRedis == null) {
            final String msg = "unable to add period to metric " + metric + ".  No redis instance found for key.";
            LOGGER.warn(msg);
            if (callback != null) {
                callback.handle(new ASResult<String>(new IllegalStateException(msg)));
            }
            return;
        }
        String key = serverKey + ".periods";
        RedisUtils.sadd(metricRedis, key, period, eb, new AsyncResultHandler<Integer>() {
            @Override
            public void handle(@Nonnull final AsyncResult<Integer> event) {
                if (event.succeeded()) {
                    if (callback != null) {
                        callback.handle(new ASResult<String>(period));
                    }
                } else {
                    if (callback != null) {
                        callback.handle(new ASResult<String>(event.cause()));
                    }
                }
            }
        });
    }

    private void writeMetricAggregation(@Nonnull final AggregatorConnection connection, @Nonnull final Messages.AggregationRecord record,
                                        @Nonnull final Metric metric, @Nonnull final String period, @Nonnull final String statistic)
    {
        EventBus eb = vertx.eventBus();
        String key = getServerKeyForMetric(metric) + "." + period + "." + statistic + "." + connection.getHostName().or("unknown");
        RedisInstance metricRedis = getRedisInstanceFor(key);
        if (metricRedis == null) {
            final String msg = "unable to add period to metric " + metric + ".  No redis instance found for key.";
            LOGGER.warn(msg);
            return;
        }
        Map<String, String> values = Maps.newHashMap();
        values.put("value", String.valueOf(record.getStatisticValue()));
        values.put("periodStart", record.getPeriodStart());
        StringBuilder samplesBuilder = new StringBuilder();
        samplesBuilder.append("[");
        Joiner.on(", ").appendTo(samplesBuilder, record.getStatisticSamplesList()).append("]");
        String samples = samplesBuilder.toString();
        values.put("samples", samples);
        RedisUtils.hmset(metricRedis, key, values, eb, null);
    }

    @Nullable
    private RedisInstance getRedisInstanceFor(@Nonnull final String key) {
        final KetamaRing.NodeEntry entry = _ketamaRing.hash(key, KetamaLayers.REDIS.getVal());
        return entry.getMappedObject();
    }

    private void updateLastSeen(@Nonnull final String hostName, @Nonnull final RedisInstance hostRedis) {
        final EventBus eb = vertx.eventBus();
        RedisUtils.set(hostRedis, getHostLastSeenKey(hostName), Long.toString(DateTime.now().getMillis()), eb, null);
    }

    private void getHostLastSeen(@Nonnull final String hostName, @Nonnull final RedisInstance hostRedis,
                                 @Nullable final AsyncResultHandler<DateTime> handler)
    {
        RedisUtils.get(hostRedis, getHostLastSeenKey(hostName), vertx.eventBus(), new AsyncResultHandler<String>() {
            @Override
            public void handle(@Nonnull final AsyncResult<String> event) {
                if (handler == null) {
                    return;
                }
                if (event.succeeded()) {
                    handler.handle(new ASResult<DateTime>(new DateTime(Long.valueOf(event.result()))));
                } else {
                    handler.handle(new ASResult<DateTime>(event.cause()));
                }
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

    private void aggregationRecordArrived(@Nonnull final AggregatorConnection connection,
                                          @Nonnull final Messages.AggregationRecord record)
    {
        //We must have a cluster name and a host name to process the metric
        if (!(connection.getClusterName().isPresent() && connection.getHostName().isPresent())) {
            LOGGER.warn("Unable to process aggregation record, unknown cluster or host on connection " + connection);
            return;
        }
        if (!record.hasMetric() || record.getMetric() == null) {
            LOGGER.warn("Unable to process aggregation record, metric name is null. record: " + record);
            return;
        }
        if (!record.hasPeriod() || record.getPeriod() == null) {
            LOGGER.warn("Unable to process aggregation record, period is null. record: " + record);
            return;
        }
        if (!record.hasStatistic() || record.getStatistic() == null) {
            LOGGER.warn("Unable to process aggregation record, statistic is null. record: " + record);
            return;
        }

        createMetric(record.getService(), record.getMetric(), connection.getClusterName().get(), record.getStatistic(), record.getPeriod(),
                new AsyncResultHandler<Metric>() {
                    @Override
                    public void handle(@Nonnull final AsyncResult<Metric> event) {
                        if (event.succeeded()) {
                            final String period = record.getPeriod();
                            final String statistic = record.getStatistic();
                            updateMetricMetadata(event.result(), period, statistic);
                            writeMetricAggregation(connection, record, event.result(), period, statistic);
                        }
                    }
                });
    }

    private void createMetric(final String serviceName, final String metricName, final String clusterName, final String statistic,
                              final String period, @Nullable final AsyncResultHandler<Metric> callback)
    {
        ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>> services = _registeredMetrics.get(clusterName);
        if (services == null) {
            ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>> newServices =
                    new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Metric>>();
            services = _registeredMetrics.putIfAbsent(clusterName, newServices);
            if (services == null) {
                services = newServices;
            }
        }

        ConcurrentSkipListMap<String, Metric> metrics = services.get(serviceName);
        if (metrics == null) {
            ConcurrentSkipListMap<String, Metric> newMetrics = new ConcurrentSkipListMap<String, Metric>();
            metrics = services.putIfAbsent(serviceName, newMetrics);
            if (metrics == null) {
                metrics = newMetrics;
            }
        }


        if (!metrics.containsKey(metricName)) {
            registerMetric(clusterName, serviceName, metricName, new AsyncResultHandler<Metric>() {
                @Override
                public void handle(@Nonnull final AsyncResult<Metric> event) {
                    if (event.succeeded()) {
                        final Metric metric = event.result();
                        if (callback != null) {
                            callback.handle(new ASResult<Metric>(metric));
                        }
                        updateMetricMetadata(metric, period, statistic);
                    } else {
                        if (callback != null) {
                            callback.handle(new ASResult<Metric>(event.cause()));
                        }
                    }
                }
            });
        } else {
            final Metric metric = metrics.get(metricName);
            if (callback != null) {
                callback.handle(new ASResult<Metric>(metric));
            }
        }
    }

    private void updateMetricMetadata(@Nonnull final Metric metric, @Nonnull final String period, @Nonnull final String statistic) {
        if (!metric.hasAggregation(statistic)) {
            registerAggregationForMetric(metric, statistic, new AsyncResultHandler<String>() {
                @Override
                public void handle(@Nonnull final AsyncResult<String> event) {
                    if (event.succeeded()) {
                        metric.addAggregation(statistic);
                    }
                }
            });
        }

        if (!metric.hasPeriod(period)) {
            registerPeriodForMetric(metric, period, new AsyncResultHandler<String>() {
                @Override
                public void handle(@Nonnull final AsyncResult<String> event) {
                    if (event.succeeded()) {
                        metric.addPeriod(period);
                    }
                }
            });
        }
    }
}
