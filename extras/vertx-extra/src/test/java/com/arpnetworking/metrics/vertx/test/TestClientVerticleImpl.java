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
package com.arpnetworking.metrics.vertx.test;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.vertx.EventBusSink;
import com.arpnetworking.metrics.vertx.SinkVerticle;
import com.google.common.collect.ImmutableMap;
import org.vertx.java.platform.Verticle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test verticle to integrate with the <code>EventBusSink</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class TestClientVerticleImpl extends Verticle {

    @Override
    public void start() {
        final Sink sink = new EventBusSink.Builder()
                .setEventBus(vertx.eventBus())
                .build();
        sink.record(ANNOTATIONS, TIMER_SAMPLES, COUNTER_SAMPLES, GAUGE_SAMPLES);
    }

    /**
     * Static annotations map.
     */
    public static final Map<String, String> ANNOTATIONS = ImmutableMap.of("someAnnotationKey", "someAnnotationValue");
    /**
     * Static timer samples map.
     */
    public static final Map<String, List<Quantity>> TIMER_SAMPLES = ImmutableMap.of(
            "timerSamples",
            Arrays.asList(
                    SinkVerticle.DefaultQuantity.newInstance(100, Unit.MEGABYTE),
                    SinkVerticle.DefaultQuantity.newInstance(40, Unit.GIGABYTE)));
    /**
     * Static counter samples map.
     */
    public static final Map<String, List<Quantity>> COUNTER_SAMPLES = ImmutableMap.of(
            "counterSamples",
            Arrays.asList(
                    SinkVerticle.DefaultQuantity.newInstance(400, Unit.MILLISECOND)));
    /**
     * Static gauge samples map.
     */
    public static final Map<String, List<Quantity>> GAUGE_SAMPLES = ImmutableMap.of(
            "gaugeSamples",
            Arrays.asList(
                    SinkVerticle.DefaultQuantity.newInstance(1000, Unit.MILLISECOND),
                    SinkVerticle.DefaultQuantity.newInstance(5, Unit.MINUTE)));
}
