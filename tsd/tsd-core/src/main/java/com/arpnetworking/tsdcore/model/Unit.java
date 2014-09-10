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

/**
 * Specifies the unit on a counter variable.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public enum Unit {
    /***************************************************************************
     * Time
     */
    /**
     * Nanoseconds.
     */
    NANOSECOND(1L, Type.TIME),
    /**
     * Microseconds.
     */
    MICROSECOND(1000L, Type.TIME),
    /**
     * Milliseconds.
     */
    MILLISECOND(1000L * 1000, Type.TIME),
    /**
     * Seconds.
     */
    SECOND(1000L * 1000 * 1000, Type.TIME),
    /**
     * Minutes.
     */
    MINUTE(1000L * 1000 * 1000 * 60, Type.TIME),
    /**
     * Hours.
     */
    HOUR(1000L * 1000 * 1000 * 60 * 60, Type.TIME),
    /**
     * Days.
     */
    DAY(1000L * 1000 * 1000 * 60 * 60 * 24, Type.TIME),
    /**
     * Weeks.
     */
    WEEK(1000L * 1000 * 1000 * 60 * 60 * 24 * 7, Type.TIME),

    /***************************************************************************
     * Data Size
     */
    /**
     * Bits.
     */
    BIT(1L, Type.DATA_SIZE),
    /**
     * Bytes.
     */
    BYTE(8L, Type.DATA_SIZE),
    /**
     * Kilobits.
     */
    KILOBIT(1024L, Type.DATA_SIZE),
    /**
     * Megabits.
     */
    MEGABIT(1024L * 1024, Type.DATA_SIZE),
    /**
     * Gigabits.
     */
    GIGABIT(1024L * 1024 * 1024, Type.DATA_SIZE),
    /**
     * Terabits.
     */
    TERABIT(1024L * 1024 * 1024 * 1024, Type.DATA_SIZE),
    /**
     * Petabits.
     */
    PETABIT(1024L * 1024 * 1024 * 1024 * 1024, Type.DATA_SIZE),
    /**
     * Kilobytes.
     */
    KILOBYTE(1000L * 8, Type.DATA_SIZE),
    /**
     * Megabytes.
     */
    MEGABYTE(1000L * 1000 * 8, Type.DATA_SIZE),
    /**
     * Gigabytes.
     */
    GIGABYTE(1000L * 1000 * 1000 * 8, Type.DATA_SIZE),
    /**
     * Terabytes.
     */
    TERABYTE(1000L * 1000 * 1000 * 1000 * 8, Type.DATA_SIZE),
    /**
     * Petabytes.
     */
    PETABYTE(1000L * 1000 * 1000 * 1000 * 1000 * 8, Type.DATA_SIZE),

    /***************************************************************************
     * Temperature
     */
    /**
     * Kelvin.
     */
    KELVIN(1L, Type.TEMPERATURE) {
        @Override
        public double convert(final double sourceValue, final Unit sourceUnit) {
            if (KELVIN.equals(sourceUnit)) {
                return sourceValue;
            } else if (CELCIUS.equals(sourceUnit)) {
                return sourceValue + 273.15;
            } else if (FAHRENHEIT.equals(sourceUnit)) {
                return (sourceValue + 459.67) * 5.0 / 9.0;
            }
            throw new IllegalArgumentException("Conversion not supported; from: " + sourceUnit + " to:" + this);
        }
    },
    /**
     * Celcius.
     */
    CELCIUS(2L, Type.TEMPERATURE) {
        @Override
        public double convert(final double sourceValue, final Unit sourceUnit) {
            if (CELCIUS.equals(sourceUnit)) {
                return sourceValue;
            } else if (FAHRENHEIT.equals(sourceUnit)) {
                return (sourceValue - 32.0) * 5.0 / 9.0;
            } else if (KELVIN.equals(sourceUnit)) {
                return sourceValue - 273.15;
            }
            throw new IllegalArgumentException("Conversion not supported; from: " + sourceUnit + " to:" + this);
        }
    },
    /**
     * Fahrenheit.
     */
    FAHRENHEIT(3L, Type.TEMPERATURE) {
        @Override
        public double convert(final double sourceValue, final Unit sourceUnit) {
            if (FAHRENHEIT.equals(sourceUnit)) {
                return sourceValue;
            } else if (CELCIUS.equals(sourceUnit)) {
                return sourceValue * 9.0 / 5.0 + 32.0;
            } else if (KELVIN.equals(sourceUnit)) {
                return sourceValue * 9.0 / 5.0 - 459.67;
            }
            throw new IllegalArgumentException("Conversion not supported; from: " + sourceUnit + " to:" + this);
        }
    };

    /* package private */enum Type {
        TIME,
        DATA_SIZE,
        TEMPERATURE
    }

    private Unit(final long scale, final Type type) {
        _scale = scale;
        _type = type;
    }

    /**
     * Converts a value in one unit to another.
     * @param sourceValue the value to be converted
     * @param sourceUnit the unit of the source value
     * @return the value after conversion
     */
    public double convert(final double sourceValue, final Unit sourceUnit) {
        return sourceUnit._scale / this._scale * sourceValue;
    }

    /**
     * Gets the smaller unit of two.
     * @param otherUnit the unit to compare this against
     * @return the smaller unit
     */
    public Unit getSmallerUnit(final Unit otherUnit) {
        if (this._type != otherUnit._type) {
            throw new IllegalArgumentException("Units must be of the same kind");
        }
        return this._scale < otherUnit._scale ? this : otherUnit;
    }

    private final double _scale;
    private final Type _type;
}
