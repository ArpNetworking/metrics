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

import com.carrotsearch.junitbenchmarks.DataCreator;
import com.carrotsearch.junitbenchmarks.GCSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the GCSnapshotSerializer.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GCShapshotSerializerTest {
    @Test
    public void testSerialization() {
        final SimpleModule module = new SimpleModule();
        module.addSerializer(GCSnapshot.class, new GCSnapshotSerializer());
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        final GCSnapshot testSnapshot = DataCreator.createGCSnapshot();
        final long accumulatedInvocations = testSnapshot.accumulatedInvocations();
        final long accumulatedTime = testSnapshot.accumulatedTime();

        final JsonNode jsonNode = mapper.valueToTree(testSnapshot);
        Assert.assertTrue(jsonNode.isObject());
        Assert.assertEquals(jsonNode.get("accumulatedInvocations").asLong(), accumulatedInvocations);
        Assert.assertEquals(jsonNode.get("accumulatedTime").asLong(), accumulatedTime);
    }
}
