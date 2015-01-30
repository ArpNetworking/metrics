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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

/**
 * Tests for <code>OptionalSerializer</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class OptionalSerializerTest {

    @Test
    public void testSerializationNull() throws IOException {
        final String serialized = OBJECT_MAPPER.writeValueAsString(Optional.absent());

        Assert.assertEquals("null", serialized);
    }

    @Test
    public void testSerializationString() throws IOException {
        final String expectedString = "ABC";
        final Optional<String> optionalString = Optional.of(expectedString);
        final String serializedString = OBJECT_MAPPER.writeValueAsString(optionalString);

        Assert.assertEquals("\"ABC\"", serializedString);
    }

    @Test
    public void testSerializationUri() throws IOException {
        final URI expectedUri = URI.create("/hosts/v1/query?name=test-app1.com");
        final Optional<URI> optionalUri = Optional.of(expectedUri);
        final String serializedUri = OBJECT_MAPPER.writeValueAsString(optionalUri);

        Assert.assertEquals("\"/hosts/v1/query?name=test-app1.com\"", serializedUri);
    }

    @Test
    public void testSerializationComplexType() throws IOException {
        final Widget expectedWidget = new Widget("foo", "bar");
        final Optional<Widget> optionalWidget = Optional.of(expectedWidget);
        final String serializedWidget = OBJECT_MAPPER.writeValueAsString(optionalWidget);

        Assert.assertEquals("\"foo\"", serializedWidget);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule();
        module.addSerializer(
                Optional.class,
                OptionalSerializer.newInstance());
        module.addSerializer(
                Widget.class,
                new JsonSerializer<Widget>() {
                    @Override
                    public void serialize(
                            final Widget value,
                            final JsonGenerator jgen,
                            final SerializerProvider provider)
                            throws IOException {

                        jgen.writeString(value.getFoo());
                    }
                });
        OBJECT_MAPPER.registerModule(module);
    }

    private static final class Widget {

        public Widget(final String foo, final String bar) {
            _foo = foo;
            _bar = bar;
        }

        public String getFoo() {
            return _foo;
        }

        public String getBar() {
            return _bar;
        }

        private final String _foo;
        private final String _bar;
    }
}
