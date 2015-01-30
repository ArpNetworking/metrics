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

/**
 * Generates a fixed number of samples for a metric.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConstantCountMetricGenerator implements MetricGenerator {
    /**
     * Public constructor.
     *
     * @param count The number of samples to generate.
     * @param wrapped The wrapped generator.
     */
    public ConstantCountMetricGenerator(final int count, final MetricGenerator wrapped) {
        _count = count;
        _wrapped = wrapped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate(final Metrics metrics) {
        for (int x = 0; x < _count; x++) {
            _wrapped.generate(metrics);
        }
    }

    private final MetricGenerator _wrapped;
    private final int _count;
}
