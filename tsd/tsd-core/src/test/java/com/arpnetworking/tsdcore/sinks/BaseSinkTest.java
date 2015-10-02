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

import com.arpnetworking.tsdcore.model.AggregatedData;

import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

/**
 * Tests for the <code>BaseSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class BaseSinkTest {

    @Test
    public void testName() {
        final String expectedName = "TheSinkName";
        final TestAggregatedDataSink sink = new TestAggregatedDataSink.Builder()
                .setName(expectedName)
                .build();
        Assert.assertEquals(expectedName, sink.getName());
    }

    @Test
    public void testMetricName() {
        final TestAggregatedDataSink sink = new TestAggregatedDataSink.Builder()
                .setName("The.Sink.Name")
                .build();
        Assert.assertEquals("The_Sink_Name", sink.getMetricSafeName());
    }

    private static final class TestAggregatedDataSink extends BaseSink {

        @Override
        public void recordAggregateData(final PeriodicData data) {
            // Nothing to do
        }

        @Override
        public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
            // Nothing to do
        }

        @Override
        public void close() {
            // Nothing to do
        }

        private TestAggregatedDataSink(final Builder builder) {
            super(builder);
        }

        public static final class Builder extends BaseSink.Builder<Builder, TestAggregatedDataSink> {

            public Builder() {
                super(TestAggregatedDataSink.class);
            }

            @Override
            protected Builder self() {
                return this;
            }
        }
    }
}
