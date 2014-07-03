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
package com.arpnetworking.tsdcore.exceptions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the ConfigurationException class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ConfigurationExceptionTest {

    @Test
    public void testConstructor() {
        final String expectedMessage = "The Message";
        final ConfigurationException exception = new ConfigurationException(expectedMessage);
        Assert.assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void testConstructorWithCause() {
        final String expectedMessage = "The Message";
        final Throwable expectedCause = new NullPointerException("The Cause");
        final ConfigurationException exception = new ConfigurationException(expectedMessage, expectedCause);
        Assert.assertEquals(expectedMessage, exception.getMessage());
        Assert.assertEquals(expectedCause, exception.getCause());
    }
}
