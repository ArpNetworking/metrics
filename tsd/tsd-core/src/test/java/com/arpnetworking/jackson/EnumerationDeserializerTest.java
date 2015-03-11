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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for the <code>EnumerationDeserializer</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class EnumerationDeserializerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testDeserializer() throws Exception {
        final EnumerationDeserializerStrategy<TestEnum> strategy = Mockito.mock(EnumerationDeserializerStrategy.class);
        Mockito.doReturn(TestEnum.FOO).when(strategy).toEnum(Mockito.any(Class.class), Mockito.eq("bar"));

        final SimpleModule module = new SimpleModule();
        module.addDeserializer(TestEnum.class, EnumerationDeserializer.newInstance(TestEnum.class, strategy));
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();
        objectMapper.registerModule(module);

        final TestClass c = objectMapper.readValue("{\"enum\":\"bar\"}", TestClass.class);
        Mockito.verify(strategy).toEnum(Mockito.any(Class.class), Mockito.eq("bar"));
        Assert.assertEquals(TestEnum.FOO, c.getEnum());
    }

    private static final class TestClass {
        @SuppressWarnings("unused")
        public void setEnum(final TestEnum e) {
            _enum = e;
        }

        public TestEnum getEnum() {
            return _enum;
        }

        private TestEnum _enum;
    }

    private enum TestEnum {
        FOO
    }
}
