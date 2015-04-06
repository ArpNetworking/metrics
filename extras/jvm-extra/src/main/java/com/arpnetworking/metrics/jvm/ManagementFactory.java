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

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * This interface defines the various methods to get JVM related data. This interface exists only to facilitate
 * testing and the clients should never need to implement this.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public interface ManagementFactory {

    /**
     * Gets the <code>List</code> of <code>GarbageCollectorMXBean</code>.
     *
     * @return A <code>List</code> of <code>GarbageCollectorMXBean</code>.
     */
    List<GarbageCollectorMXBean> getGarbageCollectorMXBeans();

    /**
     * Gets the <code>MemoryMXBean</code>.
     *
     * @return An instance of <code>MemoryMXBean</code>.
     */
    MemoryMXBean getMemoryMXBean();

    /**
     * Gets the <code>List</code> of <code>MemoryPoolMXBean</code>.
     *
     * @return A <code>List</code> of <code>MemoryPoolMXBean</code>.
     */
    List<MemoryPoolMXBean> getMemoryPoolMXBeans();

    /**
     * Gets the <code>ThreadMXBean</code>.
     *
     * @return An instance of <code>ThreadMXBean</code>.
     */
    ThreadMXBean getThreadMXBean();

    /**
     * Gets the <code>List</code> of <code>BufferPoolMXBean</code>.
     *
     * @return A <code>List</code> of <code>BufferPoolMXBean</code>.
     */
    List<BufferPoolMXBean> getBufferPoolMXBeans();

    /**
     * Gets the <code>OperatingSystemMXBean</code>.
     *
     * @return An instance of <code>OperatingSystemMXBean</code>.
     */
    OperatingSystemMXBean getOperatingSystemMXBean();
}



