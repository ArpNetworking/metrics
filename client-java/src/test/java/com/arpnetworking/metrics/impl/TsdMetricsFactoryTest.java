/**
 * Copyright 2014 Groupon.com
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
package com.arpnetworking.metrics.impl;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.test.MockitoHelper;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests for <code>TsdMetricsFactory</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdMetricsFactoryTest {

    @Test
    public void testCreate() {
        final Sink sink1 = Mockito.mock(Sink.class, "TsdMetricsFactoryTest.testCreate.sink1");
        final Sink sink2 = Mockito.mock(Sink.class, "TsdMetricsFactoryTest.testCreate.sink2");
        final List<Sink> sinks = new ArrayList<>();
        sinks.add(sink1);
        sinks.add(sink2);
        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(sinks)
                .build();
        @SuppressWarnings("resource")
        final Metrics metrics = metricsFactory.create();
        Assert.assertNotNull(metrics);
        Assert.assertTrue(metrics instanceof TsdMetrics);
        metrics.close();
        Mockito.verify(sink1).record(
                org.mockito.Matchers.anyMapOf(String.class, String.class),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()));
        Mockito.verify(sink2).record(
                org.mockito.Matchers.anyMapOf(String.class, String.class),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()),
                MockitoHelper.<Map<String, List<Quantity>>>argThat(Matchers.anEmptyMap()));
    }

    @Test
    public void testCreateEmptySinks() {
        final List<Sink> sinks = new ArrayList<>();
        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(sinks)
                .build();
        @SuppressWarnings("resource")
        final Metrics metrics = metricsFactory.create();
        Assert.assertNotNull(metrics);
        Assert.assertTrue(metrics instanceof TsdMetrics);
        metrics.close();
    }

    @Test
    public void testCreateWithDeprecatedSink() {
        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder().build();
        final Metrics metrics = metricsFactory.create();
        Assert.assertNotNull(metrics);
        Assert.assertTrue(metrics instanceof TsdMetrics);

        // Delete the file since it is created in the project directory
        Assert.assertTrue(new File("./query.log").delete());
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullPath() {
        new TsdMetricsFactory.Builder()
                .setPath(null)
                .build();
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullExtension() {
        new TsdMetricsFactory.Builder()
                .setExtension(null)
                .build();
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullImmediateFlush() {
        new TsdMetricsFactory.Builder()
                .setImmediateFlush(null)
                .build();
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullName() {
        new TsdMetricsFactory.Builder()
                .setName(null)
                .build();
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderEmptyName() {
        new TsdMetricsFactory.Builder()
                .setName("")
                .build();
    }
}
