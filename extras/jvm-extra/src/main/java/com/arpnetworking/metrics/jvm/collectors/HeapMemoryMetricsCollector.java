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

import java.lang.management.MemoryUsage;

/**
 * Collector class for JVM memory usage metrics. Uses the Java Management API to get the metrics data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class HeapMemoryMetricsCollector implements JvmMetricsCollector {
// CHECKSTYLE.ON: FinalClass
    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new HeapMemoryMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final MemoryUsage heapUsage = managementFactory.getMemoryMXBean().getHeapMemoryUsage();
        metrics.setGauge(HEAP_USED, heapUsage.getUsed(), Unit.BYTE);
        // Heap max may be -1 if undefined
        // http://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryUsage.html#getMax--
        final long heapMax = heapUsage.getMax();
        if (heapMax != -1) {
            metrics.setGauge(HEAP_MAX, heapMax, Unit.BYTE);
        }
    }

    /**
     * Protected constructor.
     */
    protected HeapMemoryMetricsCollector() {}

    private static final String HEAP_USED = String.join("/", ROOT_NAMESPACE, "heap_memory", "used");
    private static final String HEAP_MAX = String.join("/", ROOT_NAMESPACE, "heap_memory", "max");
}
