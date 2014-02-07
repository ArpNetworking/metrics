package com.arpnetworking.tsdaggregator.aggserver;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utilities for interacting with redis over the vertx eventbus.
 *
 * @author barp
 */
public class RedisUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);

    static Promise<Set<String>> getAllSMEMBERS(final Set<RedisInstance> instances, final String key, final EventBus bus) {
        final Promise<Set<String>> promise = Promise.defer();
        RedisInstance[] redisInstances = instances.toArray(new RedisInstance[instances.size()]);
        LOGGER.debug("calling smembers " + key + " for redis cluster " + Arrays.toString(redisInstances));
        final AtomicInteger latch = new AtomicInteger(redisInstances.length);
        final Set<String> collected = new ConcurrentSkipListSet<String>();
        for (RedisInstance redis : redisInstances) {
            smembers(redis, key, bus).then(new PromiseAction<Set<String>>() {
                @Override
                public void call(final Set<String> strings) {
                    collected.addAll(strings);
                    int val = latch.decrementAndGet();
                    LOGGER.debug("success");
                    LOGGER.debug("waiting for " + val + " more returns");
                    if (val == 0) {
                        promise.fulfill(collected);
                    }
                }
            })
                .fail(new PromiseAction<Exception>() {
                    @Override
                    public void call(final Exception e) {
                        LOGGER.debug("failed, setting failure message");
                        promise.reject(e);
                    }
                });
        }
        return promise;
    }

    static Promise<Set<String>> smembers(@Nonnull final RedisInstance redisInstance, final String key, @Nonnull final EventBus bus) {
        final Promise<Set<String>> promise = Promise.defer();
        LOGGER.debug("calling smembers " + key + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "SMEMBERS")
            .putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    final JsonArray array = json.getArray("value");
                    @Nonnull Set<String> list = Sets.newHashSet();
                    for (@Nonnull Object o : array) {
                        list.add(o.toString());
                    }
                    promise.fulfill(list);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<Integer> sadd(@Nonnull final RedisInstance redisInstance, final String key, final String value,
                                 @Nonnull final EventBus bus) {
        final Promise<Integer> promise = Promise.defer();
        LOGGER.debug("calling sadd " + key + " " + value + " on redis " + redisInstance);

        final JsonObject addServer = new JsonObject().putString("command", "SADD")
            .putArray("args",
                new JsonArray().add(key)
                    .add(value));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    promise.fulfill(json.getInteger("value"));
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<Void> set(@Nonnull final RedisInstance redisInstance, final String key, final String value,
                             @Nonnull final EventBus bus) {
        final Promise<Void> promise = Promise.defer();
        LOGGER.debug("calling set " + key + " " + value + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "SET")
            .putArray("args",
                new JsonArray().add(key)
                    .add(value));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    promise.fulfill(null);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<Map<String, String>> hgetall(@Nonnull final RedisInstance redisInstance, final String key, @Nonnull final EventBus bus) {
        final Promise<Map<String, String>> promise = Promise.defer();
        LOGGER.debug("calling hgetall " + key + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "HGETALL")
            .putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    Map<String, String> map = Maps.newHashMap();
                    JsonObject obj = json.getObject("value");
                    for (String field : obj.getFieldNames()) {
                        String value = obj.getString(field);
                        value = value.substring(1, value.length() - 1);
                        map.put(field, value);
                    }
                    promise.fulfill(map);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }


    static Promise<String> get(@Nonnull final RedisInstance redisInstance, final String key, @Nonnull final EventBus bus) {
        final Promise<String> promise = Promise.defer();
        LOGGER.debug("calling get " + key + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "GET")
            .putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    String val = json.getString("value");
                    promise.fulfill(val);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<Void> hmset(@Nonnull final RedisInstance redisInstance, @Nonnull final String key,
                               @Nonnull final Map<String, String> values, @Nonnull EventBus bus) {
        final Promise<Void> promise = Promise.defer();
        LOGGER.debug("calling hmset " + key + " " + values.toString() + " on redis " + redisInstance);
        final JsonArray args = new JsonArray().add(key);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            args.add(entry.getKey())
                .add("\"" + entry.getValue() + "\"");
        }
        final JsonObject hmsetRequest = new JsonObject().putString("command", "HMSET")
            .putArray("args", args);
        bus.send(redisInstance.getEBName(), hmsetRequest, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    promise.fulfill(null);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<Integer> zadd(@Nonnull final RedisInstance redisInstance, @Nonnull final String key, final int score,
                                 @Nullable final String value, @Nonnull final EventBus bus) {
        final Promise<Integer> promise = Promise.defer();
        LOGGER.debug("calling zadd " + key + " " + score + " " + value + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "ZADD")
            .putArray("args",
                new JsonArray().add(key)
                    .add(score)
                    .add(value));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    promise.fulfill(json.getInteger("value"));
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }

    static Promise<List<String>> zrange(@Nonnull final RedisInstance redisInstance, @Nonnull final String key, final int startIndex,
                                        final int endIndex, @Nonnull final EventBus bus) {
        final Promise<List<String>> promise = Promise.defer();
        LOGGER.debug("calling zrange " + key + " " + startIndex + " " + endIndex + " on redis " + redisInstance);
        final JsonObject addServer = new JsonObject().putString("command", "ZRANGE")
            .putArray("args",
                new JsonArray().add(key)
                    .add(startIndex)
                    .add(endIndex));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                JsonObject json = event.body();
                if (json.getString("status")
                    .equals("ok")) {
                    List<String> list = Lists.newArrayList();
                    JsonArray obj = json.getArray("value");
                    for (Object o : obj) {
                        String value = o.toString();
                        list.add(value);
                    }
                    promise.fulfill(list);
                } else {
                    promise.reject(new Exception(json.getString("message")));
                }
            }
        });
        return promise;
    }
}
