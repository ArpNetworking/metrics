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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>FileSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class FileSinkTest {

    @Before
    public void before() throws IOException {
        _outFile = File.createTempFile("file-sink-test", ".out");
        _fileSinkBuilder = new FileSink.Builder()
                .setName("file_sink_test")
                .setFileName(_outFile.getAbsolutePath());
    }

    @Test
    public void testRecordProcessedAggregateData() throws IOException {
        final AggregatedData datum = TestBeanFactory.createAggregatedData();
        final List<AggregatedData> data = Collections.singletonList(datum);
        final Sink fileSink = _fileSinkBuilder.build();
        fileSink.recordAggregateData(data);
        fileSink.close();

        final List<String> outLines = Files.readLines(_outFile, Charsets.UTF_8);
        Assert.assertEquals(1, outLines.size());
        final JsonNode jsonNode = OBJECT_MAPPER.readTree(Iterables.getOnlyElement(outLines));
        Assert.assertEquals(datum.getValue(), jsonNode.get("value").asDouble(), 0.001);
        Assert.assertEquals(datum.getMetric(), jsonNode.get("metric").asText());
        Assert.assertEquals(datum.getService(), jsonNode.get("service").asText());
        Assert.assertEquals(datum.getHost(), jsonNode.get("host").asText());
        Assert.assertEquals(datum.getPeriod(), Period.parse(jsonNode.get("period").asText()));
        Assert.assertEquals(datum.getPeriodStart().getMillis(), DateTime.parse(jsonNode.get("periodStart").asText()).getMillis());
        Assert.assertEquals(datum.getStatistic().getName(), jsonNode.get("statistic").asText());
    }

    @Test
    public void testRecordProcessedAggregateDataEmpty() throws IOException {
        final Sink fileSink = _fileSinkBuilder.build();
        fileSink.recordAggregateData(Collections.<AggregatedData>emptyList());
        fileSink.close();

        final List<String> outLines = Files.readLines(_outFile, Charsets.UTF_8);
        Assert.assertTrue(outLines.isEmpty());
    }

    @Test
    public void testRecordProcessedAggregateDataAfterClose() {
        final Sink fileSink = _fileSinkBuilder.build();
        fileSink.close();
        fileSink.recordAggregateData(Collections.singletonList(TestBeanFactory.createAggregatedData()));
    }

    private File _outFile;
    private FileSink.Builder _fileSinkBuilder;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
