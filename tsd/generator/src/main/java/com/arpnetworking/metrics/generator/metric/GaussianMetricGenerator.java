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
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.concurrent.TimeUnit;

/**
 * Generates a timer with a Gaussian distribution.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GaussianMetricGenerator implements MetricGenerator {
    /**
     * Public constructor.
     *
     * @param mu The mean of the distribution.
     * @param sigma The standard deviation of the distribution.
     * @param nameGenerator The name generator to name the metric created.
     */
    public GaussianMetricGenerator(final double mu, final double sigma, final NameGenerator nameGenerator) {
        _mu = mu;
        _sigma = sigma;
        _nameGenerator = nameGenerator;
        _generator = new RandomDataGenerator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate(final Metrics metrics) {
        metrics.setTimer(
                _nameGenerator.getName(),
                Math.round(_generator.nextGaussian(_mu, _sigma)),
                TimeUnit.MILLISECONDS);
    }

    private final double _mu;
    private final double _sigma;
    private final NameGenerator _nameGenerator;
    private final RandomDataGenerator _generator;
}
