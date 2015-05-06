/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.metrics.jvm.collectors;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Tests the <code>MetricsUtil</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class MetricsUtilTest {

    @Test
    public void convertToSnakeCaseUpperCaseStringWithSpaces() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("Test String");
        Assert.assertEquals("test_string", snakeCased);
    }

    @Test
    public void convertToSnakeCaseLowerCaseStringWithSpaces() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("test string");
        Assert.assertEquals("test_string", snakeCased);
    }

    @Test
    public void convertToSnakeCaseMixedCaseStringWithSpaces() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("Test string");
        Assert.assertEquals("test_string", snakeCased);
    }

    @Test
    public void convertToSnakeCaseLowerCaseStringWithoutSpaces() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("teststring");
        Assert.assertEquals("teststring", snakeCased);
    }

    @Test
    public void convertToSnakeCaseUpperCaseStringWithoutSpaces() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("Teststring");
        Assert.assertEquals("teststring", snakeCased);
    }

    @Test
    public void convertToSnakeCaseAlreadyCamelCase() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("TestStringAt");
        Assert.assertEquals("test_string_at", snakeCased);
    }

    @Test
    public void convertToSnakeCaseStartingWithDigit() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("1 Test String");
        Assert.assertEquals("1_test_string", snakeCased);
    }

    @Test
    public void convertToSnakeCaseWithUpperCaseElements() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("PS Survivor Space");
        Assert.assertEquals("ps_survivor_space", snakeCased);
    }

    @Test
    public void convertToSnakeCaseWithUpperCaseElementsWithoutSpace() {
        final String snakeCased = MetricsUtil.convertToSnakeCase("PS MarkSweep");
        Assert.assertEquals("ps_mark_sweep", snakeCased);
    }

    @Test
    public void testPrivateConstructor() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor<MetricsUtil> constructor = MetricsUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
