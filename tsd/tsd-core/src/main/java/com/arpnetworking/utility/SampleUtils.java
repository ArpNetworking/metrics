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

import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

/**
 * Utilities to convert samples.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class SampleUtils {
    /**
     * Converts all of the samples to a single unit.
     * 
     * @param samples samples to convert
     * @return a new list of samples with a unified unit
     */
    public static List<Quantity> unifyUnits(final List<Quantity> samples) {
        //This is a 2-pass operation:
        //First pass is to grab the smallest unit in the samples
        //Second pass is to convert everything to that unit
        Optional<Unit> smallestUnit = Optional.absent();
        for (final Quantity sample : samples) {
            if (!smallestUnit.isPresent()) {
                smallestUnit = sample.getUnit();
            } else if (sample.getUnit().isPresent() && smallestUnit.get().getSmallerUnit(sample.getUnit().get()) != smallestUnit.get()) {
                smallestUnit = sample.getUnit();
            }
        }

        if (smallestUnit.isPresent()) {
            return FluentIterable.from(samples).transform(new SampleConverter(smallestUnit.get())).toList();
        }

        return Lists.newArrayList(samples);
    }

    private SampleUtils() {}

    private static class SampleConverter implements Function<Quantity, Quantity> {
        public SampleConverter(final Unit convertTo) {
            _unit = convertTo;
        }

        @Override
        public Quantity apply(final Quantity input) {
            if (!input.getUnit().isPresent()) {
                throw new IllegalArgumentException("Cannot convert a sample with no unit: " + input);
            }
            return new Quantity(_unit.convert(input.getValue(), input.getUnit().get()), Optional.of(_unit));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(_unit);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleConverter that = (SampleConverter) o;

            return Objects.equals(_unit, that._unit);

        }

        private final Unit _unit;
    }
}
