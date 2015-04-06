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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.management.ThreadMXBean;

/**
 * Tests the <code>ThreadMetricsCollector</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class ThreadMetricsCollectorTest {

    @Before
    public void setUp() {
        _metrics =  Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _threadMXBean = Mockito.mock(ThreadMXBean.class);
        Mockito.doReturn(_threadMXBean).when(_managementFactory).getThreadMXBean();
    }

    @After
    public void tearDown() {
        _metrics = null;
        _managementFactory = null;
        _threadMXBean = null;
    }

    @Test
    public void testCollect() {
        createMockBean(10, 3, 4);
        ThreadMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/threads/thread_count", 10);
        Mockito.verify(_metrics).setGauge("jvm/threads/daemon_thread_count", 3);
        Mockito.verify(_metrics).setGauge("jvm/threads/peak_thread_count", 4);
    }

    @Test(expected = Exception.class)
    public void testCollectWithException() {
        Mockito.doThrow(Exception.class).when(_managementFactory).getThreadMXBean();
        ThreadMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    private void createMockBean(
            final int threadCount,
            final int daemonThreadCount,
            final int peakThreadCount) {
        Mockito.doReturn(threadCount).when(_threadMXBean).getThreadCount();
        Mockito.doReturn(daemonThreadCount).when(_threadMXBean).getDaemonThreadCount();
        Mockito.doReturn(peakThreadCount).when(_threadMXBean).getPeakThreadCount();
    }

    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private ThreadMXBean _threadMXBean = null;
}
