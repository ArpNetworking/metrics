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
import com.google.common.base.Charsets;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>ConsoleSink</code> class. These tests are not isolated
 * from other tests that rely on standard output.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConsoleSinkTest {

    @Before
    public void before() throws UnsupportedEncodingException {
        _byteArrayStream = new ByteArrayOutputStream();
        _outputStream = new PrintStream(_byteArrayStream, true, Charsets.UTF_8.name());
        _consoleSinkBuilder = new ConsoleSink.Builder()
                .setName("console_sink_test");
    }

    @Test
    public void testConstructor() {
        _consoleSinkBuilder.build();
    }

    @Test
    public void testClose() {
        final Sink consoleSink = new ConsoleSink(_consoleSinkBuilder, _outputStream);
        final String beforeClose = _byteArrayStream.toString();
        consoleSink.close();
        final String afterClose = _byteArrayStream.toString();
        Assert.assertEquals(beforeClose, afterClose);
    }

    @Test
    public void testRecordProcessedAggregateData() {
        final AggregatedData datum = TestBeanFactory.createAggregatedData();
        final List<AggregatedData> data = Collections.singletonList(datum);
        final Sink consoleSink = new ConsoleSink(_consoleSinkBuilder, _outputStream);
        consoleSink.recordAggregateData(data);

        final String output = _byteArrayStream.toString();
        Assert.assertThat(output, Matchers.containsString(datum.getHost()));
        Assert.assertThat(output, Matchers.containsString(datum.getFQDSN().getService()));
        Assert.assertThat(output, Matchers.containsString(datum.getFQDSN().getMetric()));
        Assert.assertThat(output, Matchers.containsString(datum.getPeriodStart().toString()));
        Assert.assertThat(output, Matchers.containsString(datum.getPeriod().toString()));
        Assert.assertThat(output, Matchers.containsString(datum.getFQDSN().getStatistic().getName()));
    }

    @Test
    public void testRecordProcessedAggregateDataEmpty() {
        final Sink consoleSink = _consoleSinkBuilder.build();
        consoleSink.recordAggregateData(Collections.<AggregatedData>emptyList());
        consoleSink.close();
        Assert.assertTrue(_byteArrayStream.toString().isEmpty());
    }

    private PrintStream _outputStream;
    private ByteArrayOutputStream _byteArrayStream;
    private ConsoleSink.Builder _consoleSinkBuilder;
}
