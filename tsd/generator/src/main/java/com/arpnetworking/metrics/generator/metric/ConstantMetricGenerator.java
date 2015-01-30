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

package com.arpnetworking.metrics.generator.metric;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.generator.name.NameGenerator;

import java.util.concurrent.TimeUnit;

/**
 * Generates a metric sample with a constant value.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConstantMetricGenerator implements MetricGenerator {
    /**
     * Public constructor.
     *
     * @param value The value to generate.
     * @param nameGenerator The name generator to name the metric created.
     */
    public ConstantMetricGenerator(final long value, final NameGenerator nameGenerator) {
        _value = value;
        _nameGenerator = nameGenerator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate(final Metrics metrics) {
        metrics.setTimer(
                _nameGenerator.getName(),
                _value,
                TimeUnit.MILLISECONDS);
    }

    private final long _value;
    private final NameGenerator _nameGenerator;
}
