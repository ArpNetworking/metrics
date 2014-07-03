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
package com.arpnetworking.test;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;
import java.util.UUID;

/**
 * Creates reasonable random instances of common data types for testing. This is
 * strongly preferred over mocking data type classes as mocking should be
 * reserved for defining behavior and not data.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TestBeanFactory {

    /**
     * Create a builder for pseudo-random <code>AggregatedData</code>.
     *
     * @return New builder for pseudo-random <code>AggregatedData</code>.
     */
    public static AggregatedData.Builder createAggregatedDataBuilder() {
        return new AggregatedData.Builder()
                .setStatistic(new MeanStatistic())
                .setService("service-" + UUID.randomUUID())
                .setHost("host-" + UUID.randomUUID())
                .setMetric("metric-" + UUID.randomUUID())
                .setValue(Double.valueOf(Math.random()))
                .setPeriodStart(DateTime.now())
                .setPeriod(Period.minutes(5))
                .setSamples(Lists.newArrayList(new Quantity(Math.random(), Optional.<Unit>absent())))
                .setPopulationSize(Long.valueOf((long) (Math.random() * 100)));
    }

    /**
     * Create a new reasonable pseudo-random <code>AggregatedData</code>.
     *
     * @return New reasonable pseudo-random <code>AggregatedData</code>.
     */
    public static AggregatedData createAggregatedData() {
        return createAggregatedDataBuilder().build();
    }

    /**
     * Create a new reasonable pseudo-random <code>Sample</code>.
     *
     * @return New reasonable pseudo-random <code>Sample</code>.
     */
    public static Quantity createSample() {
        return new Quantity(Math.random(), Optional.of(Unit.BIT));
    }

    /**
     * Create samples from an array of doubles.
     * @param values values
     * @return a list of samples
     */
    public static List<Quantity> createSamples(final List<Double> values) {
        return FluentIterable.from(Lists.newArrayList(values)).transform(CREATE_SAMPLE).toList();
    }

    private static final Function<Double, Quantity> CREATE_SAMPLE = new Function<Double, Quantity>() {
        @Override
        public Quantity apply(final Double input) {
            if (input == null) {
                throw new IllegalArgumentException("cannot create a sample from a null value");
            }
            return new Quantity(input.doubleValue(), Optional.fromNullable(Unit.MILLISECOND));
        }
    };

    private TestBeanFactory() {}
}
