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

package com.arpnetworking.metrics.generator.uow;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.generator.metric.MetricGenerator;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Generates a unit of work from a <code>List</code> of <code>MetricGenerator</code>.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class UnitOfWorkGenerator {
    /**
     * Public constructor.
     *
     * @param metricGenerators <code>List</code> of <code>MetricGenerator</code> to use to generate the unit of work.
     */
    public UnitOfWorkGenerator(final List<MetricGenerator> metricGenerators) {
        _metricGenerators = Lists.newArrayList(metricGenerators);
    }

    /**
     * Generates a unit of work.
     *
     * @param metricsFactory Metrics factory to generate the unit of work on.
     */
    public void generate(final MetricsFactory metricsFactory) {
        try (final Metrics metrics = metricsFactory.create()) {
            for (final MetricGenerator generator : _metricGenerators) {
                generator.generate(metrics);
            }
        }
    }

    private final List<MetricGenerator> _metricGenerators;
}

