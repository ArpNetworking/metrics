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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the <code>DomainInjectingSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DomainInjectingSinkTest {

    @Before()
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInjectNoSuffixes() {
        final DomainInjectingSink sink = new DomainInjectingSink.Builder()
                .setName("testInjectNoSuffixes")
                .setSink(_target)
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicData();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(dataIn);
    }

    @Test
    public void testInjectMismatchedSuffix() {
        final DomainInjectingSink sink = new DomainInjectingSink.Builder()
                .setName("testInjectMismatchedSuffix")
                .setSink(_target)
                .setSuffixes(ImmutableSet.of("mycolo"))
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("host", "foo.com"))
                .build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("foo.com", _dataOut.getValue().getDimensions().get("host"));
        Assert.assertFalse(_dataOut.getValue().getDimensions().containsKey("domain"));
    }

    @Test
    public void testInjectMatchedSuffix() {
        final DomainInjectingSink sink = new DomainInjectingSink.Builder()
                .setName("testInjectMatchedSuffix")
                .setSink(_target)
                .setSuffixes(ImmutableSet.of("com"))
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("host", "foo.com"))
                .build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("foo.com", _dataOut.getValue().getDimensions().get("host"));
        Assert.assertEquals("com", _dataOut.getValue().getDimensions().get("domain"));
    }

    @Test
    public void testInjectLongestMatchedSuffix() {
        final DomainInjectingSink sink = new DomainInjectingSink.Builder()
                .setName("testInjectLongestMatchedSuffix")
                .setSink(_target)
                .setSuffixes(ImmutableSet.of("com", "example.com"))
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of("host", "foo.example.com"))
                .build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("foo.example.com", _dataOut.getValue().getDimensions().get("host"));
        Assert.assertEquals("example.com", _dataOut.getValue().getDimensions().get("domain"));
    }


    @Test
    public void testDomainAlreadyDefined() {
        final DomainInjectingSink sink = new DomainInjectingSink.Builder()
                .setName("testDomainAlreadyDefined")
                .setSink(_target)
                .setSuffixes(ImmutableSet.of("com"))
                .build();
        final PeriodicData dataIn = TestBeanFactory.createPeriodicDataBuilder()
                .setDimensions(ImmutableMap.of(
                        "host", "foo.example.com",
                        "domain", "example.com"))
                .build();
        sink.recordAggregateData(dataIn);
        Mockito.verify(_target).recordAggregateData(_dataOut.capture());
        Assert.assertEquals("foo.example.com", _dataOut.getValue().getDimensions().get("host"));
        Assert.assertEquals("example.com", _dataOut.getValue().getDimensions().get("domain"));
    }

    @Mock
    private Sink _target;
    @Captor
    private ArgumentCaptor<PeriodicData> _dataOut;
}
