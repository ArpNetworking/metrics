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
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

/**
 * Test cases for DefaultMetricsLimiter class.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
public class LegacyMetricsLimiterTest extends MetricsLimiterTestBase {

    @Before
    public void setUp() {
        _builder = new LegacyMetricsLimiter.Builder()
                .setEnableStateAutoWriter(Boolean.FALSE)
                .setMaxAggregations(1000L)
                .setStateManagerBuilder(
                        new MetricsLimiterStateManager.Builder()
                                .setStateFile(STATE_FILE)
                );
    }

    @Test
    public void singleAggregationFits() throws Exception {
        final LegacyMetricsLimiter limiter = _builder
                .setMaxAggregations(6L)
                .build();

        // Fits exactly
        final DateTime now = new DateTime();
        boolean accepted = false;
        accepted = limiter.offer(TSD_1A, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1B, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1C, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1D, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1E, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1F, now);
        Assert.assertTrue(accepted);
    }

    @Test
    public void aggregationsThatDontFitReturnFalse() throws Exception {
        // Only room for TSD_1
        final LegacyMetricsLimiter limiter = _builder
                .setMaxAggregations(6L)
                .build();

        final DateTime now = new DateTime();
        limiter.offer(TSD_1A, now);
        limiter.offer(TSD_1B, now);
        limiter.offer(TSD_1C, now);
        limiter.offer(TSD_1D, now);
        limiter.offer(TSD_1E, now);
        limiter.offer(TSD_1F, now);

        boolean accepted = false;
        accepted = limiter.offer(TSD_2A, now);
        Assert.assertFalse(accepted);
    }

    @Test
    public void aggregationsAddAlreadyAdded() throws Exception {
        final LegacyMetricsLimiter limiter = _builder
                .setMaxAggregations(1L)
                .build();

        final DateTime now = new DateTime();
        boolean accepted = false;
        accepted = limiter.offer(TSD_1A, now);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1A, now);
        Assert.assertTrue(accepted);
    }

    @Test
    public void aggregationsAgeOut() throws Exception {
        // Only room for TSD_1
        final LegacyMetricsLimiter limiter = _builder
                .setMaxAggregations(1L)
                .build();

        limiter.offer(TSD_1A, new DateTime(60 * 1000L)); // 1 minutes after the epoch -- a long time ago

        final DateTime now = new DateTime();
        boolean accepted = false;
        accepted = limiter.offer(TSD_2A, now);
        Assert.assertTrue(accepted);

        accepted = limiter.offer(TSD_1A, now);
        Assert.assertFalse(accepted);
    }

    @Test
    public void nAggregationsIncrementAndDecrementConsistently() {
        final LegacyMetricsLimiter limiter = _builder
                .setMaxAggregations(100L)
                .setAgeOutThreshold(Duration.standardDays(3))
                .build();

        boolean accepted = false;

        accepted = limiter.offer(TSD_1A, WEEK_0);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1B, WEEK_0);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1C, WEEK_0);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1D, WEEK_0);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1E, WEEK_0);
        Assert.assertTrue(accepted);
        accepted = limiter.offer(TSD_1F, WEEK_0);
        Assert.assertTrue(accepted);

        Assert.assertEquals(6, limiter.getNAggregations());

        accepted = limiter.offer(TSD_2A, WEEK_2);
        Assert.assertTrue(accepted);

        Assert.assertEquals(7, limiter.getNAggregations());

        // Remove TSD_1
        limiter.ageOutAggregations(WEEK_1.getMillis());
        Assert.assertEquals(1, limiter.getNAggregations());

        // Remove TSD_2
        limiter.ageOutAggregations(WEEK_3.getMillis());
        Assert.assertEquals(0, limiter.getNAggregations());
    }

    @Test
    public void stateFileOldMarksFilteredOnLoad() throws Exception {
        final long t1 = _now;
        final long t2 = t1 - 120 * 1000; //two minutes earlier

        final LegacyMetricsLimiter out = _builder.build();
        out.offer(TSD_1A, new DateTime(t1));
        out.offer(TSD_2A, new DateTime(t2));

        // Force the state to be written
        out.getStateManager().writeState();

        // Now a new limiter that reads that file
        final LegacyMetricsLimiter in = _builder
                .setAgeOutThreshold(Duration.standardMinutes(1))  // one minute, so t2 metrics should not be loaded
                .build();

        final Map<String, Mark> marks = in.getMarks();

        Assert.assertEquals(1, marks.size());
        Assert.assertThat(marks, Matchers.hasEntry(createName(TSD_1A), new Mark(1, t1)));
    }

    @Test
    public void stateFileLoadedOnLimiterCreate() throws Exception {
        final LegacyMetricsLimiter out = _builder.build();
        out.offer(TSD_1A, new DateTime(_now));
        out.offer(TSD_2A, new DateTime(_now));
        out.getStateManager().writeState();

        final int nBeforeAggregations = out.getNAggregations();

        final LegacyMetricsLimiter in = _builder.build();

        Assert.assertEquals(nBeforeAggregations, in.getNAggregations());

        final Map<String, Mark> marks = in.getMarks();

        Assert.assertEquals(2, marks.size());
        Assert.assertThat(marks, Matchers.hasEntry(createName(TSD_1A), new Mark(1, _now)));
    }

    @Test
    public void stateAffectsCurrentInstanceLimits() throws Exception {
        final long nAggregations = 6;
        final LegacyMetricsLimiter limiter = _builder.setMaxAggregations(nAggregations).build();

        // Simulate stateFile load
        limiter.updateMarksAndAggregationCount(
                Collections.singletonMap(createName(TSD_1A), new Mark(nAggregations, _now)),
                _now  // our one metric won't get filtered because it is too old
        );

        Assert.assertEquals(nAggregations, limiter.getNAggregations());
        // Limiter is "full" so no room for B
        Assert.assertFalse(limiter.offer(TSD_2A, new DateTime(_now)));
        // But METRIC_A is already in the mark list, so it should be allowed
        Assert.assertTrue(limiter.offer(TSD_1A, new DateTime(_now)));
    }

    @Test
    public void addingNewMetricForcesStateFileWrite() throws Exception {
        LegacyMetricsLimiter limiter = null;
        try {
            limiter = _builder.setEnableStateAutoWriter(true).build();

            // Just double checking -- first timed write shouldn't happen for 10 minutes
            Assert.assertFalse(Files.exists(STATE_FILE));

            // A new metric triggers a request to the AutoWriter to write the state file
            limiter.offer(TSD_1A, new DateTime(_now));
            assertStateFileAppears();
        } finally {
            if (limiter != null) {
                limiter.getStateManager().stopAutoWriter(true);
            }
        }
    }

    private LegacyMetricsLimiter.Builder _builder;
}
