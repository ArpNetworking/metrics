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

import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import net.sf.oval.constraint.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a sample.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Quantity implements Comparable<Quantity>, Serializable {

    /**
     * Public constructor.
     *
     * @param value the value
     * @param unit the unit of the value
     * @deprecated Replaced with <code>Builder</code>.
     */
    @Deprecated
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
     * Convert this <code>Quantity</code> to one in the specified unit. This
     * <code>Quantity</code> must also have a <code>Unit</code> and it must
     * be in the same domain as the provided unit.
     *
     * @param unit <code>Unit</code> to convert to.
     * @return <code>Quantity</code> in specified unit.
     */
    public Quantity convertTo(final Unit unit) {
        if (!_unit.isPresent()) {
            throw new IllegalStateException(String.format(
                    "Cannot convert a quantity without a unit; this=%s",
                    this));
        }
        return new Quantity(unit.convert(_value, _unit.get()), Optional.of(unit));
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
        throw new IllegalArgumentException(String.format(
                "Cannot compare a quantity with a unit to a quantity without a unit; this=%s, other=%s",
                this,
                other));
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
        return MoreObjects.toStringHelper(this)
                .add("Unit", _unit)
                .add("Value", _value)
                .toString();
    }

    private Quantity(final Builder builder) {
        _value = builder._value.doubleValue();
        _unit = Optional.fromNullable(builder._unit);
    }

    private final Optional<Unit> _unit;
    private final double _value;

    private static final long serialVersionUID = -6339526234042605516L;

    /**
     * <code>Builder</code> implementation for <code>Quantity</code>.
     */
    public static final class Builder extends OvalBuilder<Quantity> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(Quantity.class);
        }

        /**
         * Set the value. Required. Cannot be null.
         *
         * @param value The value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setValue(final Double value) {
            _value = value;
            return this;
        }

        /**
         * Set the unit. Optional. Default is no unit.
         *
         * @param value The unit.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUnit(final Unit value) {
            _unit = value;
            return this;
        }

        @NotNull
        private Double _value;
        private Unit _unit;
    }
}
