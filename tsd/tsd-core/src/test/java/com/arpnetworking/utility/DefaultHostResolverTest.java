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
package com.arpnetworking.utility;

import org.junit.Assert;
import org.junit.Test;

import java.net.UnknownHostException;

/**
 * Tests for the DefaultHostResolver class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DefaultHostResolverTest {

    @Test
    public void test() throws UnknownHostException {
        final String hostName = new DefaultHostResolver().getLocalHostName();
        Assert.assertNotNull(hostName);
        Assert.assertFalse(hostName.isEmpty());
    }
}
