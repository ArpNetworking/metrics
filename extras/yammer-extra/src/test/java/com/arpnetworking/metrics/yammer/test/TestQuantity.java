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
package com.arpnetworking.metrics.yammer.test;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Unit;

/**
 * Test implementation of <code>Quantity</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TestQuantity implements Quantity {

    /**
     * Static factory.
     * 
     * @param value The value.
     * @param unit The units of the value.
     * @return New instance of <code>TestQuantity</code>.
     */
    public static TestQuantity newInstance(final Number value, final Unit unit) {
        return new TestQuantity(value, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getValue() {
        return _value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getUnit() {
        return _unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("TestQuantity{Value=%s, Unit=%s}", _value, _unit);
    }

    private TestQuantity(final Number value, final Unit unit) {
        _value = value;
        _unit = unit;
    }

    private final Number _value;
    private final Unit _unit;
}
