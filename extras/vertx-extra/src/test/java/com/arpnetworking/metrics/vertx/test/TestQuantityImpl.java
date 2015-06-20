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
package com.arpnetworking.metrics.vertx.test;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Unit;

/**
 * Test implementation of <code>Quantity</code> interface.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class TestQuantityImpl implements Quantity {

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

    private TestQuantityImpl(final Builder builder) {
        _value = builder._value;
        _unit = builder._unit;
    }

    private final Number _value;
    private final Unit _unit;

    /**
     * Builder implementation of <code>TestQuantityImpl</code>.
     */
    public static final class Builder {

        /**
         * Builds an instance of <code>TestQuantityImpl</code>.
         *
         * @return A new instance of <code>TestQuantityImpl</code>.
         */
        public TestQuantityImpl build() {
            return new TestQuantityImpl(this);
        }

        /**
         * Sets the value attribute.
         *
         * @param value An instance of <code>Number</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setValue(final Number value) {
            _value = value;
            return this;
        }

        /**
         * Sets the unit attribute.
         *
         * @param value An instance of <code>Unit</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setUnit(final Unit value) {
            _unit = value;
            return this;
        }

        private Number _value;
        private Unit _unit;
    }
}
