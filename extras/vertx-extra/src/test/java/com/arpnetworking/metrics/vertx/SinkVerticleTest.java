/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.metrics.vertx;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.impl.TsdEvent;
import com.arpnetworking.metrics.vertx.test.TestSinkVerticleImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests <code>SinkVerticle</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class SinkVerticleTest extends TestVerticle {

    @Override
    public void start() {
        initialize();
        container.deployWorkerVerticle(
                TARGET_WORKER_VERTICLE_NAME,
                new JsonObject(Collections.singletonMap("sinkAddress", DEFAULT_SINK_ADDRESS)),
                1,
                false,
                asyncResultWorker -> {
                    VertxAssert.assertTrue(asyncResultWorker.succeeded());
                    // If deployed correctly then start the tests
                    startTests();
                }
        );
    }

    @Test
    public void testValidMessageSentOnEB() throws JsonProcessingException {
        final Map<String, String> annotationMap = ImmutableMap.of("someAnnotationKey", "someAnnotationValue");
        final Map<String, List<Quantity>> timerSampleMap = ImmutableMap.of(
                "timerSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(100, Unit.MEGABYTE),
                        SinkVerticle.DefaultQuantity.newInstance(40, Unit.GIGABYTE)));
        final Map<String, List<Quantity>> counterSampleMap = ImmutableMap.of(
                "counterSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(400, Unit.MILLISECOND)));
        final Map<String, List<Quantity>> gaugeSampleMap = ImmutableMap.of(
                "gaugeSamples",
                Arrays.asList(
                        SinkVerticle.DefaultQuantity.newInstance(1000, Unit.MILLISECOND),
                        SinkVerticle.DefaultQuantity.newInstance(5, Unit.MINUTE)));
        final String data = OBJECT_MAPPER.writeValueAsString(
                new TsdEvent.Builder()
                        .setAnnotations(annotationMap)
                        .setTimerSamples(timerSampleMap)
                        .setCounterSamples(counterSampleMap)
                        .setGaugeSamples(gaugeSampleMap)
                        .build());
        vertx.eventBus().send(
                DEFAULT_SINK_ADDRESS,
                data,
                (Message<String> reply) -> {
                    VertxAssert.assertEquals(data, reply.body());
                    VertxAssert.testComplete();
                });
    }

    @Test
    public void testInvalidMessageSentOnEB() throws JsonProcessingException {
        final Map<String, Object> dataMap = ImmutableMap.of("someKey", "someValue");
        vertx.eventBus().send(
            DEFAULT_SINK_ADDRESS,
            OBJECT_MAPPER.writeValueAsString(dataMap),
            (Message<String> reply) -> {
                VertxAssert.assertNull(reply);
                VertxAssert.testComplete();
            });
        VertxAssert.testComplete();
    }

    private static final String TARGET_WORKER_VERTICLE_NAME = TestSinkVerticleImpl.class.getCanonicalName();
    private static final String DEFAULT_SINK_ADDRESS = "defaultSinkAddress";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
