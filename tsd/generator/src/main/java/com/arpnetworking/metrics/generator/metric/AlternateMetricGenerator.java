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
 * Generates a timer that alternates between a high and a low value.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AlternateMetricGenerator implements MetricGenerator {
    /**
     * Public constructor.
     *
     * @param high The high value.
     * @param low The low value.
     * @param nameGenerator The name generator to name the metric created.
     */
    public AlternateMetricGenerator(final double high, final double low, final NameGenerator nameGenerator) {
        _high = high;
        _low = low;
        _nameGenerator = nameGenerator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate(final Metrics metrics) {
        _odd = !_odd;
        if (_odd) {
            metrics.setTimer(_nameGenerator.getName(), (long) _high, TimeUnit.MILLISECONDS);
        } else {
            metrics.setTimer(_nameGenerator.getName(), (long) _low, TimeUnit.MILLISECONDS);
        }
    }

    private boolean _odd = true;
    private final double _high;
    private final double _low;
    private final NameGenerator _nameGenerator;
}
