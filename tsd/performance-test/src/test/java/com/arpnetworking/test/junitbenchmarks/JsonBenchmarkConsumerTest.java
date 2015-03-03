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

package com.arpnetworking.test.junitbenchmarks;

import com.arpnetworking.jackson.ObjectMapperFactory;
import com.carrotsearch.junitbenchmarks.DataCreator;
import com.carrotsearch.junitbenchmarks.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests a {@link JsonBenchmarkConsumer}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class JsonBenchmarkConsumerTest {
    @Test
    public void testNormalBenchmarkCase() throws IOException {
        final Path path = Paths.get("build/tmp/test/testConsumer.json");
        path.toFile().deleteOnExit();
        Files.deleteIfExists(path);
        final JsonBenchmarkConsumer consumer = new JsonBenchmarkConsumer(path);

        final Result result = DataCreator.createResult();
        consumer.accept(result);
        consumer.close();

        // Read the file back in as json
        final ObjectMapper mapper = ObjectMapperFactory.getInstance();
        final JsonNode readBack = mapper.readTree(path.toFile());
        Assert.assertTrue(readBack.isObject());
        Assert.assertTrue(readBack.get("results").isArray());
        final ArrayNode resultsArray = (ArrayNode) readBack.get("results");
        Assert.assertEquals(1, resultsArray.size());
        final JsonNode resultNode = resultsArray.get(0);
        Assert.assertTrue(resultNode.isObject());

        Assert.assertEquals("com.arpnetworking.test.junitbenchmarks.JsonBenchmarkConsumerTest", resultNode.get("testClassName").asText());
        Assert.assertEquals("testNormalBenchmarkCase", resultNode.get("testMethodName").asText());

        Assert.assertEquals(result.benchmarkRounds, resultNode.get("benchmarkRounds").asInt());
        Assert.assertEquals(result.warmupRounds, resultNode.get("warmupRounds").asInt());
        Assert.assertEquals(result.warmupTime, resultNode.get("warmupTime").asInt());
        Assert.assertEquals(result.benchmarkTime, resultNode.get("benchmarkTime").asInt());

        Assert.assertTrue(resultNode.get("roundAverage").isObject());
        final ObjectNode roundAverageNode = (ObjectNode) resultNode.get("roundAverage");
        Assert.assertEquals(result.roundAverage.avg, roundAverageNode.get("avg").asDouble(), 0.0001d);
        Assert.assertEquals(result.roundAverage.stddev, roundAverageNode.get("stddev").asDouble(), 0.0001d);

        Assert.assertTrue(resultNode.get("blockedAverage").isObject());
        final ObjectNode blockedAverageNode = (ObjectNode) resultNode.get("blockedAverage");
        Assert.assertEquals(result.blockedAverage.avg, blockedAverageNode.get("avg").asDouble(), 0.0001d);
        Assert.assertEquals(result.blockedAverage.stddev, blockedAverageNode.get("stddev").asDouble(), 0.0001d);

        Assert.assertTrue(resultNode.get("gcAverage").isObject());
        final ObjectNode gcAverageNode = (ObjectNode) resultNode.get("gcAverage");
        Assert.assertEquals(result.gcAverage.avg, gcAverageNode.get("avg").asDouble(), 0.0001d);
        Assert.assertEquals(result.gcAverage.stddev, gcAverageNode.get("stddev").asDouble(), 0.0001d);

        Assert.assertTrue(resultNode.get("gcInfo").isObject());
        final ObjectNode gcInfoNode = (ObjectNode) resultNode.get("gcInfo");
        Assert.assertEquals(result.gcInfo.accumulatedInvocations(), gcInfoNode.get("accumulatedInvocations").asInt());
        Assert.assertEquals(result.gcInfo.accumulatedTime(), gcInfoNode.get("accumulatedTime").asInt());

        Assert.assertEquals(result.getThreadCount(), resultNode.get("threadCount").asInt());
    }

    @Test
    public void testMultipleClose() throws IOException {
        final Path path = Paths.get("build/tmp/test/testConsumerMultiClose.json");
        path.toFile().deleteOnExit();
        Files.deleteIfExists(path);
        final JsonBenchmarkConsumer consumer = new JsonBenchmarkConsumer(path);

        final Result result = DataCreator.createResult();
        consumer.accept(result);
        consumer.close();
        // Should not throw an exception
        consumer.close();
    }
    @Test
    public void testCreatesParentDirs() throws IOException {
        final Path path = Paths.get("build/tmp/test/another/directory/testConsumerMultiClose.json");
        path.toFile().deleteOnExit();
        FileUtils.deleteDirectory(Paths.get("build/tmp/test/another").toFile());
        final JsonBenchmarkConsumer consumer = new JsonBenchmarkConsumer(path);

        final Result result = DataCreator.createResult();
        consumer.accept(result);
        consumer.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteAfterClose() throws IOException {
        final Path path = Paths.get("build/tmp/test/testConsumerMultiClose.json");
        path.toFile().deleteOnExit();
        Files.deleteIfExists(path);
        final JsonBenchmarkConsumer consumer = new JsonBenchmarkConsumer(path);

        final Result result = DataCreator.createResult();
        consumer.accept(result);
        consumer.close();
        // Should throw
        consumer.accept(result);
    }

    @Test
    public void testWriteInvalidFile() throws IOException {
        final Path path = Paths.get("/tmp");
        final JsonBenchmarkConsumer consumer = new JsonBenchmarkConsumer(path);

        final Result result = DataCreator.createResult();
        consumer.accept(result);
        consumer.close();
    }
}
