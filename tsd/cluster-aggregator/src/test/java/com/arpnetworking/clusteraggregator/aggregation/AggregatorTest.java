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
package com.arpnetworking.clusteraggregator.aggregation;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.contrib.pattern.ShardRegion;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.arpnetworking.utility.BaseActorTest;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Tests for the Aggregator actor.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggregatorTest extends BaseActorTest {
    @Test
    public void passivatesProperly() {
        final TestProbe probe = TestProbe.apply(getSystem());

        final ActorRef aggregator = createAggregator(probe);
        aggregator.tell(ReceiveTimeout.getInstance(), aggregator);
        probe.watch(aggregator);
        final ShardRegion.Passivate passivate = probe.expectMsgClass(TIMEOUT, ShardRegion.Passivate.class);
        aggregator.tell(passivate.stopMessage(), aggregator);
        final Terminated terminated = probe.expectMsgClass(Terminated.class);
        Assert.assertEquals(aggregator, terminated.getActor());
    }

    public ActorRef createAggregator(final TestProbe probe) {
        final TestProbe ignored = TestProbe.apply(getSystem());
        return TestActorRef.apply(Aggregator.props(ignored.ref(), ignored.ref(), ignored.ref()), probe.ref(), "agg", getSystem());
    }

    private static final FiniteDuration TIMEOUT = FiniteDuration.apply(10, TimeUnit.SECONDS);
}
