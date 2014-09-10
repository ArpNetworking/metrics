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
package com.arpnetworking.tsdcore.model;

import com.google.common.base.Optional;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the Quantity class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class QuantityTest {

    @Test
    public void testConstructor() {
        final double expectedValue = 3.14f;
        final Unit expectedUnit = Unit.GIGABYTE;
        final Quantity sample = new Quantity(expectedValue, Optional.of(expectedUnit));
        Assert.assertEquals(expectedValue, sample.getValue(), 0.001);
        Assert.assertTrue(sample.getUnit().isPresent());
        Assert.assertEquals(expectedUnit, sample.getUnit().get());
    }

    @Test
    public void testCompare() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample3 = new Quantity(3.14f, Optional.of(Unit.GIGABIT));
        final Quantity sample4 = new Quantity(3.14f, Optional.of(Unit.TERABYTE));

        Assert.assertEquals(0, sample1.compareTo(sample1));
        Assert.assertEquals(0, sample1.compareTo(sample2));
        Assert.assertEquals(1, sample1.compareTo(sample3));
        Assert.assertEquals(-1, sample1.compareTo(sample4));

        final Quantity sample5 = new Quantity(3.14f, Optional.<Unit>absent());
        final Quantity sample6 = new Quantity(3.14f, Optional.<Unit>absent());
        final Quantity sample7 = new Quantity(6.28f, Optional.<Unit>absent());

        Assert.assertEquals(0, sample5.compareTo(sample6));
        Assert.assertEquals(-1, sample5.compareTo(sample7));
        Assert.assertEquals(1, sample7.compareTo(sample5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareFailureAbsent() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.<Unit>absent());
        sample1.compareTo(sample2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareFailureAbsentReverse() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.<Unit>absent());
        sample2.compareTo(sample1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareFailureDifferentDomains() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.of(Unit.SECOND));
        sample1.compareTo(sample2);
    }

    @Test
    public void testHash() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));

        Assert.assertEquals(sample1.hashCode(), sample2.hashCode());
    }

    @Test
    public void testEquality() {
        final Quantity sample1 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample2 = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        final Quantity sample3 = new Quantity(3.14f, Optional.of(Unit.GIGABIT));
        final Quantity sample4 = new Quantity(6.28f, Optional.of(Unit.GIGABYTE));
        final Quantity sample5 = new Quantity(3.14f, Optional.<Unit>absent());

        Assert.assertTrue(sample1.equals(sample1));
        Assert.assertFalse(sample1.equals("Not a sample"));
        Assert.assertFalse(sample1.equals(null));
        Assert.assertTrue(sample1.equals(sample2));
        Assert.assertFalse(sample1.equals(sample3));
        Assert.assertFalse(sample1.equals(sample4));
        Assert.assertFalse(sample1.equals(sample5));
        Assert.assertFalse(sample3.equals(sample4));
    }

    @Test
    public void testToString() {
        final Quantity sample = new Quantity(3.14f, Optional.of(Unit.GIGABYTE));
        Assert.assertNotNull(sample.toString());
        Assert.assertFalse(sample.toString().isEmpty());
    }
}
