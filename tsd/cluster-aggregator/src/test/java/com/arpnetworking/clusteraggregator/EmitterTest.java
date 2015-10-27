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
import com.arpnetworking.clusteraggregator.configuration.EmitterConfiguration;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.arpnetworking.utility.BaseActorTest;
import org.joda.time.Period;
import org.junit.Assert;
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
 * Tests for the {@link Emitter} actor.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class EmitterTest extends BaseActorTest {
    @Before
    public void setUp() {
        _config = new EmitterConfiguration.Builder().setSinks(Collections.singletonList(_sink)).build();
    }

    @Test
    public void propsCreation() {
        TestActorRef.create(getSystem(), Emitter.props(_config));
    }

    @Test
    public void callsSink() {
        final AggregatedData data = new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("TestCluster")
                        .setMetric("TestMetric")
                        .setService("TestService")
                        .setStatistic(MEDIAN_STATISTIC)
                        .build())
                .setHost("TestHost")
                .setPeriod(Period.minutes(1))
                .setIsSpecified(true)
                .setStart(org.joda.time.DateTime.now().hourOfDay().roundFloorCopy())
                .setPopulationSize(0L)
                .setSamples(Collections.<Quantity>emptyList())
                .setValue(new Quantity.Builder()
                        .setValue(14.0)
                        .build())
                .build();
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), Emitter.props(_config));

        ref.tell(data, ActorRef.noSender());
        Mockito.verify(_sink).recordAggregateData(_periodicData.capture());
        final List<AggregatedData> dataList = _periodicData.getValue().getData();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(1, dataList.size());
        Assert.assertSame(data, dataList.get(0));
    }

    @Test
    public void doesNotSwallowUnhandled() {
        final TestProbe probe = TestProbe.apply(getSystem());
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), Emitter.props(_config));
        getSystem().eventStream().subscribe(probe.ref(), UnhandledMessage.class);
        ref.tell("notAValidMessage", ActorRef.noSender());
        probe.expectMsgClass(FiniteDuration.apply(3, TimeUnit.SECONDS), UnhandledMessage.class);
    }

    @Captor
    private ArgumentCaptor<PeriodicData> _periodicData;
    private EmitterConfiguration _config = null;
    @Mock
    private Sink _sink = null;

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic MEDIAN_STATISTIC = STATISTIC_FACTORY.getStatistic("median");
}
