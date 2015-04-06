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

import java.lang.management.GarbageCollectorMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Collector class for JVM garbage collection metrics. Uses the Java Management API to get the metrics data.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class GarbageCollectionMetricsCollector implements JvmMetricsCollector {
// CHECKSTYLE.ON: FinalClass

    /**
     * Creates a new instance of <code>JvmMetricsCollector</code>.
     *
     * @return An instance of <code>JvmMetricsCollector</code>
     */
    public static JvmMetricsCollector newInstance() {
        return new GarbageCollectionMetricsCollector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final Metrics metrics, final ManagementFactory managementFactory) {
        final List<GarbageCollectorMXBean> gcBeans = managementFactory.getGarbageCollectorMXBeans();
        for (final GarbageCollectorMXBean bean : gcBeans) {
            final String beanName = bean.getName();
            final long currentCollectionCount = bean.getCollectionCount();
            // Collection count may be -1 if undefined
            // http://docs.oracle.com/javase/8/docs/api/java/lang/management/GarbageCollectorMXBean.html#getCollectionCount--
            if (currentCollectionCount != -1) {
                metrics.setGauge(
                        String.join(
                                "/",
                                ROOT_NAMESPACE,
                                GARBAGE_COLLECTOR,
                                MetricsUtil.convertToSnakeCase(beanName),
                                COLLECTION_COUNT),
                        currentCollectionCount
                );
            }
            final Optional<Long> collectionCountDelta = getAndUpdateCollectionCountDelta(beanName, currentCollectionCount);
            if (collectionCountDelta.isPresent()) {
                metrics.incrementCounter(
                        String.join(
                                "/",
                                ROOT_NAMESPACE,
                                GARBAGE_COLLECTOR,
                                MetricsUtil.convertToSnakeCase(beanName),
                                COLLECTION_COUNT_DELTA),
                        collectionCountDelta.get()
                );
            }
            // Collection time may be -1 if undefined
            // http://docs.oracle.com/javase/8/docs/api/java/lang/management/GarbageCollectorMXBean.html#getCollectionTime--
            final long collectionTime = bean.getCollectionTime();
            if (collectionTime != -1) {
                metrics.setGauge(
                        String.join(
                                "/",
                                ROOT_NAMESPACE,
                                GARBAGE_COLLECTOR,
                                MetricsUtil.convertToSnakeCase(beanName),
                                COLLECTION_TIME),
                        collectionTime,
                        Unit.MILLISECOND
                );
            }
        }
    }

    private Optional<Long> getAndUpdateCollectionCountDelta(final String beanName, final long currentValue) {
        final long lastValue = _lastCollectionCountMap.getOrDefault(beanName, -1L);
        _lastCollectionCountMap.put(beanName, currentValue);
        //-1 signifies undefined.
        //If there is no previous value or the previous value is -1 or the current value is -1, return Optional.empty().
        if (lastValue == -1 || currentValue == -1) {
            return Optional.empty();
        }
        return Optional.of(currentValue - lastValue);
    }

    /**
     * Protected constructor.
     */
    protected GarbageCollectionMetricsCollector() {}

    // CHECKSTYLE.OFF: IllegalInstantiation - Needed, since we dont use Guava
    private Map<String, Long> _lastCollectionCountMap = new HashMap<>();
    // CHECKSTYLE.ON: IllegalInstantiation

    private static final String COLLECTION_COUNT = "collection_count";
    private static final String COLLECTION_TIME = "collection_time";
    private static final String COLLECTION_COUNT_DELTA = "collection_count_delta";
    private static final String GARBAGE_COLLECTOR = "garbage_collector";
}
