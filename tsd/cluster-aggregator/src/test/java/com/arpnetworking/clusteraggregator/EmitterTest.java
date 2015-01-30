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
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.TP50Statistic;
import com.google.common.base.Optional;
import org.joda.time.Period;
import org.junit.Assert;
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
    @Test
    public void propsCreation() {
        TestActorRef.create(getSystem(), Emitter.props(_sink));
    }

    @Test
    public void callsSink() {
        final AggregatedData data = new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                    .setCluster("TestCluster")
                    .setMetric("TestMetric")
                    .setService("TestService")
                    .setStatistic(new TP50Statistic())
                    .build())
                .setHost("TestHost")
                .setPeriod(Period.minutes(1))
                .setStart(org.joda.time.DateTime.now().hourOfDay().roundFloorCopy())
                .setPopulationSize(0L)
                .setSamples(Collections.<Quantity>emptyList())
                .setValue(new Quantity(14.0, Optional.<Unit>absent()))
                .build();
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), Emitter.props(_sink));

        ref.tell(data, ActorRef.noSender());
        Mockito.verify(_sink).recordAggregateData(_aggregatedData.capture());
        final List<AggregatedData> dataList = _aggregatedData.getValue();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(1, dataList.size());
        Assert.assertSame(data, dataList.get(0));
    }

    @Test
    public void doesNotSwallowUnhandled() {
        final TestProbe probe = TestProbe.apply(getSystem());
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), Emitter.props(_sink));
        getSystem().eventStream().subscribe(probe.ref(), UnhandledMessage.class);
        ref.tell("notAValidMessage", ActorRef.noSender());
        probe.expectMsgClass(FiniteDuration.apply(3, TimeUnit.SECONDS), UnhandledMessage.class);
    }

    @Captor
    private ArgumentCaptor<List<AggregatedData>> _aggregatedData;

    @Mock
    private Sink _sink;
}
