/**
 * Copyright 2014 Brandon Arp
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
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>MultiSink</code> class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class MultiSinkTest {

    @Before
    public void before() {
        _multiSinkBuilder = new MultiSink.Builder()
                .setName("multi_sink_test");
    }

    @Test
    public void testClose() {
        final Sink mockSinkA = Mockito.mock(Sink.class, "mockSinkA");
        final Sink mockSinkB = Mockito.mock(Sink.class, "mockSinkB");
        final Sink multiSink = _multiSinkBuilder
                .setSinks(Lists.newArrayList(mockSinkA, mockSinkB))
                .build();
        multiSink.close();
        Mockito.verify(mockSinkA).close();
        Mockito.verify(mockSinkB).close();
    }

    @Test
    public void testRecordProcessedAggregateData() {
        final List<AggregatedData> data = Collections.singletonList(TestBeanFactory.createAggregatedData());
        final Sink mockSinkA = Mockito.mock(Sink.class, "mockSinkA");
        final Sink mockSinkB = Mockito.mock(Sink.class, "mockSinkB");
        final Sink multiSink = _multiSinkBuilder
                .setSinks(Lists.newArrayList(mockSinkA, mockSinkB))
                .build();
        multiSink.recordAggregateData(data);
        Mockito.verify(mockSinkA).recordAggregateData(Matchers.eq(data));
        Mockito.verify(mockSinkB).recordAggregateData(Matchers.eq(data));
    }

    private MultiSink.Builder _multiSinkBuilder;
}
