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
package com.arpnetworking.tsdcore.limiter.legacy;

import com.arpnetworking.tsdcore.limiter.legacy.LegacyMetricsLimiter.Mark;

import org.hamcrest.Matchers;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Test cases for MetricsLimiterStateManager class.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
@Ignore // Test _class_ cannot be executed in parallel; soon to be deprecated anyway
public class MetricsLimiterStateManagerTest extends MetricsLimiterTestBase {

    @Before
    public void setUp() throws IOException {
        _time1 = System.currentTimeMillis();
        _time2 = _time1 - 60 * 1000; // one minute earlier
        _time1AsString = Long.toString(_time1);
        _time2AsString = Long.toString(_time2);

        _marks = new ConcurrentHashMap<>();
        _marks.put(METRIC_A, new Mark(6, _time1));
        _marks.put(METRIC_B, new Mark(1, _time2));
        Files.deleteIfExists(STATE_FILE);
    }

    @Test
    public void testStateFileCanBeWritten() throws Exception {
        final MetricsLimiterStateManager out = new MetricsLimiterStateManager.Builder()
                .setStateFile(STATE_FILE)
                .build(_marks);

        out.writeState();

        Assert.assertTrue(Files.exists(STATE_FILE));

        final List<String> lines = Files.readAllLines(STATE_FILE, StandardCharsets.UTF_8);

        Assert.assertEquals(2, lines.size());
        Assert.assertThat(lines, Matchers.allOf(
                Matchers.hasItem(Matchers.containsString(METRIC_A)),
                Matchers.hasItem(Matchers.containsString(METRIC_B))
        ));
        if (lines.get(0).contains(METRIC_A)) {
            Assert.assertThat(lines.get(0), Matchers.containsString(_time1AsString));
            Assert.assertThat(lines.get(0), Matchers.containsString(" " + Integer.toString(6) + " "));
            Assert.assertThat(lines.get(1), Matchers.containsString(_time2AsString));
            Assert.assertThat(lines.get(1), Matchers.containsString(" " + Integer.toString(1) + " "));

        } else {
            Assert.assertThat(lines.get(1), Matchers.containsString(_time1AsString));
            Assert.assertThat(lines.get(1), Matchers.containsString(" " + Integer.toString(6) + " "));
            Assert.assertThat(lines.get(0), Matchers.containsString(_time2AsString));
            Assert.assertThat(lines.get(0), Matchers.containsString(" " + Integer.toString(1) + " "));

        }
    }

    @Test
    public void testStateFileCanBeRead() throws Exception {
        final MetricsLimiterStateManager out = new MetricsLimiterStateManager.Builder()
                .setStateFile(STATE_FILE)
                .build(_marks);
        out.writeState();

        final Map<String, Mark> inMarks = out.readState();

        Assert.assertEquals(2, inMarks.size());
        Assert.assertThat(inMarks, Matchers.allOf(
                Matchers.hasEntry(METRIC_A, new Mark(6, _time1)),
                Matchers.hasEntry(METRIC_B, new Mark(1, _time2))
                ));
    }

    @Test
    public void testAutoWriterStartsAndStops() throws Exception {
        MetricsLimiterStateManager manager = null;
        try {
            manager = new MetricsLimiterStateManager.Builder()
                    .setStateFile(STATE_FILE)
                    .setStateFileFlushInterval(Duration.millis(1))  // There is a 500ms sleep in the loop.
                    .build(_marks);

            manager.startAutoWriter();
            Thread.yield();

            Assert.assertTrue(manager.isAlive());

            // Give the file a chance to be written
            assertStateFileAppears();

            Files.delete(STATE_FILE);
            manager.stopAutoWriter(true);

            // Final write
            assertStateFileAppears();
        } finally {
            if (manager != null) {
                manager.stopAutoWriter(true);
            }
        }
    }

    private ConcurrentMap<String, Mark> _marks;
    private long _time1;
    private long _time2;
    private String _time1AsString;
    private String _time2AsString;
}
