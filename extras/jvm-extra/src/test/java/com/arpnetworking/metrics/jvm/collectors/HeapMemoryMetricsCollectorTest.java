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

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Tests <code>MemoryMetricsCollector</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class HeapMemoryMetricsCollectorTest {

    @Before
    public void setUp() {
        _metrics =  Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _memoryMXBean = Mockito.mock(MemoryMXBean.class);
        Mockito.doReturn(_memoryMXBean).when(_managementFactory).getMemoryMXBean();
        _memoryUsage = Mockito.mock(MemoryUsage.class);
    }

    @After
    public void tearDown() {
        _metrics = null;
        _managementFactory = null;
        _memoryMXBean = null;
        _memoryUsage = null;
    }

    @Test
    public void testCollect() {
        createMockBean(_memoryUsage, 10L, 100L);
        Mockito.when(_memoryMXBean.getHeapMemoryUsage()).thenReturn(_memoryUsage);
        HeapMemoryMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/heap_memory/used", 10L, Unit.BYTE);
        Mockito.verify(_metrics).setGauge("jvm/heap_memory/max", 100L, Unit.BYTE);
    }

    @Test
    public void testCollectWithUndefinedMax() {
        createMockBean(_memoryUsage, 10L, -1);
        Mockito.when(_memoryMXBean.getHeapMemoryUsage()).thenReturn(_memoryUsage);
        HeapMemoryMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/heap_memory/used", 10L, Unit.BYTE);
        Mockito.verify(_metrics, Mockito.never()).setGauge(Matchers.eq("jvm/heap_memory/max"), Mockito.anyLong(), Matchers.eq(Unit.BYTE));
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionOnGetUsed() {
        createMockBean(_memoryUsage, 0L, 100L);
        Mockito.doThrow(Exception.class).when(_memoryUsage).getUsed();
        Mockito.when(_memoryMXBean.getHeapMemoryUsage()).thenReturn(_memoryUsage);
        HeapMemoryMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionOnGetMax() {
        createMockBean(_memoryUsage, 10L, 0L);
        Mockito.doThrow(Exception.class).when(_memoryUsage).getMax();
        Mockito.when(_memoryMXBean.getHeapMemoryUsage()).thenReturn(_memoryUsage);
        HeapMemoryMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test(expected = Exception.class)
    public void testCollectWithExceptionOnGetMemoryMXBean() {
        Mockito.doThrow(Exception.class).when(_managementFactory).getMemoryMXBean();
        HeapMemoryMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    @Test
    public void testCollectWithExceptionOnGetMemoryUsage() {
        Mockito.doThrow(Exception.class).when(_memoryMXBean).getHeapMemoryUsage();
        Mockito.verifyNoMoreInteractions(_metrics);
    }

    private void createMockBean(
            final MemoryUsage usage,
            final long used,
            final long max) {
        Mockito.doReturn(used).when(usage).getUsed();
        Mockito.doReturn(max).when(usage).getMax();
    }

    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private MemoryMXBean _memoryMXBean = null;
    private MemoryUsage _memoryUsage = null;
}
