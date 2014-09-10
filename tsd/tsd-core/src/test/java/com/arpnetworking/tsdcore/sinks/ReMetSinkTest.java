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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>ReMetSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ReMetSinkTest {

    @Before
    public void before() {
        _remetSinkBuilder = new ReMetSink.Builder()
                .setName("file_sink_test")
                .setUri(URI.create("localhost:8888"));
    }

    @Test
    public void testSerialize() throws IOException {
        final AggregatedData datum = TestBeanFactory.createAggregatedData();
        final List<AggregatedData> data = Collections.singletonList(datum);
        final ReMetSink remetSink = (ReMetSink) _remetSinkBuilder.build();
        final Collection<String> serializedData = remetSink.serialize(data);
        remetSink.close();

        Assert.assertEquals(1, serializedData.size());
        final JsonNode jsonArrayNode = OBJECT_MAPPER.readTree(Iterables.getOnlyElement(serializedData));
        Assert.assertTrue(jsonArrayNode.isArray());
        Assert.assertEquals(1, jsonArrayNode.size());
        final JsonNode jsonNode = jsonArrayNode.get(0);
        assertJsonEqualsDatum(jsonNode, datum);
    }

    @Test
    public void testSerializeMultiple() throws IOException {
        final AggregatedData datumA = TestBeanFactory.createAggregatedData();
        final AggregatedData datumB = TestBeanFactory.createAggregatedData();
        final List<AggregatedData> data = Lists.newArrayList(datumA, datumB);
        final ReMetSink remetSink = (ReMetSink) _remetSinkBuilder.build();
        final Collection<String> serializedData = remetSink.serialize(data);
        remetSink.close();

        Assert.assertEquals(1, serializedData.size());
        final JsonNode jsonArrayNode = OBJECT_MAPPER.readTree(Iterables.getOnlyElement(serializedData));
        Assert.assertTrue(jsonArrayNode.isArray());
        Assert.assertEquals(2, jsonArrayNode.size());
        final JsonNode jsonNodeA = jsonArrayNode.get(0);
        final JsonNode jsonNodeB = jsonArrayNode.get(1);
        assertJsonEqualsDatum(jsonNodeA, datumA);
        assertJsonEqualsDatum(jsonNodeB, datumB);
    }

    private static void assertJsonEqualsDatum(final JsonNode jsonNode, final AggregatedData datum) {
        Assert.assertEquals(datum.getValue(), jsonNode.get("value").asDouble(), 0.001);
        Assert.assertEquals(datum.getMetric(), jsonNode.get("metric").asText());
        Assert.assertEquals(datum.getService(), jsonNode.get("service").asText());
        Assert.assertEquals(datum.getHost(), jsonNode.get("host").asText());
        Assert.assertEquals(datum.getPeriod(), Period.parse(jsonNode.get("period").asText()));
        Assert.assertEquals(datum.getPeriodStart().getMillis(), DateTime.parse(jsonNode.get("periodStart").asText()).getMillis());
        Assert.assertEquals(datum.getStatistic().getName(), jsonNode.get("statistic").asText());
    }

    private ReMetSink.Builder _remetSinkBuilder;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
