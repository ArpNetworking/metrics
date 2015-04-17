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

import java.lang.management.BufferPoolMXBean;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests <code>BufferPoolMetricsCollector</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class BufferPoolMetricsCollectorTest {

    @Before
    public void setUp() {
        _metrics =  Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _bufferPoolMXBean1 = Mockito.mock(BufferPoolMXBean.class);
        _bufferPoolMXBean2 = Mockito.mock(BufferPoolMXBean.class);
    }

    @After
    public void tearDown() {
        _metrics = null;
        _managementFactory = null;
        _bufferPoolMXBean1 = null;
        _bufferPoolMXBean2 = null;
    }

    @Test
    public void testCollectWithNoPoolBeans() {
        Mockito.doReturn(Collections.emptyList()).when(_managementFactory).getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verifyNoMoreInteractions(_metrics);
    }

    @Test
    public void testCollectWithSingleBeans() {
        createMockBean(_bufferPoolMXBean1, "My Bean 1", 2L, 10L, 100L);
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1)).when(_managementFactory).getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/count", 2L);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/total_capacity", 10L, Unit.BYTE);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/memory_used", 100L, Unit.BYTE);
    }

    @Test
    public void testCollectWithMultipleBeans() {
        createMockBean(_bufferPoolMXBean1, "My Bean 1", 2L, 10L, 100L);
        createMockBean(_bufferPoolMXBean2, "My Bean 2", 3L, 30L, 400L);
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1, _bufferPoolMXBean2))
                .when(_managementFactory)
                .getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/count", 2L);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/total_capacity", 10L, Unit.BYTE);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/memory_used", 100L, Unit.BYTE);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_2/count", 3L);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_2/total_capacity", 30L, Unit.BYTE);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_2/memory_used", 400L, Unit.BYTE);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionInGettingPools() {
        Mockito.doThrow(Exception.class).when(_managementFactory).getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionInGetCount() {
        createMockBean(_bufferPoolMXBean2, "My Bean 2", 2L, 10L, 100L);
        Mockito.doThrow(Exception.class).when(_bufferPoolMXBean1).getCount();
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1, _bufferPoolMXBean2))
                .when(_managementFactory)
                .getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionInGetTotalCapacity() {
        createMockBean(_bufferPoolMXBean1, "My Bean 1", 2L, 10L, 100L);
        createMockBean(_bufferPoolMXBean2, "My Bean 2", 3L, 30L, 400L);
        Mockito.doThrow(Exception.class).when(_bufferPoolMXBean2).getTotalCapacity();
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1, _bufferPoolMXBean2))
                .when(_managementFactory)
                .getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionInGetMemoryUsed() {
        createMockBean(_bufferPoolMXBean1, "My Bean 1", 2L, 10L, 100L);
        createMockBean(_bufferPoolMXBean2, "My Bean 2", 3L, 30L, 400L);
        Mockito.doThrow(Exception.class).when(_bufferPoolMXBean1).getMemoryUsed();
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1, _bufferPoolMXBean2))
                .when(_managementFactory)
                .getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test
    public void testCollectWithUndefinedMax() {
        createMockBean(_bufferPoolMXBean1, "My Bean 1", 2L, 10L, -1);
        Mockito.doReturn(Arrays.asList(_bufferPoolMXBean1)).when(_managementFactory).getBufferPoolMXBeans();
        BufferPoolMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/count", 2L);
        Mockito.verify(_metrics).setGauge("jvm/buffer_pool/my_bean_1/total_capacity", 10L, Unit.BYTE);
        Mockito.verify(_metrics, Mockito.never())
                .setGauge(Matchers.eq("jvm/buffer_pool/my_bean_1/memory_used"), Matchers.anyLong(), Matchers.eq(Unit.BYTE));
    }

    private void createMockBean(
            final BufferPoolMXBean pool,
            final String name,
            final long count,
            final long totalCapacity,
            final long memoryUsed) {
        Mockito.doReturn(count).when(pool).getCount();
        Mockito.doReturn(totalCapacity).when(pool).getTotalCapacity();
        Mockito.doReturn(memoryUsed).when(pool).getMemoryUsed();
        Mockito.doReturn(name).when(pool).getName();
    }

    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private BufferPoolMXBean _bufferPoolMXBean1 = null;
    private BufferPoolMXBean _bufferPoolMXBean2 = null;
}
