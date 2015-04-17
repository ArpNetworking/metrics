/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.metrics.jvm.collectors;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.metrics.jvm.ManagementFactory;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * Collector class for JVM memory usage metrics for each memory pool. Uses the Java Management API to get the metrics
 * data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
*/
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class NonHeapMemoryMetricsCollector implements JvmMetricsCollector {
// CHECKSTYLE.ON: FinalClass
    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new NonHeapMemoryMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final List<MemoryPoolMXBean> memoryPoolBeans = managementFactory.getMemoryPoolMXBeans();
        for (final MemoryPoolMXBean pool : memoryPoolBeans) {
            final MemoryUsage usage = pool.getUsage();
            metrics.setGauge(
                    String.join(
                            "/",
                            ROOT_NAMESPACE,
                            NON_HEAP_MEMORY,
                            MetricsUtil.convertToSnakeCase(pool.getName()),
                            MEMORY_USED),
                    usage.getUsed(),
                    Unit.BYTE
            );
            final long memoryMax = usage.getMax();
            if (memoryMax != -1) {
                metrics.setGauge(
                        String.join(
                                "/",
                                ROOT_NAMESPACE,
                                NON_HEAP_MEMORY,
                                MetricsUtil.convertToSnakeCase(pool.getName()),
                                MEMORY_MAX),
                        memoryMax,
                        Unit.BYTE
                );
            }
        }
    }

    /**
     * Protected constructor.
     */
    protected NonHeapMemoryMetricsCollector() {}

    private static final String MEMORY_USED = "used";
    private static final String MEMORY_MAX = "max";
    private static final String NON_HEAP_MEMORY = "non_heap_memory";
}
