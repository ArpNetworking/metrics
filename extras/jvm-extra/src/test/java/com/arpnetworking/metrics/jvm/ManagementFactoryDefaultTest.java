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
package com.arpnetworking.metrics.jvm;

import org.junit.Assert;
import org.junit.Test;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * Tests <code>ManagementFactoryDefault</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class ManagementFactoryDefaultTest {

    @Test
    public void testGetGarbageCollectorMXBeans() {
        final List<GarbageCollectorMXBean> beans = MANAGEMENT_FACTORY.getGarbageCollectorMXBeans();
        Assert.assertFalse(beans.isEmpty());
        Assert.assertNotNull(beans.get(0).getCollectionCount());
        Assert.assertNotNull(beans.get(0).getCollectionTime());
    }

    @Test
    public void testGetMemoryMXBean() {
        final MemoryMXBean bean = MANAGEMENT_FACTORY.getMemoryMXBean();
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getHeapMemoryUsage());
    }

    @Test
    public void testGetMemoryPoolMXBeans() {
        final List<MemoryPoolMXBean> beans = MANAGEMENT_FACTORY.getMemoryPoolMXBeans();
        Assert.assertFalse(beans.isEmpty());
        Assert.assertNotNull(beans.get(0).getUsage());
    }

    @Test
    public void testGetThreadMXBean() {
        final ThreadMXBean bean = MANAGEMENT_FACTORY.getThreadMXBean();
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getThreadCount());
        Assert.assertNotNull(bean.getDaemonThreadCount());
        Assert.assertNotNull(bean.getPeakThreadCount());
    }

    @Test
    public void testGetBufferPoolBeans() {
        final List<BufferPoolMXBean> beans = MANAGEMENT_FACTORY.getBufferPoolMXBeans();
        Assert.assertNotNull(beans);
        Assert.assertFalse(beans.isEmpty());
        Assert.assertNotNull(beans.get(0).getCount());
        Assert.assertNotNull(beans.get(0).getTotalCapacity());
        Assert.assertNotNull(beans.get(0).getMemoryUsed());
    }

    @Test
    public void testGetOperatingSystemMXBean() {
        final OperatingSystemMXBean bean = MANAGEMENT_FACTORY.getOperatingSystemMXBean();
        Assert.assertNotNull(bean);
    }

    private static final ManagementFactory MANAGEMENT_FACTORY = JvmMetricsRunnable.ManagementFactoryDefault.newInstance();
}
