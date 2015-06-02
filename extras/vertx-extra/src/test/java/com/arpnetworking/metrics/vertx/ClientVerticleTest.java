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

import com.arpnetworking.metrics.impl.TsdEvent;
import com.arpnetworking.metrics.vertx.test.TestClientVerticleImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

/**
 * Integration tests for client verticle writing to sinks through <code>EventBusSink</code> and <code>SinkVerticle</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class ClientVerticleTest extends TestVerticle {

    @Override
    public void start() {
        initialize();
        startTests();
    }

    @Test
    public void testClientVerticlePublishesToDefaultAddress() throws JsonProcessingException {
        // initMocks() need to be called from here because @Before annotations are not supported by the test framework yet.
        // Source: http://vertx.io/dev_guide.html#java-integration-tests
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(_handler).handle(Matchers.<Message<String>>any());
        final String expectedData = OBJECT_MAPPER.writeValueAsString(
                new TsdEvent.Builder()
                        .setAnnotations(TestClientVerticleImpl.ANNOTATIONS)
                        .setCounterSamples(TestClientVerticleImpl.COUNTER_SAMPLES)
                        .setTimerSamples(TestClientVerticleImpl.TIMER_SAMPLES)
                        .setGaugeSamples(TestClientVerticleImpl.GAUGE_SAMPLES)
                        .build());

        vertx.eventBus().registerHandler(DEFAULT_SINK_ADDRESS, _handler);
        container.deployVerticle(
                TARGET_CLIENT_VERTICLE_NAME,
                asyncResult -> {
                    VertxAssert.assertTrue(asyncResult.succeeded());
                    Mockito.verify(_handler).handle(_argumentCaptor.capture());
                    VertxAssert.assertEquals(expectedData, _argumentCaptor.getValue().body());
                    VertxAssert.testComplete();
                }
        );
    }

    @Mock
    private Handler<Message<String>> _handler;

    @Captor
    private ArgumentCaptor<Message<String>> _argumentCaptor;

    private static final String DEFAULT_SINK_ADDRESS = "metrics.sink.default";
    private static final String TARGET_CLIENT_VERTICLE_NAME = TestClientVerticleImpl.class.getCanonicalName();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
