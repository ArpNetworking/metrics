/**
 * Copyright 2016 Groupon.com
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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the <code>DimensionInjectingSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DimensionInjectingSinkTest {

    @Before()
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInject() {
        final DimensionInjectingSink sink = new DimensionInjectingSink.Builder()
                .setName("testInject")
                .setDimensions(ImmutableMap.of(
                        "Foo", "Bar"))
                .setSink(_target)
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder().build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("Bar", _dataOut.getValue().getDimensions().get("Foo"));
    }

    @Test
    public void testInjectNone() {
        final DimensionInjectingSink sink = new DimensionInjectingSink.Builder()
                .setName("testInject")
                .setSink(_target)
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicData();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(dataIn);
    }

    @Test
    public void testInjectOverride() {
        final DimensionInjectingSink sink = new DimensionInjectingSink.Builder()
                .setName("testInjectOverride")
                .setDimensions(ImmutableMap.of(
                        "Foo", "Bar"))
                .setSink(_target)
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of(
                        "Foo", "None"))
                .build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("Bar", _dataOut.getValue().getDimensions().get("Foo"));
    }

    @Mock
    private Sink _target;
    @Captor
    private ArgumentCaptor<PeriodicData> _dataOut;
}
