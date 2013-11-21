package com.arpnetworking.tsdaggregator.aggserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Description goes here
 *
 * @author barp
 */
public class RedisUtils {
    private static Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);

    static void getRedisSetEntries(final RedisInstance redisInstance, final String key,
                                   final AsyncResultHandler<List<String>> replyHandler, final EventBus bus) {
        final JsonObject addServer = new JsonObject().putString("command", "SMEMBERS")
                .putArray("args", new JsonArray().add(key));
        bus.send(redisInstance.getEBName(), addServer,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> event) {
                        if (replyHandler == null) {
                            return;
                        }
                        JsonObject json = event.body();
                        final ASResult<List<String>> result;
                        if (json.getString("status").equals("ok")) {
                            final JsonArray array = json.getArray("value");
                            List<String> list = new ArrayList<>();
                            for (Object o : array) {
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

    static void addEntryToRedisSet(final RedisInstance redisInstance, final String key, final String value,
                                   final AsyncResultHandler<Integer> replyHandler, final EventBus bus) {
        final JsonObject addServer = new JsonObject().putString("command", "SADD")
                .putArray("args", new JsonArray().add(key).add(value));
        bus.send(redisInstance.getEBName(), addServer,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> event) {
                        if (replyHandler == null) {
                            return;
                        }
                        JsonObject json = event.body();
                        final ASResult<Integer> result;
                        if (json.getString("status").equals("ok")) {
                            result = new ASResult<>(json.getInteger("value"));
                        } else {
                            result = new ASResult<>(new Exception(json.getString("message").toString()));
                        }
                        replyHandler.handle(result);
                    }
                });

    }
}
