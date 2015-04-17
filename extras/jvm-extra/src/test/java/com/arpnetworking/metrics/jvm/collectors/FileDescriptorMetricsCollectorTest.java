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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.management.OperatingSystemMXBean;

/**
 * Tests <code>FileDescriptorMetricsCollector</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class FileDescriptorMetricsCollectorTest {

    @Before
    public void setUp() {
        _metrics =  Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _unixOsBean = Mockito.mock(UnixOperatingSystemMXBean.class);
        Mockito.doReturn(_unixOsBean).when(_managementFactory).getOperatingSystemMXBean();
    }

    @After
    public void tearDown() {
        _metrics = null;
        _managementFactory = null;
        _unixOsBean = null;
    }

    @Test
    public void testCollectWhenNotInstanceOfUnixOperatingSystemMXBean() {
        final OperatingSystemMXBean osBean = Mockito.mock(OperatingSystemMXBean.class);
        Mockito.doReturn(osBean).when(_managementFactory).getOperatingSystemMXBean();
        FileDescriptorMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verifyZeroInteractions(_metrics);
    }

    @Test
    public void testCollect() {
        createMockBean(3, 4);
        FileDescriptorMetricsCollector.newInstance().collect(_metrics, _managementFactory);
        Mockito.verify(_metrics).setGauge("jvm/file_descriptor/open_count", 3);
        Mockito.verify(_metrics).setGauge("jvm/file_descriptor/max_count", 4);
    }

    @Test(expected = Exception.class)
    public void testCollectWithException() {
        Mockito.doThrow(Exception.class).when(_managementFactory).getOperatingSystemMXBean();
        FileDescriptorMetricsCollector.newInstance().collect(_metrics, _managementFactory);
    }

    private void createMockBean(
            final long openCount,
            final long maxCount) {
        Mockito.doReturn(openCount).when(_unixOsBean).getOpenFileDescriptorCount();
        Mockito.doReturn(maxCount).when(_unixOsBean).getMaxFileDescriptorCount();
    }

    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private UnixOperatingSystemMXBean _unixOsBean = null;
}
