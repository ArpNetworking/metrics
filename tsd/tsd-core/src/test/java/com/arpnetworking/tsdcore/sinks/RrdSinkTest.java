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
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>RrdSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class RrdSinkTest {

    @Before
    public void before() throws IOException {
        _path = Files.createTempDir();
        final File rrdToolFile = getRrdToolFile();
        final File outFile = getOutFile();
        Files.write("#!/bin/bash\necho \"$@\" >> " + outFile.getAbsolutePath(), rrdToolFile, Charsets.UTF_8);
        rrdToolFile.setExecutable(true);
        _rrdSinkBuilder = new RrdSink.Builder()
                .setName("rrd_sink_test")
                .setPath(_path.getAbsolutePath())
                .setRrdTool(rrdToolFile.getAbsolutePath());
    }

    @Test
    public void testClose() {
        final Sink rrdSink = _rrdSinkBuilder.build();
        rrdSink.close();
        Assert.assertFalse(getOutFile().exists());
    }

    @Test
    public void testRecordProcessedAggregateData() throws IOException {
        final Sink rrdSink = _rrdSinkBuilder.build();
        final AggregatedData datum = TestBeanFactory.createAggregatedData();
        rrdSink.recordAggregateData(Collections.singletonList(datum));

        final List<String> outLines = Files.readLines(getOutFile(), Charsets.UTF_8);
        Assert.assertEquals(2, outLines.size());
        final String[] createLine = outLines.get(0).split(" ");
        final String[] updateLine = outLines.get(1).split(" ");
        Assert.assertEquals(8, createLine.length);
        Assert.assertEquals("create", createLine[0]);
        Assert.assertEquals(3, updateLine.length);
        Assert.assertEquals("update", updateLine[0]);
    }

    @Test
    public void testMultipleRecordProcessedAggregateData() throws IOException {
        final Sink rrdSink = _rrdSinkBuilder.build();
        final AggregatedData datumA = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setMetric("metric")
                        .setStatistic(new MeanStatistic())
                        .build())
                .setHost("localhost")
                .setPeriod(Period.minutes(5))
                .build();
        rrdSink.recordAggregateData(Collections.singletonList(datumA));

        // Simulate rrd file creation
        Assert.assertTrue(getRrdFile(datumA).createNewFile());

        final AggregatedData datumB = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setMetric("metric")
                        .setStatistic(new MeanStatistic())
                        .build())
                .setHost("localhost")
                .setPeriod(Period.minutes(5))
                .build();
        rrdSink.recordAggregateData(Collections.singletonList(datumB));

        final List<String> outLines = Files.readLines(getOutFile(), Charsets.UTF_8);
        Assert.assertEquals(3, outLines.size());
        final String[] createLine = outLines.get(0).split(" ");
        final String[] updateLineA = outLines.get(1).split(" ");
        final String[] updateLineB = outLines.get(2).split(" ");
        Assert.assertEquals(8, createLine.length);
        Assert.assertEquals("create", createLine[0]);
        Assert.assertEquals(3, updateLineA.length);
        Assert.assertEquals("update", updateLineA[0]);
        Assert.assertEquals(3, updateLineB.length);
        Assert.assertEquals("update", updateLineB[0]);
    }

    private File getRrdToolFile() {
        return new File(_path.getAbsolutePath() + File.separator + "rrdtool");
    }

    private File getOutFile() {
        return new File(_path.getAbsolutePath() + File.separator + "rrdtool.out");
    }

    private File getRrdFile(final AggregatedData datum) {
        return new File(_path.getAbsolutePath()
                + File.separator
                + (datum.getHost() + "."
                        + datum.getFQDSN().getMetric() + "."
                        + datum.getPeriod().toString()
                        + datum.getFQDSN().getStatistic().getName()
                        + ".rrd").replace("/", "-"));
    }

    private RrdSink.Builder _rrdSinkBuilder;
    private File _path;
}
