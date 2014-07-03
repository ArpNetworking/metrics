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
package com.arpnetworking.metrics.impl;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Unit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for <code>TsdQuantity</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdQuantityTest {

    @Test
    public void testQuantity() {
        final Long expectedValue = Long.valueOf(1);
        final Unit expectedUnit = Unit.BYTE;
        final Quantity q = TsdQuantity.newInstance(expectedValue, expectedUnit);
        Assert.assertEquals(expectedValue, q.getValue());
        Assert.assertEquals(expectedUnit, q.getUnit());
    }

    @Test
    public void testToString() {
        final String asString = TsdQuantity.newInstance(Long.valueOf(1), Unit.BYTE).toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }
}
