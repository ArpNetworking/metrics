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

import akka.actor.ActorRef;
import akka.testkit.TestProbe;
import com.arpnetworking.utility.BaseActorTest;
import com.arpnetworking.utility.ParallelLeastShardAllocationStrategy;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tests for the StatusResponse class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StatusResponseTest extends BaseActorTest {
    @Test
    public void constructSimple() {
        final Map<String, ActorRef> pending = Maps.newHashMap();
        final Set<String> inflight = Sets.newHashSet();
        final Map<ActorRef, Set<String>> current = Maps.newHashMap();
        final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = new ParallelLeastShardAllocationStrategy
                .RebalanceNotification(current, inflight, pending);
        new ClusterStatusCache.StatusResponse(null, Optional.of(notification));
    }

    @Test
    public void constructWithTwoRegions() {
        final Map<String, ActorRef> pending = Maps.newHashMap();
        final Set<String> inflight = Sets.newHashSet();
        final Map<ActorRef, Set<String>> current = Maps.newHashMap();
        final TestProbe a = TestProbe.apply(getSystem());
        final TestProbe b = TestProbe.apply(getSystem());

        current.put(a.ref(), Sets.newHashSet("1", "2"));
        current.put(b.ref(), Sets.newHashSet("3", "4"));

        final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = new ParallelLeastShardAllocationStrategy
                .RebalanceNotification(current, inflight, pending);
        new ClusterStatusCache.StatusResponse(null, Optional.of(notification));
    }

    @Test
    public void constructWithInflight() {
        final Map<String, ActorRef> pending = Maps.newHashMap();
        final Set<String> inflight = Sets.newHashSet();
        final Map<ActorRef, Set<String>> current = Maps.newHashMap();
        final TestProbe a = TestProbe.apply(getSystem());
        final TestProbe b = TestProbe.apply(getSystem());

        current.put(a.ref(), Sets.newHashSet("1", "2"));
        current.put(b.ref(), Sets.newHashSet("3", "4"));
        inflight.add("2");

        final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = new ParallelLeastShardAllocationStrategy
                .RebalanceNotification(current, inflight, pending);
        new ClusterStatusCache.StatusResponse(null, Optional.of(notification));
    }

    @Test
    public void constructWithPending() {
        final Map<String, ActorRef> pending = Maps.newHashMap();
        final Set<String> inflight = Sets.newHashSet();
        final Map<ActorRef, Set<String>> current = Maps.newHashMap();
        final TestProbe a = TestProbe.apply(getSystem());
        final TestProbe b = TestProbe.apply(getSystem());

        current.put(a.ref(), Sets.newHashSet("1", "2"));
        current.put(b.ref(), Sets.newHashSet("3", "4"));
        pending.put("3", a.ref());

        final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = new ParallelLeastShardAllocationStrategy
                .RebalanceNotification(current, inflight, pending);
        new ClusterStatusCache.StatusResponse(null, Optional.of(notification));
    }
}
