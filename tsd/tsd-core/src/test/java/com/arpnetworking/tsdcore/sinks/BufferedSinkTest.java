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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>BufferedSink</code> class. The <code>doAnswer</code> form
 * of Mockito assertion is used to inspect the buffer state before it is cleared
 * since expectations on arguments are not deep copies.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class BufferedSinkTest {

    @Before
    public void before() {
        _sinkBuilder = new BufferedSink.Builder()
                .setName("buffered_sink_test");
    }

    @Test
    public void testClose() {
        final Sink mockSink = Mockito.mock(Sink.class);
        final Sink bufferedSink = _sinkBuilder.setSink(mockSink).build();
        bufferedSink.close();
        Mockito.verify(mockSink, Mockito.never()).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        Mockito.verify(mockSink).close();
    }

    @Test
    public void testFlushOnClose() {
        final Sink mockSink = Mockito.mock(Sink.class);
        final Sink bufferedSink = _sinkBuilder.setSink(mockSink).build();
        final List<AggregatedData> data = Collections.singletonList(TestBeanFactory.createAggregatedData());
        bufferedSink.recordAggregateData(data);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Assert.assertEquals(data, invocation.getArguments()[0]);
                return null;
            }
        }).when(mockSink).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        bufferedSink.close();
        Mockito.verify(mockSink).close();
    }

    @Test
    public void testFullFlushOnRecordProcessedAggregateData() {
        final Sink mockSink = Mockito.mock(Sink.class);
        final Sink bufferedSink = _sinkBuilder.setBufferSize(Integer.valueOf(1)).setSink(mockSink).build();
        final List<AggregatedData> data1 = Collections.singletonList(TestBeanFactory.createAggregatedData());
        final List<AggregatedData> data2 = Collections.singletonList(TestBeanFactory.createAggregatedData());

        bufferedSink.recordAggregateData(data1);
        Mockito.verify(mockSink, Mockito.never()).recordAggregateData(Matchers.anyListOf(AggregatedData.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Assert.assertEquals(data1, invocation.getArguments()[0]);
                return null;
            }
        }).when(mockSink).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        bufferedSink.recordAggregateData(data2);

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Assert.assertEquals(data2, invocation.getArguments()[0]);
                return null;
            }
        }).when(mockSink).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        bufferedSink.close();
        Mockito.verify(mockSink).close();
    }

    @Test
    public void testIncrementalFlushOnRecordProcessedAggregateData() {
        final Sink mockSink = Mockito.mock(Sink.class);
        final Sink bufferedSink = _sinkBuilder.setBufferSize(Integer.valueOf(1)).setSink(mockSink).build();
        final AggregatedData datumA = TestBeanFactory.createAggregatedData();
        final AggregatedData datumB = TestBeanFactory.createAggregatedData();

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Assert.assertEquals(Collections.singletonList(datumA), invocation.getArguments()[0]);
                return null;
            }
        }).when(mockSink).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        bufferedSink.recordAggregateData(Lists.newArrayList(datumA, datumB));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Assert.assertEquals(Collections.singletonList(datumB), invocation.getArguments()[0]);
                return null;
            }
        }).when(mockSink).recordAggregateData(Matchers.anyListOf(AggregatedData.class));
        bufferedSink.close();
        Mockito.verify(mockSink).close();
    }

    private BufferedSink.Builder _sinkBuilder;
}
