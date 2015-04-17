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

import java.lang.management.BufferPoolMXBean;
import java.util.List;

/**
 * Collector class for JVM buffer pool metrics. Uses the Java Management API to get the metrics data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class BufferPoolMetricsCollector implements JvmMetricsCollector{
// CHECKSTYLE.ON: FinalClass

    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new BufferPoolMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final List<BufferPoolMXBean> bufferPoolBeans = managementFactory.getBufferPoolMXBeans();
        for (final BufferPoolMXBean pool : bufferPoolBeans) {
            metrics.setGauge(
                    String.join(
                            "/",
                            ROOT_NAMESPACE,
                            BUFFER_POOL,
                            MetricsUtil.convertToSnakeCase(pool.getName()),
                            COUNT),
                    pool.getCount()
            );
            metrics.setGauge(
                    String.join(
                            "/",
                            ROOT_NAMESPACE,
                            BUFFER_POOL,
                            MetricsUtil.convertToSnakeCase(pool.getName()),
                            TOTAL_CAPACITY),
                    pool.getTotalCapacity(),
                    Unit.BYTE
            );
            final long memoryUsed = pool.getMemoryUsed();
            if (memoryUsed != -1) {
                metrics.setGauge(
                        String.join(
                                "/",
                                ROOT_NAMESPACE,
                                BUFFER_POOL,
                                MetricsUtil.convertToSnakeCase(pool.getName()),
                                MEMORY_USED),
                        memoryUsed,
                        Unit.BYTE
                );
            }
        }
    }

    /**
     * Protected constructor.
     */
    protected BufferPoolMetricsCollector() {}

    private static final String COUNT = "count";
    private static final String TOTAL_CAPACITY = "total_capacity";
    private static final String MEMORY_USED = "memory_used";
    private static final String BUFFER_POOL = "buffer_pool";
}
