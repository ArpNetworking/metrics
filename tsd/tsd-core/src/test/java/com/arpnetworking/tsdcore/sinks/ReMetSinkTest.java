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

import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
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
        final AggregatedData datum = TestBeanFactory.createAggregatedDataBuilder().setPeriod(Period.seconds(1)).build();
        final List<AggregatedData> data = Collections.singletonList(datum);
        final ReMetSink remetSink = _remetSinkBuilder.build();
        final Collection<String> serializedData = remetSink.serialize(data, Collections.<Condition>emptyList());
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
        final AggregatedData datumA = TestBeanFactory.createAggregatedDataBuilder().setPeriod(Period.seconds(1)).build();
        final AggregatedData datumB = TestBeanFactory.createAggregatedDataBuilder().setPeriod(Period.seconds(1)).build();
        final List<AggregatedData> data = Lists.newArrayList(datumA, datumB);
        final ReMetSink remetSink = _remetSinkBuilder.build();
        final Collection<String> serializedData = remetSink.serialize(data, Collections.<Condition>emptyList());
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

    @Test
    public void testSerializeChunk() throws IOException {
        final List<AggregatedData> data = Lists.newArrayList();
        for (int x = 0; x < 10000; x++) {
            data.add(TestBeanFactory.createAggregatedDataBuilder().setPeriod(Period.seconds(1)).build());
        }
        final int maxChunkSize = 83 * 1024;
        final ReMetSink remetSink = _remetSinkBuilder.setMaxRequestSize((long) maxChunkSize).build();
        final Collection<String> serializedData = remetSink.serialize(data, Collections.<Condition>emptyList());
        remetSink.close();

        Assert.assertThat(serializedData.size(), Matchers.greaterThan(1));
        int x = 0;
        for (final String serialized : serializedData) {
            Assert.assertThat(serialized.getBytes(Charsets.UTF_8).length, Matchers.lessThanOrEqualTo(maxChunkSize));
            final JsonNode jsonArrayNode = OBJECT_MAPPER.readTree(serialized);
            Assert.assertTrue(jsonArrayNode.isArray());
            for (final JsonNode jsonNode : jsonArrayNode) {
                assertJsonEqualsDatum(jsonNode, data.get(x));
                x++;
            }
        }
        Assert.assertEquals(10000, x);
    }

    private static void assertJsonEqualsDatum(final JsonNode jsonNode, final AggregatedData datum) {
        Assert.assertEquals(datum.getValue().getValue(), jsonNode.get("value").asDouble(), 0.001);
        Assert.assertEquals(datum.getFQDSN().getMetric(), jsonNode.get("metric").asText());
        Assert.assertEquals(datum.getFQDSN().getService(), jsonNode.get("service").asText());
        Assert.assertEquals(datum.getHost(), jsonNode.get("host").asText());
        Assert.assertEquals(datum.getPeriod(), Period.parse(jsonNode.get("period").asText()));
        Assert.assertEquals(datum.getPeriodStart().getMillis(), DateTime.parse(jsonNode.get("periodStart").asText()).getMillis());
        Assert.assertEquals(datum.getFQDSN().getStatistic().getName(), jsonNode.get("statistic").asText());
    }

    private ReMetSink.Builder _remetSinkBuilder;

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
}
