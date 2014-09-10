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
package com.arpnetworking.tsdcore.limiter;

import com.arpnetworking.tsdcore.limiter.DefaultMetricsLimiter.Mark;

import org.hamcrest.Matchers;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
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
public class MetricsLimiterStateManagerTest extends MetricsLimiterTestBase {
    private static final long T1 = System.currentTimeMillis();
    private static final long T2 = T1 - 60 * 1000; //one minute earlier
    private static final String T1_STRING = Long.toString(T1);
    private static final String T2_STRING = Long.toString(T2);

    private ConcurrentMap<String, Mark> _marks;

    @Before
    public void setUp() throws IOException {
        _marks = new ConcurrentHashMap<>();
        _marks.put(METRIC_A, new Mark(6, T1));
        _marks.put(METRIC_B, new Mark(1, T2));
        Files.deleteIfExists(STATE_FILE);
    }

    @Test
    public void stateFileCanBeWritten() throws Exception {
        final MetricsLimiterStateManager out = MetricsLimiterStateManager.builder()
                .withStateFile(STATE_FILE)
                .build(_marks);

        out.writeState();

        Assert.assertTrue(Files.exists(STATE_FILE));

        final List<String> lines = Files.readAllLines(STATE_FILE, StandardCharsets.UTF_8);

        Assert.assertEquals(2, lines.size());
        Assert.assertThat(lines.get(0), Matchers.containsString(T1_STRING));
        Assert.assertThat(lines.get(0), Matchers.containsString(" " + Integer.toString(6) + " "));
        Assert.assertThat(lines.get(1), Matchers.containsString(T2_STRING));
        Assert.assertThat(lines.get(1), Matchers.containsString(" " + Integer.toString(1) + " "));
        Assert.assertThat(lines, Matchers.allOf(
                Matchers.hasItem(Matchers.containsString(METRIC_A)),
                Matchers.hasItem(Matchers.containsString(METRIC_B))
                ));
    }

    @Test
    public void stateFileCanBeRead() throws Exception {
        final MetricsLimiterStateManager out = MetricsLimiterStateManager.builder()
                .withStateFile(STATE_FILE)
                .build(_marks);
        out.writeState();

        final Map<String, Mark> inMarks = out.readState();

        Assert.assertEquals(2, inMarks.size());
        Assert.assertThat(inMarks, Matchers.allOf(
                Matchers.hasEntry(METRIC_A, new DefaultMetricsLimiter.Mark(6, T1)),
                Matchers.hasEntry(METRIC_B, new DefaultMetricsLimiter.Mark(1, T2))
                ));
    }

    @Test
    public void autoWriterStartsAndStops() throws Exception {
        MetricsLimiterStateManager manager = null;
        try {
            manager = MetricsLimiterStateManager.builder()
                    .withStateFile(STATE_FILE)
                    .withStateFileFlushInterval(Duration.millis(1))  // There is a 500ms sleep in the loop.
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
}
