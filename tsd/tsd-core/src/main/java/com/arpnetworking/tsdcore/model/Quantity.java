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

import java.util.Objects;

/**
 * Represents a sample.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Quantity implements Comparable<Quantity> {

    /**
     * Constructor.
     * 
     * @param value the value
     * @param unit the unit of the value
     */
    public Quantity(final double value, final Optional<Unit> unit) {
        _value = value;
        _unit = unit;
    }

    public double getValue() {
        return _value;
    }

    public Optional<Unit> getUnit() {
        return _unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Quantity other) {
        if (other._unit.equals(_unit)) {
            return Double.compare(_value, other._value);
        } else if (other._unit.isPresent() && _unit.isPresent()) {
            final Unit smallerUnit = _unit.get().getSmallerUnit(other._unit.get());
            final double convertedValue = smallerUnit.convert(_value, _unit.get());
            final double otherConvertedValue = smallerUnit.convert(other._value, other._unit.get());
            return Double.compare(convertedValue, otherConvertedValue);
        }
        throw new IllegalArgumentException(
                "Cannot compare a sample with a unit to a sample without a unit; this=" + this + " other=" + other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(Double.valueOf(_value), _unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quantity)) {
            return false;
        }

        final Quantity sample = (Quantity) o;

        return Double.compare(sample._value, _value) == 0
                && Objects.equals(_unit, sample._unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("Unit", _unit)
                .add("Value", _value)
                .toString();
    }

    private final Optional<Unit> _unit;
    private final double _value;
}
