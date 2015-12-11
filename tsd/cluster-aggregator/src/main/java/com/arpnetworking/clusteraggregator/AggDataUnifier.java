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

package com.arpnetworking.clusteraggregator;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Unifies units.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggDataUnifier {
    private AggDataUnifier() {}

    /**
     * Unifies <code>AggregatedData</code> units.
     * @param aggData List of <code>AggregatedData</code> to unify.
     * @return A new {@code List<AggregatedData>} with unified units.
     */
    public static List<AggregatedData> unify(final Collection<AggregatedData> aggData) {
        Optional<Unit> smallestUnit = Optional.absent();
        for (final AggregatedData data : aggData) {
            smallestUnit = getSmaller(smallestUnit, data.getValue().getUnit());

            for (final Quantity quantity : data.getSamples()) {
                smallestUnit = getSmaller(smallestUnit, quantity.getUnit());
            }
        }

        return FluentIterable.from(aggData).transform(new ConvertUnitTransform(smallestUnit)).toList();
    }

    private static Optional<Unit> getSmaller(final Optional<Unit> a, final Optional<Unit> b) {
        if (a.isPresent() && b.isPresent()) {
            return a.get().isSmallerThan(b.get()) ? a : b;
        } else {
            return a.or(b);
        }
    }

    private static final class ConvertUnitTransform implements Function<AggregatedData, AggregatedData> {
        private ConvertUnitTransform(final Optional<Unit> unit) {
            _unit = unit;
        }

        @Nonnull
        @Override
        public AggregatedData apply(final AggregatedData input) {
            boolean transformSamples = false;
            if (input == null) {
                throw new IllegalArgumentException("input cannot be null");
            }

            for (final Quantity quantity : input.getSamples()) {
                if (!quantity.getUnit().equals(_unit)) {
                    transformSamples = true;
                    break;
                }
            }

            final boolean transformValue = !input.getValue().getUnit().equals(_unit);

            // We don't need to transform anything
            if (!(transformValue || transformSamples)) {
                return input;
            } else {

                final List<Quantity> newDataSamples;
                if (transformSamples) {
                    newDataSamples = Lists.newArrayList();
                    for (final Quantity oldSample : input.getSamples()) {
                        newDataSamples.add(convertQuantity(oldSample));
                    }
                } else {
                    newDataSamples = input.getSamples();
                }
                // Build a new AggregatedData
                return AggregatedData.Builder.<AggregatedData, AggregatedData.Builder>clone(input)
                        .setSamples(newDataSamples)
                        .setValue(convertQuantity(input.getValue()))
                        .build();
            }
        }

        private Quantity convertQuantity(final Quantity oldSample) {
            if (oldSample.getUnit().equals(_unit)) {
                return oldSample;
            } else if (!oldSample.getUnit().isPresent()) {
                throw new IllegalArgumentException(
                        String.format(
                                "cannot convert old sample to %s; oldSample=%s",
                                _unit,
                                oldSample));
            } else {
                return new Quantity.Builder()
                        .setValue(_unit.get().convert(oldSample.getValue(), oldSample.getUnit().get()))
                        .setUnit(_unit.get())
                        .build();
            }
        }

        private final Optional<Unit> _unit;
    }
}
