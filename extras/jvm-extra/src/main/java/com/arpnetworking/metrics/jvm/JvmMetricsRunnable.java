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

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.jvm.collectors.BufferPoolMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.FileDescriptorMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.GarbageCollectionMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.HeapMemoryMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.JvmMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.NonHeapMemoryMetricsCollector;
import com.arpnetworking.metrics.jvm.collectors.ThreadMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of <code>Runnable</code> that collects all JVM metrics each time its run.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
// CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
public class JvmMetricsRunnable implements Runnable {
// CHECKSTYLE.ON: FinalClass

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Metrics metrics = null;
        try {
            metrics = _metricsFactory.create();
            for (final JvmMetricsCollector collector : _collectorsEnabled) {
                collector.collect(metrics, _managementFactory);
            }
            // CHECKSTYLE.OFF: IllegalCatch - No checked exceptions here
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            handleException(e);
        } finally {
            try {
                metrics.close();
            // CHECKSTYLE.OFF: IllegalCatch - No checked exceptions here
            } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
                handleException(e);
            }
        }
    }

    /**
     * Handles exceptions based on a boolean flag. If the flag is true, it logs and swallows the exception, else it will
     * throw.
     *
     * @param e An instance of <code>RuntimeException</code>.
     */
    protected void handleException(final Exception e) {
        if (_swallowException) {
            LOGGER.warn("JVM metrics collection failed.", e);
        } else {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private JvmMetricsRunnable(final Builder builder) {
        _metricsFactory = builder._metricsFactory;
        _managementFactory = builder._managementFactory;
        _swallowException = builder._swallowException;
        if (builder._collectGarbageCollectionMetrics) {
            _collectorsEnabled.add(builder._garbageCollectionMetricsCollector);
        }
        if (builder._collectHeapMemoryMetrics) {
            _collectorsEnabled.add(builder._heapMemoryMetricsCollector);
        }
        if (builder._collectNonHeapMemoryMetrics) {
            _collectorsEnabled.add(builder._nonHeapMemoryMetricsCollector);
        }
        if (builder._collectThreadMetrics) {
            _collectorsEnabled.add(builder._threadMetricsCollector);
        }
        if (builder._collectBufferPoolMetrics) {
            _collectorsEnabled.add(builder._bufferPoolMetricsCollector);
        }
        if (builder._collectFileDescriptorMetrics) {
            _collectorsEnabled.add(builder._fileDescriptorMetricsCollector);
        }
    }

    private final MetricsFactory _metricsFactory;
    private final ManagementFactory _managementFactory;
    private final boolean _swallowException;
    private final List<JvmMetricsCollector> _collectorsEnabled = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmMetricsRunnable.class);

    /**
     * Builder for <code>JvmMetricsRunnable</code>.
     *
     * @author Deepika Misra (deepika at groupon dot com)
     */
    // CHECKSTYLE.OFF: FinalClass - Allow clients to inherit from this.
    public static class Builder {
    // CHECKSTYLE.ON: FinalClass

        /**
         * Builds an instance of <code>JvmMetricsRunnable</code>.
         *
         * @return An instance of <code>JvmMetricsRunnable</code>.
         */
        public JvmMetricsRunnable build() {
            if (_metricsFactory == null) {
                throw new IllegalArgumentException("MetricsFactory cannot be null.");
            }
            if (_managementFactory == null) {
                throw new IllegalArgumentException("ManagementFactory cannot be null.");
            }
            if (_swallowException == null) {
                throw new IllegalArgumentException("SwallowException cannot be null.");
            }
            if (_collectNonHeapMemoryMetrics == null) {
                throw new IllegalArgumentException("CollectNonHeapMemoryMetrics cannot be null.");
            }
            if (_collectHeapMemoryMetrics == null) {
                throw new IllegalArgumentException("CollectHeapMemoryMetrics cannot be null.");
            }
            if (_collectThreadMetrics == null) {
                throw new IllegalArgumentException("CollectThreadMetrics cannot be null.");
            }
            if (_collectGarbageCollectionMetrics == null) {
                throw new IllegalArgumentException("CollectGarbageCollectionMetrics cannot be null.");
            }
            if (_collectBufferPoolMetrics == null) {
                throw new IllegalArgumentException("CollectBufferPoolMetrics cannot be null.");
            }
            if (_collectFileDescriptorMetrics == null) {
                throw new IllegalArgumentException("CollectFileDescriptorMetrics cannot be null.");
            }
            if (_nonHeapMemoryMetricsCollector == null) {
                throw new IllegalArgumentException("NonHeapMemoryMetricsCollector cannot be null.");
            }
            if (_heapMemoryMetricsCollector == null) {
                throw new IllegalArgumentException("HeapMemoryMetricsCollector cannot be null.");
            }
            if (_threadMetricsCollector == null) {
                throw new IllegalArgumentException("ThreadMetricsCollector cannot be null.");
            }
            if (_garbageCollectionMetricsCollector == null) {
                throw new IllegalArgumentException("GarbageCollectionMetricsCollector cannot be null.");
            }
            if (_bufferPoolMetricsCollector == null) {
                throw new IllegalArgumentException("BufferPoolMetricsCollector cannot be null.");
            }
            if (_fileDescriptorMetricsCollector == null) {
                throw new IllegalArgumentException("FileDescriptorMetricsCollector cannot be null.");
            }
            return new JvmMetricsRunnable(this);
        }

        /**
         * Set the <code>MetricsFactory</code> instance. Required. Cannot be null.
         *
         * @param value The value for the <code>MetricsFactory</code> instance.
         * @return This <code>Builder</code> instance.
         */
        public Builder setMetricsFactory(final MetricsFactory value) {
            _metricsFactory = value;
            return this;
        }

        /**
         * Set the flag indicating if any exception caught during the process of metrics collection should be logged and
         * swallowed. Optional. Defaults to true. True indicates that the exception will be logged and swallowed.
         *
         * @param value The value for the <code>Boolean</code> instance.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSwallowException(final Boolean value) {
            _swallowException = value;
            return this;
        }

        /**
         * Set the flag indicating if Heap Memory metrics should be collected. A true value indicates that these
         * metrics need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectHeapMemoryMetrics(final Boolean value) {
            _collectHeapMemoryMetrics = value;
            return this;
        }

        /**
         * Set the flag indicating if Non-Heap Memory metrics should be collected. A true value indicates that these
         * metrics need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectNonHeapMemoryMetrics(final Boolean value) {
            _collectNonHeapMemoryMetrics = value;
            return this;
        }


        /**
         * Set the flag indicating if Thread metrics should be collected. A true value indicates that these metrics
         * need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectThreadMetrics(final Boolean value) {
            _collectThreadMetrics = value;
            return this;
        }

        /**
         * Set the flag indicating if Garbage Collection metrics should be collected. A true value indicates that these
         * metrics need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectGarbageCollectionMetrics(final Boolean value) {
            _collectGarbageCollectionMetrics = value;
            return this;
        }

        /**
         * Set the flag indicating if Buffer Pool metrics should be collected. A true value indicates that these
         * metrics need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectBufferPoolMetrics(final Boolean value) {
            _collectBufferPoolMetrics = value;
            return this;
        }

        /**
         * Set the flag indicating if File Descriptor metrics should be collected. A true value indicates that these
         * metrics need to be collected. Optional. Defaults to true.
         *
         * @param value A <code>Boolean</code> value.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCollectFileDescriptorMetrics(final Boolean value) {
            _collectFileDescriptorMetrics = value;
            return this;
        }

        /**
         * Set the <code>ManagementFactory</code> instance. Defaults to an instance of
         * <code>ManagementFactoryDefault</code>. This is for testing purposes only and should never be used by clients.
         *
         * @param value The value for the <code>ManagementFactory</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setManagementFactory(final ManagementFactory value) {
            _managementFactory = value;
            return this;
        }

        /**
         * Set the <code>HeapMemoryMetricsCollector</code>. Defaults to an instance of
         * <code>HeapMemoryMetricsCollector</code>. This is for testing purposes only and should never be used by
         * clients.
         *
         * @param value A <code>HeapMemoryMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setHeapMemoryMetricsCollector(final JvmMetricsCollector value) {
            _heapMemoryMetricsCollector = value;
            return this;
        }

        /**
         * Set the <code>NonHeapMemoryMetricsCollector</code>. Defaults to an instance of
         * <code>NonHeapMemoryMetricsCollector</code>. This is for testing purposes only and should never be used by
         * clients.
         *
         * @param value A <code>NonHeapMemoryMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setNonHeapMemoryMetricsCollector(final JvmMetricsCollector value) {
            _nonHeapMemoryMetricsCollector = value;
            return this;
        }

        /**
         * Set the <code>ThreadMetricsCollector</code>. Defaults to an instance of
         * <code>ThreadMetricsCollector</code>. This is for testing purposes only and should never be used by clients.
         *
         * @param value A <code>ThreadMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setThreadMetricsCollector(final JvmMetricsCollector value) {
            _threadMetricsCollector = value;
            return this;
        }

        /**
         * Set the <code>GarbageCollectionMetricsCollector</code>. Defaults to an instance of
         * <code>GarbageCollectionMetricsCollector</code>. This is for testing purposes only and should never be used
         * by clients.
         *
         * @param value A <code>GarbageCollectionMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setGarbageCollectionMetricsCollector(final JvmMetricsCollector value) {
            _garbageCollectionMetricsCollector = value;
            return this;
        }

        /**
         * Set the <code>BufferPoolMetricsCollector</code>. Defaults to an instance of
         * <code>BufferPoolMetricsCollector</code>. This is for testing purposes only and should never be used
         * by clients.
         *
         * @param value A <code>BufferPoolMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setBufferPoolMetricsCollector(final JvmMetricsCollector value) {
            _bufferPoolMetricsCollector = value;
            return this;
        }

        /**
         * Set the <code>FileDescriptorMetricsCollector</code>. Defaults to an instance of
         * <code>FileDescriptorMetricsCollector</code>. This is for testing purposes only and should never be used
         * by clients.
         *
         * @param value A <code>FileDescriptorMetricsCollector</code> instance.
         * @return This <code>Builder</code> instance.
         */
        /* package private */ Builder setFileDescriptorMetricsCollector(final JvmMetricsCollector value) {
            _fileDescriptorMetricsCollector = value;
            return this;
        }

        private MetricsFactory _metricsFactory;
        private ManagementFactory _managementFactory = MANAGEMENT_FACTORY_DEFAULT;
        private Boolean _swallowException = true;
        private Boolean _collectNonHeapMemoryMetrics = true;
        private Boolean _collectHeapMemoryMetrics = true;
        private Boolean _collectThreadMetrics = true;
        private Boolean _collectGarbageCollectionMetrics = true;
        private Boolean _collectBufferPoolMetrics = true;
        private Boolean _collectFileDescriptorMetrics = true;
        private JvmMetricsCollector _nonHeapMemoryMetricsCollector = NonHeapMemoryMetricsCollector.newInstance();
        private JvmMetricsCollector _heapMemoryMetricsCollector = HeapMemoryMetricsCollector.newInstance();
        private JvmMetricsCollector _threadMetricsCollector = ThreadMetricsCollector.newInstance();
        private JvmMetricsCollector _garbageCollectionMetricsCollector = GarbageCollectionMetricsCollector.newInstance();
        private JvmMetricsCollector _bufferPoolMetricsCollector = BufferPoolMetricsCollector.newInstance();
        private JvmMetricsCollector _fileDescriptorMetricsCollector = FileDescriptorMetricsCollector.newInstance();

        private static final ManagementFactory MANAGEMENT_FACTORY_DEFAULT = ManagementFactoryDefault.newInstance();
    }

    /**
    * An implementation class of <code>ManagementFactory</code> that is to be used for getting the actual values for jvm
    * metrics from the java management API. This class exists to facilitate testing only and the clients should never
    * have to explicitly instantiate this.
    *
    * @author Deepika Misra (deepika at groupon dot com)
    */
    /* package private */ static final class ManagementFactoryDefault implements ManagementFactory {

        /**
         * Creates a new instance of <code>ManagementFactoryDefault</code>.
         *
         * @return An instance of <code>ManagementFactoryDefault</code>
         */
        /* package private */ static ManagementFactory newInstance() {
            return new ManagementFactoryDefault();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
            return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MemoryMXBean getMemoryMXBean() {
            return java.lang.management.ManagementFactory.getMemoryMXBean();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
            return java.lang.management.ManagementFactory.getMemoryPoolMXBeans();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ThreadMXBean getThreadMXBean() {
            return java.lang.management.ManagementFactory.getThreadMXBean();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<BufferPoolMXBean> getBufferPoolMXBeans() {
            return java.lang.management.ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OperatingSystemMXBean getOperatingSystemMXBean() {
            return java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        }

        private ManagementFactoryDefault() {}
    }
}
