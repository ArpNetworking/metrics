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
import org.apache.commons.math3.random.RandomDataGenerator;

/**
 * Generates a multiple number of metrics by decorating another generator,
 * the count of metrics generated is determined by a Gaussian distribution.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GaussianCountMetricGenerator implements MetricGenerator {
    /**
     * Public constructor.
     *
     * @param mu Mean of the distribution.
     * @param sigma Standard deviation of the distribution.
     * @param wrapped The wrapped generator.
     */
    public GaussianCountMetricGenerator(final double mu, final double sigma, final MetricGenerator wrapped) {
        _mu = mu;
        _sigma = sigma;
        _wrapped = wrapped;
        _generator = new RandomDataGenerator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate(final Metrics metrics) {
        final int count = (int) _generator.nextGaussian(_mu, _sigma);
        for (int x = 0; x < count; x++) {
            _wrapped.generate(metrics);
        }
    }

    private final double _mu;
    private final double _sigma;
    private final MetricGenerator _wrapped;
    private final RandomDataGenerator _generator;
}
