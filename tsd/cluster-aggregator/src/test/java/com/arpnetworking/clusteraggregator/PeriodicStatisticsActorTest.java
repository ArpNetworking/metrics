/**
 * Copyright 2014 Groupon.com
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

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UnhandledMessage;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.arpnetworking.clusteraggregator.test.ThrowingMetrics;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.arpnetworking.utility.BaseActorTest;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the {@link PeriodicStatisticsActor} actor.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class PeriodicStatisticsActorTest extends BaseActorTest {
    @Before
    public void setUp() {
        Mockito.when(_mfMock.create()).thenReturn(_metrics);
    }

    @Test
    public void propsCreation() {
        TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
    }

    @Test
    public void storeMetric() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final AggregatedData metricReport = getAggregatedDataBuilder(DateTime.now().hourOfDay().roundFloorCopy(), Period.minutes(1))
                .build();

        ref.tell(metricReport, ActorRef.noSender());
        Mockito.verifyZeroInteractions(_mfMock, _metrics);
    }

    @Test
    public void singleAggregatedData() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_METRICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_STATISTICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_SERVICES_SEEN, 1);
    }

    @Test
    public void metricsFailCreate() {
        final TestActorRef<Actor> ref = TestActorRef.create(
                getSystem(),
                PeriodicStatisticsActor.props(
                        throwingFactory(true, false)));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
    }

    private MetricsFactory throwingFactory(final boolean throwOnRecord, final boolean throwOnClose) {
        final ThrowingMetrics metrics = new ThrowingMetrics(throwOnRecord, throwOnClose);
        final MetricsFactory factory = Mockito.mock(MetricsFactory.class);
        Mockito.when(factory.create()).thenReturn(metrics);
        return factory;
    }

    @Test
    public void metricsFailStore() {
        final TestActorRef<Actor> ref = TestActorRef.create(
                getSystem(),
                PeriodicStatisticsActor.props(
                        throwingFactory(true, false)));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
    }

    @Test
    public void metricsFailClose() {
        final TestActorRef<Actor> ref = TestActorRef.create(
                getSystem(),
                PeriodicStatisticsActor.props(
                        throwingFactory(false, true)));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the getNext time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
    }

    @Test
    public void metricsFailAll() {
        final TestActorRef<Actor> ref = TestActorRef.create(
                getSystem(),
                PeriodicStatisticsActor.props(
                        throwingFactory(true, true)));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
    }

    @Test
    public void twoAggregatedDatas() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        final AggregatedData metricReport2 = builder.setFQDSN(getFQDSNBuilder().setMetric("anothermetric").build()).build();
        ref.tell(metricReport2, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_METRICS_SEEN, 2);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_STATISTICS_SEEN, 2);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_SERVICES_SEEN, 1);
    }

    /**
     * Tests that the same metric name from two separate services counts as different metrics.
     */
    @Test
    public void twoAggregatedDatasDifferentService() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        final AggregatedData metricReport2 = builder.setFQDSN(getFQDSNBuilder().setService("anotherService").build()).build();
        ref.tell(metricReport2, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_METRICS_SEEN, 2);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_STATISTICS_SEEN, 2);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_SERVICES_SEEN, 2);
    }

    /**
     * Tests multiple deliveries of the same fqsn, should only report it once.
     */
    @Test
    public void twoDeliveriesSameFQSN() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());
        ref.tell(metricReport1, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_METRICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_STATISTICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_SERVICES_SEEN, 1);
    }

    /**
     * Test that an out-of-date message doesn't record.
     */
    @Test
    public void outOfDate() {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final Period period = Period.minutes(1);
        final AggregatedData.Builder builder = getAggregatedDataBuilder(start, period);

        final AggregatedData metricReport1 = builder.build();
        ref.tell(metricReport1, ActorRef.noSender());

        final AggregatedData metricReport2 = builder.setStart(start.minus(period)).build();
        ref.tell(metricReport2, ActorRef.noSender());

        // Will push the listener to the next time period
        final AggregatedData flushReport = builder.setStart(start.plus(period)).build();
        ref.tell(flushReport, ActorRef.noSender());
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_METRICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_STATISTICS_SEEN, 1);
        Mockito.verify(_metrics).incrementCounter(CLUSTER_PERIOD_SERVICES_SEEN, 1);
    }

    @Test
    public void doesNotSwallowUnhandled() {
        final TestProbe probe = TestProbe.apply(getSystem());
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), PeriodicStatisticsActor.props(_mfMock));
        getSystem().eventStream().subscribe(probe.ref(), UnhandledMessage.class);
        ref.tell("notAValidMessage", ActorRef.noSender());
        probe.expectMsgClass(FiniteDuration.apply(3, TimeUnit.SECONDS), UnhandledMessage.class);
    }

    private AggregatedData.Builder getAggregatedDataBuilder(final DateTime start, final Period period) {
        return new AggregatedData.Builder()
                .setFQDSN(getFQDSNBuilder().build())
                .setPeriod(period)
                .setStart(start)
                .setHost("testhost")
                .setIsSpecified(true)
                .setValue(new Quantity.Builder().setValue(0d).build())
                .setSamples(Collections.<Quantity>emptyList())
                .setPopulationSize(0L);
    }

    private FQDSN.Builder getFQDSNBuilder() {
        return new FQDSN.Builder()
                .setCluster("testcluster")
                .setService("testservice")
                .setMetric("testmetric")
                .setStatistic(MEDIAN_STATISTIC);
    }

    @Captor
    private ArgumentCaptor<List<AggregatedData>> _aggregatedData;

    @Mock
    private MetricsFactory _mfMock;
    @Mock
    private Metrics _metrics;

    private static final String CLUSTER_PERIOD_METRICS_SEEN = "cluster/period/metrics_seen";
    private static final String CLUSTER_PERIOD_STATISTICS_SEEN = "cluster/period/statistics_seen";
    private static final String CLUSTER_PERIOD_SERVICES_SEEN = "cluster/period/services_seen";
    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic MEDIAN_STATISTIC = STATISTIC_FACTORY.getStatistic("median");
}
