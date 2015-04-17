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
import com.arpnetworking.metrics.jvm.ManagementFactory;

import java.lang.management.ThreadMXBean;

/**
 * Collector class for JVM threads metrics. Uses the Java Management API to get the metrics data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class ThreadMetricsCollector implements JvmMetricsCollector {
// CHECKSTYLE.ON: FinalClass
    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new ThreadMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final ThreadMXBean threadBean = managementFactory.getThreadMXBean();
        metrics.setGauge(THREAD_COUNT, threadBean.getThreadCount());
        metrics.setGauge(DAEMON_THREAD_COUNT, threadBean.getDaemonThreadCount());
        metrics.setGauge(PEAK_THREAD_COUNT, threadBean.getPeakThreadCount());
    }

    /**
     * Protected constructor.
     */
    protected ThreadMetricsCollector() {}

    private static final String THREAD_COUNT = String.join("/", ROOT_NAMESPACE, "threads", "thread_count");
    private static final String DAEMON_THREAD_COUNT = String.join("/", ROOT_NAMESPACE, "threads", "daemon_thread_count");
    private static final String PEAK_THREAD_COUNT = String.join("/", ROOT_NAMESPACE, "threads", "peak_thread_count");
}
