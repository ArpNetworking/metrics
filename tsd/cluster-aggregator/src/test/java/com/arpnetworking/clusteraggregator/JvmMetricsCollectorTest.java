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
package com.arpnetworking.clusteraggregator;

import akka.actor.Props;
import akka.actor.Scheduler;
import akka.testkit.TestActorRef;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.utility.BaseActorTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Tests <code>JvmMetricsCollector</code> class.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class JvmMetricsCollectorTest extends BaseActorTest {

    @Before
    public void setup() {
        Mockito.doReturn(_metrics).when(_metricsFactory).create();
    }

    @Test
    public void testCollectMessage() {
        _collector = TestActorRef.create(
                getSystem(),
                Props.create(JvmMetricsCollector.class, INTERVAL_DURATION, _runnable, getSystem().scheduler()));
        _collector.receive(new JvmMetricsCollector.CollectJvmMetrics());
        Mockito.verify(_runnable, Mockito.atLeastOnce()).run();
    }

    @Test
    public void testCancelsCollectionOnStop() {
        _collector = TestActorRef.create(
                getSystem(),
                Props.create(JvmMetricsCollector.class, INTERVAL_DURATION, _runnable, getSystem().scheduler()));
        final JvmMetricsCollector collectorActor = _collector.underlyingActor();
        Assert.assertFalse(collectorActor.getCancellable().isCancelled());
        collectorActor.postStop();
        Assert.assertTrue(collectorActor.getCancellable().isCancelled());
    }

    @Test
    public void testScheduleCollectionOnStart() {
        _collector = TestActorRef.create(
                getSystem(),
                Props.create(JvmMetricsCollector.class, INTERVAL_DURATION, _runnable, _scheduler));
        Mockito.verify(_scheduler).schedule(
                Mockito.eq(FiniteDuration.Zero()),
                Mockito.eq(INTERVAL_DURATION),
                Mockito.eq(_collector),
                Mockito.any(JvmMetricsCollector.CollectJvmMetrics.class),
                Mockito.any(ExecutionContextExecutor.class),
                Mockito.eq(_collector));
    }

    private TestActorRef<JvmMetricsCollector> _collector = null;
    @Mock
    private MetricsFactory _metricsFactory;
    @Mock
    private Metrics _metrics;
    @Mock
    private Runnable _runnable;
    @Mock
    private Scheduler _scheduler;

    private static final FiniteDuration INTERVAL_DURATION = FiniteDuration.create(100, TimeUnit.MILLISECONDS);
}
