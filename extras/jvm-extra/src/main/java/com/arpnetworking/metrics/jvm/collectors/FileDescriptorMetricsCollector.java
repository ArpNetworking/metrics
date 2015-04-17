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
import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.OperatingSystemMXBean;

/**
 * Collector class for JVM file descriptor metrics. Uses the Java Management API to get the metrics data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class FileDescriptorMetricsCollector implements JvmMetricsCollector {
// CHECKSTYLE.ON: FinalClass

    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new FileDescriptorMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final OperatingSystemMXBean bean = managementFactory.getOperatingSystemMXBean();
        if (bean instanceof UnixOperatingSystemMXBean) {
            final UnixOperatingSystemMXBean unixOsBean = (UnixOperatingSystemMXBean) bean;
            metrics.setGauge(OPEN_COUNT, unixOsBean.getOpenFileDescriptorCount());
            metrics.setGauge(MAX_COUNT, unixOsBean.getMaxFileDescriptorCount());
        }
    }

    /**
     * Protected constructor.
     */
    protected FileDescriptorMetricsCollector() {}

    private static final String OPEN_COUNT = String.join("/", ROOT_NAMESPACE, "file_descriptor", "open_count");
    private static final String MAX_COUNT = String.join("/", ROOT_NAMESPACE, "file_descriptor", "max_count");
}
