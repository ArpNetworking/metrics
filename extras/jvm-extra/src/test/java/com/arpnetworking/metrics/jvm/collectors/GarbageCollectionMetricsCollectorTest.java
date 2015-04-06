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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.lang.management.GarbageCollectorMXBean;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests the <code>GarbageCollectionMetricsCollector</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class GarbageCollectionMetricsCollectorTest {

    @Before
    public void setUp() {
        _metrics =  Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _gcBean1 = Mockito.mock(GarbageCollectorMXBean.class);
        _gcBean2 = Mockito.mock(GarbageCollectorMXBean.class);
    }

    @After
    public void tearDown() {
        _metrics = null;
        _managementFactory = null;
        _gcBean1 = null;
        _gcBean2 = null;
    }

    @Test
    public void testCollectWithNoGcBeans() {
        Mockito.doReturn(Collections.emptyList()).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verifyNoMoreInteractions(_metrics);
    }

    @Test
    public void testCollectWithSingleGcBean() {
        createMockBean(_gcBean1, "My Bean", 5L, 100L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_time", 100L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectWithMultipleGcBeans() {
        createMockBean(_gcBean1, "My Bean 1", 5L, 100L);
        createMockBean(_gcBean2, "My Bean 2", 10L, 400L);
        Mockito.doReturn(Arrays.asList(_gcBean1, _gcBean2)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean_1/collection_count", 5L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean_1/collection_time", 100L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean_1/collection_count_delta"), Mockito.anyLong());
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean_2/collection_count", 10L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean_2/collection_time", 400L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean_2/collection_count_delta"), Mockito.anyLong());
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionOnCollectionCount() {
        createMockBean(_gcBean1, "My Bean 1", 5L, 100L);
        createMockBean(_gcBean2, "My Bean 2", 0L, 400L);
        Mockito.doThrow(Exception.class).when(_gcBean2).getCollectionCount();
        Mockito.doReturn(Arrays.asList(_gcBean1, _gcBean2)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionOnCollectionTime() {
        createMockBean(_gcBean1, "My Bean 1", 5L, 100L);
        createMockBean(_gcBean2, "My Bean 2", 10L, 0L);
        Mockito.doThrow(Exception.class).when(_gcBean2).getCollectionTime();
        Mockito.doReturn(Arrays.asList(_gcBean1, _gcBean2)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test
    public void testCollectWithUndefinedValuesForCollectionCount() {
        createMockBean(_gcBean1, "My Bean", -1, 100L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics, Mockito.never())
                .setGauge(Matchers.eq("jvm/garbage_collector/my_bean/collection_count"), Matchers.anyLong());
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_time", 100L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectWithUndefinedValuesForCollectionTime() {
        createMockBean(_gcBean1, "My Bean", 5L, -1);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics, Mockito.never())
                .setGauge(
                        Matchers.eq("jvm/garbage_collector/my_bean/collection_time"),
                        Matchers.anyLong(),
                        Matchers.eq(Unit.MILLISECOND));
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectCollectionCountDeltaMultipleCalls() {
        createMockBean(_gcBean1, "My Bean", 5L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .setGauge(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
        createMockBean(_gcBean1, "My Bean", 7L, 12L);
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 7L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_time", 12L, Unit.MILLISECOND);
        Mockito.verify(_metrics).incrementCounter("jvm/garbage_collector/my_bean/collection_count_delta", 2L);
    }

    @Test
    public void testCollectWithCollectionCountDelta() {
        createMockBean(_gcBean1, "My Bean", 3L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        createMockBean(_gcBean1, "My Bean", 5L, 10L);
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 3L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics, Mockito.times(2))
                .setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics).incrementCounter("jvm/garbage_collector/my_bean/collection_count_delta", 2L);
    }

    @Test
    public void testCollectCollectionCountDeltaWithLastCountUndefined() {
        createMockBean(_gcBean1, "My Bean", -1L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        createMockBean(_gcBean1, "My Bean", 5L, 10L);
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics, Mockito.times(2))
                .setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectCollectionCountDeltaWithCurrentCountUndefined() {
        createMockBean(_gcBean1, "My Bean", 3L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        createMockBean(_gcBean1, "My Bean", -1L, 10L);
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 3L);
        Mockito.verify(_metrics, Mockito.times(2))
                .setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectCollectionCountDeltaWithNoLastValue() {
        createMockBean(_gcBean1, "My Bean", 5L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics, Mockito.never())
                .incrementCounter(Mockito.eq("jvm/garbage_collector/my_bean/collection_count_delta"), Mockito.anyLong());
    }

    @Test
    public void testCollectCollectionCountDeltaWithNegativeValue() {
        createMockBean(_gcBean1, "My Bean", 7L, 10L);
        Mockito.doReturn(Arrays.asList(_gcBean1)).when(_managementFactory).getGarbageCollectorMXBeans();
        final GarbageCollectionMetricsCollector collector =
                (GarbageCollectionMetricsCollector) GarbageCollectionMetricsCollector.newInstance();
        collector.collect(_metrics, _managementFactory);
        createMockBean(_gcBean1, "My Bean", 5L, 10L);
        collector.collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 7L);
        Mockito.verify(_metrics).setGauge("jvm/garbage_collector/my_bean/collection_count", 5L);
        Mockito.verify(_metrics, Mockito.times(2))
                .setGauge("jvm/garbage_collector/my_bean/collection_time", 10L, Unit.MILLISECOND);
        Mockito.verify(_metrics).incrementCounter("jvm/garbage_collector/my_bean/collection_count_delta", -2L);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionWithGettingBeans() {
        Mockito.doThrow(Exception.class).when(_managementFactory).getGarbageCollectorMXBeans();
        GarbageCollectionMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    private void createMockBean(
            final GarbageCollectorMXBean gcBean,
            final String name,
            final long collectionCount,
            final long collectionTime) {
        Mockito.doReturn(name).when(gcBean).getName();
        Mockito.doReturn(collectionCount).when(gcBean).getCollectionCount();
        Mockito.doReturn(collectionTime).when(gcBean).getCollectionTime();
    }

    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private GarbageCollectorMXBean _gcBean1 = null;
    private GarbageCollectorMXBean _gcBean2 = null;
}
