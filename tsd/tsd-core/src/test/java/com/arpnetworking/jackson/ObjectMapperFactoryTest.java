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
package com.arpnetworking.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests for <code>ObjectMapperFactory</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ObjectMapperFactoryTest {

    @Test
    public void testNewInstance() {
        final ObjectMapper objectMapper1 = ObjectMapperFactory.createInstance();
        final ObjectMapper objectMapper2 = ObjectMapperFactory.createInstance();
        Assert.assertNotSame(objectMapper1, objectMapper2);

        // Deserialization feature
        objectMapper1.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Assert.assertTrue(objectMapper1.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        Assert.assertFalse(objectMapper2.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        // Serialization feature
        objectMapper1.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper2.configure(SerializationFeature.INDENT_OUTPUT, false);
        Assert.assertTrue(objectMapper1.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT));
        Assert.assertFalse(objectMapper2.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT));
    }

    @Test
    public void testNewInstanceConversion() throws IOException {
        final String expectedJson = "{}";
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();
        final JsonNode jsonNode = objectMapper.readValue(expectedJson, JsonNode.class);
        final String actualJson = objectMapper.writeValueAsString(jsonNode);
        Assert.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGetInstance() {
        final ObjectMapper objectMapper1 = ObjectMapperFactory.getInstance();
        final ObjectMapper objectMapper2 = ObjectMapperFactory.getInstance();
        Assert.assertSame(objectMapper1, objectMapper2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInstanceUnmodifiableDeserializationConfig() {
        final ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInstanceUnmodifiableSerializationConfig() {
        final ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Test
    public void testGetInstanceToString() {
        final String asString = ObjectMapperFactory.getInstance().toString();
        Assert.assertNotNull(asString);
    }

    @Test
    public void testGetInstanceConversion() throws IOException {
        final String expectedJson = "{}";
        final ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        final JsonNode jsonNode = objectMapper.readValue(expectedJson, JsonNode.class);
        final String actualJson = objectMapper.writeValueAsString(jsonNode);
        Assert.assertEquals(expectedJson, actualJson);
    }
}
