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
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the <code>DimensionFilteringSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DimensionFilteringSinkTest {

    @Before()
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
     public void testInjectNoExclusions() {
        final DimensionFilteringSink sink = new DimensionFilteringSink.Builder()
                .setName("testInjectNoExclusions")
                .setSink(_target)
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicData();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(dataIn);
    }

    @Test
    public void testInjectExcludeWith() {
        final DimensionFilteringSink sink = new DimensionFilteringSink.Builder()
                .setName("testInjectExcludeWith")
                .setSink(_target)
                .setExcludeWithDimensions(ImmutableSet.of("foo"))
                .build();
        final PeriodicData dataExcluded = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("foo", "bar"))
                .build();
        sink.recordAggregateData(dataExcluded);
        Mockito.verify(_target, Mockito.never()).recordAggregateData(Mockito.any(PeriodicData.class));
        final PeriodicData dataIncluded = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("bar", "foo"))
                .build();
        sink.recordAggregateData(dataIncluded);
        Mockito.verify(_target).recordAggregateData(dataIncluded);
    }

    @Test
    public void testInjectExcludeWithout() {
        final DimensionFilteringSink sink = new DimensionFilteringSink.Builder()
                .setName("testInjectExcludeWithout")
                .setSink(_target)
                .setExcludeWithoutDimensions(ImmutableSet.of("foo"))
                .build();
        final PeriodicData dataExcluded = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("bar", "foo"))
                .build();
        sink.recordAggregateData(dataExcluded);
        Mockito.verify(_target, Mockito.never()).recordAggregateData(Mockito.any(PeriodicData.class));
        final PeriodicData dataIncluded = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("foo", "bar"))
                .build();
        sink.recordAggregateData(dataIncluded);
        Mockito.verify(_target).recordAggregateData(dataIncluded);
    }

    @Mock
    private Sink _target;
}
