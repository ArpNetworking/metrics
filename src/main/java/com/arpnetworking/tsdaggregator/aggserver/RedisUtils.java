package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utilities for interacting with redis over the vertx eventbus.
 *
 * @author barp
 */
public class RedisUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);

    static void getAllSMEMBERS(final Set<RedisInstance> instances, final String key, final EventBus bus,
                               final AsyncResultHandler<Set<String>> replyHandler) {
        RedisInstance[] redisInstances = instances.toArray(new RedisInstance[instances.size()]);
        LOGGER.debug("calling smembers " + key + " for redis cluster " + Arrays.toString(redisInstances));
        final AtomicInteger latch = new AtomicInteger(redisInstances.length);
        final Set<String> collected = new ConcurrentSkipListSet<>();
        final AtomicReference<Throwable> failed = new AtomicReference<>(null);
        for (RedisInstance redis : redisInstances) {
            smembers(redis, key, new AsyncResultHandler<Set<String>>() {
                @Override
                public void handle(final AsyncResult<Set<String>> event) {
                    if (event.failed()) {
                        LOGGER.debug("failed, setting failure message");
                        failed.set(event.cause());
                    } else {
                        LOGGER.debug("success");
                        collected.addAll(event.result());
                    }
                    int val = latch.decrementAndGet();
                    LOGGER.debug("waiting for " + val + " more returns");
                    if (val == 0) {
                        if (replyHandler != null) {
                            if (failed.get() == null) {
                                ASResult<Set<String>> result = new ASResult<>(collected);
                                replyHandler.handle(result);
                            } else {
                                ASResult<Set<String>> result = new ASResult<>(failed.get());
                                replyHandler.handle(result);
                            }
                        }
                    }
                }
            }, bus);
        }
    }

    static void smembers(@Nonnull final RedisInstance redisInstance, final String key,
                         @Nullable final AsyncResultHandler<Set<String>> replyHandler, @Nonnull final EventBus bus) {
        LOGGER.debug("calling smembers " + key + " on redis " + redisInstance);
        final JsonObject addServer =
                new JsonObject().putString("command", "SMEMBERS").putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                if (replyHandler == null) {
                    return;
                }
                JsonObject json = event.body();
                @Nonnull final ASResult<Set<String>> result;
                if (json.getString("status").equals("ok")) {
                    final JsonArray array = json.getArray("value");
                    @Nonnull Set<String> list = new HashSet<>();
                    for (@Nonnull Object o : array) {
                        list.add(o.toString());
                    }
                    result = new ASResult<>(list);
                } else {
                    result = new ASResult<>(new Exception(json.getString("message")));
                }
                replyHandler.handle(result);
            }
        });
    }

    static void sadd(@Nonnull final RedisInstance redisInstance, final String key, final String value,
                     @Nullable final AsyncResultHandler<Integer> replyHandler, @Nonnull final EventBus bus) {
        LOGGER.debug("calling sadd " + key + " " + value + " on redis " + redisInstance);
        final JsonObject addServer =
                new JsonObject().putString("command", "SADD").putArray("args", new JsonArray().add(key).add(value));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                if (replyHandler == null) {
                    return;
                }
                JsonObject json = event.body();
                @Nonnull final ASResult<Integer> result;
                if (json.getString("status").equals("ok")) {
                    result = new ASResult<>(json.getInteger("value"));
                } else {
                    result = new ASResult<>(new Exception(json.getString("message")));
                }
                replyHandler.handle(result);
            }
        });

    }

    static void set(@Nonnull final RedisInstance redisInstance, final String key, final String value,
                    @Nullable final AsyncResultHandler<Void> replyHandler, @Nonnull final EventBus bus) {
        LOGGER.debug("calling set " + key + " " + value + " on redis " + redisInstance);
        final JsonObject addServer =
                new JsonObject().putString("command", "SET").putArray("args", new JsonArray().add(key).add(value));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                if (replyHandler == null) {
                    return;
                }
                JsonObject json = event.body();
                @Nonnull final ASResult<Void> result;
                if (json.getString("status").equals("ok")) {
                    result = new ASResult<>((Void) null);
                } else {
                    result = new ASResult<>(new Exception(json.getString("message")));
                }
                replyHandler.handle(result);
            }
        });

    }

    static void hgetall(@Nonnull final RedisInstance redisInstance, final String key,
                        @Nullable final AsyncResultHandler<Map<String, String>> replyHandler,
                        @Nonnull final EventBus bus) {
        LOGGER.debug("calling hgetall " + key + " on redis " + redisInstance);
        final JsonObject addServer =
                new JsonObject().putString("command", "HGETALL").putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                if (replyHandler == null) {
                    return;
                }
                JsonObject json = event.body();
                @Nonnull final ASResult<Map<String, String>> result;
                if (json.getString("status").equals("ok")) {
                    Map<String, String> map = Maps.newHashMap();
                    JsonObject obj = json.getObject("value");
                    for (String field : obj.getFieldNames()) {
                        String value = obj.getString(field);
                        value = value.substring(1, value.length() - 1);
                        map.put(field, value);
                    }
                    result = new ASResult<>(map);
                } else {
                    result = new ASResult<>(new Exception(json.getString("message")));
                }
                replyHandler.handle(result);
            }
        });
    }

    static void hmset(@Nonnull final RedisInstance redisInstance, @Nonnull final String key,
                      @Nonnull final Map<String, String> values, @Nonnull EventBus bus,
                      @Nullable final AsyncResultHandler<Void> replyHandler) {
        LOGGER.debug("calling hmset " + key + " " + values.toString() + " on redis " + redisInstance);
        final JsonArray args = new JsonArray().add(key);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            args.add(entry.getKey()).add("\"" + entry.getValue() + "\"");
        }
        final JsonObject hmsetRequest = new JsonObject().putString("command", "HMSET").putArray("args", args);
        bus.send(redisInstance.getEBName(), hmsetRequest, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(@Nonnull final Message<JsonObject> event) {
                LOGGER.debug("data = " + event.body());
                if (replyHandler == null) {
                    return;
                }
                JsonObject json = event.body();
                final ASResult<Void> result;
                if (json.getString("status").equals("ok")) {
                    result = new ASResult<Void>((Void)null);
                } else {
                    result = new ASResult<Void>(new Exception(json.getString("message")));
                }
                replyHandler.handle(result);
            }
        });
    }
}
