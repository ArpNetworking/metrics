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
import com.arpnetworking.metrics.jvm.collectors.JvmMetricsCollector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests the <code>JvmMetricsRunnable</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class JvmMetricsRunnableTest {

    @Before
    public void setUp() {
        _metricsFactory = Mockito.mock(MetricsFactory.class);
        _metrics = Mockito.mock(Metrics.class);
        _managementFactory = Mockito.mock(ManagementFactory.class);
        _gcCollector = Mockito.mock(JvmMetricsCollector.class);
        _heapMemoryCollector = Mockito.mock(JvmMetricsCollector.class);
        _nonHeapMemoryCollector = Mockito.mock(JvmMetricsCollector.class);
        _threadCollector = Mockito.mock(JvmMetricsCollector.class);
        _bufferPoolCollector = Mockito.mock(BufferPoolMetricsCollector.class);
        _fileDescriptorCollector = Mockito.mock(FileDescriptorMetricsCollector.class);
        Mockito.doReturn(_metrics).when(_metricsFactory).create();
    }

    @After
    public void tearDown() {
        _gcCollector = null;
        _heapMemoryCollector = null;
        _nonHeapMemoryCollector = null;
        _threadCollector = null;
        _bufferPoolCollector = null;
        _fileDescriptorCollector = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableMetricsFactoryNullCase() {
        createJvmMetricsRunnableBuilder().setMetricsFactory(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableSwallowExceptionNullCase() {
        createJvmMetricsRunnableBuilder().setSwallowException(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectHeapMemoryMetricsNullCase() {
        createJvmMetricsRunnableBuilder().setCollectHeapMemoryMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectNonHeapMemoryMetricsNullCase() {
        createJvmMetricsRunnableBuilder().setCollectNonHeapMemoryMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectThreadMetricsNullCase() {
        createJvmMetricsRunnableBuilder().setCollectThreadMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectBufferPoolMetricsNullCase() {
        createJvmMetricsRunnableBuilder().setCollectBufferPoolMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectGcMetricsNullCase() {
        createJvmMetricsRunnableBuilder().setCollectGarbageCollectionMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableCollectFileDescriptorNullCase() {
        createJvmMetricsRunnableBuilder().setCollectFileDescriptorMetrics(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableManagementFactoryNullCase() {
        createJvmMetricsRunnableBuilder().setManagementFactory(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableNonHeapMemoryMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setNonHeapMemoryMetricsCollector(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableHeapMemoryMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setHeapMemoryMetricsCollector(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableThreadyMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setThreadMetricsCollector(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableGarbageCollectionMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setGarbageCollectionMetricsCollector(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableBufferPoolMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setBufferPoolMetricsCollector(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRunnableFileDescriptorMetricsCollectorNullCase() {
        createJvmMetricsRunnableBuilder().setFileDescriptorMetricsCollector(null).build();
    }

    @Test
    public void testRunDefaultCollectorsEnabledCase() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        runnable.run();
        Mockito.verify(_gcCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verify(_heapMemoryCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verify(_nonHeapMemoryCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verify(_threadCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verify(_bufferPoolCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verify(_fileDescriptorCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
    }

    @Test
    public void testRunCollectorsAllDisabledCase() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectHeapMemoryMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyGcCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectHeapMemoryMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verify(_gcCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyHeapMemoryCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verify(_heapMemoryCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyNonHeapMemoryCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verify(_nonHeapMemoryCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyThreadCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectHeapMemoryMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verify(_threadCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyBufferPoolCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectHeapMemoryMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectFileDescriptorMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verify(_bufferPoolCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
        Mockito.verifyNoMoreInteractions(_fileDescriptorCollector);
    }

    @Test
    public void testRunOnlyFileDescriptorCollectorEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setCollectGarbageCollectionMetrics(false)
                .setCollectHeapMemoryMetrics(false)
                .setCollectNonHeapMemoryMetrics(false)
                .setCollectThreadMetrics(false)
                .setCollectBufferPoolMetrics(false)
                .build();
        runnable.run();
        Mockito.verifyNoMoreInteractions(_gcCollector);
        Mockito.verifyNoMoreInteractions(_heapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_nonHeapMemoryCollector);
        Mockito.verifyNoMoreInteractions(_threadCollector);
        Mockito.verifyNoMoreInteractions(_bufferPoolCollector);
        Mockito.verify(_fileDescriptorCollector).collect(Mockito.any(Metrics.class), Mockito.any(ManagementFactory.class));
    }

    @Test
    public void testRunWithExceptionOnGcCollect() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        Mockito.doThrow(Exception.class).when(_gcCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test
    public void testRunWithExceptionOnHeapMemoryCollect() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        Mockito.doThrow(Exception.class).when(_heapMemoryCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test
    public void testRunWithExceptionOnNonHeapMemoryCollect() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        Mockito.doThrow(Exception.class).when(_nonHeapMemoryCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test
    public void testRunWithExceptionOnBufferPoolCollect() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        Mockito.doThrow(Exception.class).when(_bufferPoolCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test
    public void testRunWithExceptionOnFileDescriptorCollect() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().build();
        Mockito.doThrow(Exception.class).when(_fileDescriptorCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test
    public void testRunWithExceptionThrownWithSwallowExceptionEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder().setSwallowException(true).build();
        Mockito.doThrow(Exception.class).when(_threadCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test(expected = Exception.class)
    public void testRunWithExceptionThrownWithSwallowExceptionDisabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(false)
                .build();
        Mockito.doThrow(Exception.class).when(_threadCollector).collect(_metrics, _managementFactory);
        runnable.run();
    }

    @Test(expected = Exception.class)
    public void testRunWithExceptionOnMetricsCreateWithSwallowExceptionDisabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(false)
                .build();
        Mockito.doThrow(Exception.class).when(_metricsFactory).create();
        runnable.run();
    }

    @Test
    public void testRunWithExceptionOnMetricsCreateWithSwallowExceptionEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(true)
                .build();
        Mockito.doThrow(Exception.class).when(_metricsFactory).create();
        runnable.run();
    }

    @Test(expected = Exception.class)
    public void testRunWithExceptionOnCollectAndCloseWithSwallowExceptionDisabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(false)
                .build();
        Mockito.doThrow(Exception.class).when(_gcCollector).collect(_metrics, _managementFactory);
        Mockito.doThrow(Exception.class).when(_metrics).close();
        runnable.run();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandleRuntimeExceptionWithSwallowExceptionDisabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(false)
                .build();
        runnable.handleException(new IllegalArgumentException());
    }

    @Test(expected = RuntimeException.class)
    public void testHandleExceptionWithSwallowExceptionDisabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(false)
                .build();
        try {
            runnable.handleException(new ClassNotFoundException());
        // CHECKSTYLE.OFF: IllegalCatch - No checked exceptions thrown
        } catch (final Exception e) {
        // CHECKSTYLE.ON: IllegalCatch
            Assert.assertEquals(ClassNotFoundException.class, e.getCause().getClass());
            throw e;
        }
    }

    @Test
    public void testHandleExceptionWithSwallowExceptionEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(true)
                .build();
        runnable.handleException(new ClassNotFoundException());
    }

    @Test
    public void testHandleRuntimeExceptionWithSwallowExceptionEnabled() {
        final JvmMetricsRunnable runnable = createJvmMetricsRunnableBuilder()
                .setSwallowException(true)
                .build();
        runnable.handleException(new RuntimeException());
    }
    
    private JvmMetricsRunnable.Builder createJvmMetricsRunnableBuilder() {
        return new JvmMetricsRunnable.Builder()
                .setMetricsFactory(_metricsFactory)
                .setManagementFactory(_managementFactory)
                .setGarbageCollectionMetricsCollector(_gcCollector)
                .setHeapMemoryMetricsCollector(_heapMemoryCollector)
                .setNonHeapMemoryMetricsCollector(_nonHeapMemoryCollector)
                .setThreadMetricsCollector(_threadCollector)
                .setBufferPoolMetricsCollector(_bufferPoolCollector)
                .setFileDescriptorMetricsCollector(_fileDescriptorCollector);
    }

    private MetricsFactory _metricsFactory = null;
    private Metrics _metrics = null;
    private ManagementFactory _managementFactory = null;
    private JvmMetricsCollector _gcCollector = null;
    private JvmMetricsCollector _heapMemoryCollector = null;
    private JvmMetricsCollector _nonHeapMemoryCollector = null;
    private JvmMetricsCollector _threadCollector = null;
    private JvmMetricsCollector _bufferPoolCollector = null;
    private JvmMetricsCollector _fileDescriptorCollector = null;
}
