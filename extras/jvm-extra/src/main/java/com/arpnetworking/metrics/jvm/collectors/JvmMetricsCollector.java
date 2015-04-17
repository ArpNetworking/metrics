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

/**
 * Interface for collecting JVM metrics.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public interface JvmMetricsCollector {
    /**
     * Collects JVM metrics and writes to the given metrics instance.
     *
     * @param metrics An instance of <code>Metrics</code> that the collected metrics will be written to.
     * @param managementFactory An instance of <code>ManagementFactory</code>.
     */
    void collect(final Metrics metrics, final ManagementFactory managementFactory);

    /**
     * The prefix for the jvm metrics namespace.
     */
    String ROOT_NAMESPACE = "jvm";
}
