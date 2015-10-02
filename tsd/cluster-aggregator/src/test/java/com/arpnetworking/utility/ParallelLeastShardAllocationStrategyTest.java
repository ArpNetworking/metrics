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
package com.arpnetworking.utility;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import scala.collection.JavaConversions;
import scala.collection.immutable.IndexedSeq;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the ParallelLeastShardAllocationStrategy class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ParallelLeastShardAllocationStrategyTest extends BaseActorTest {
    @Test
    public void testNoRegion() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                10,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertTrue(rebalance.isEmpty());
    }

    @Test
    public void testSingleRegion() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                10,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertEquals(0, rebalance.size());
    }

    @Test
    public void testDoesntMeetThreshold() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                10,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        allocateShardsToNewActor(19, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertEquals(0, rebalance.size());
    }

    @Test
    public void testSingleRemap() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(20, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertEquals(1, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });
    }

    @Test
    public void testSendsNotification() {
        final TestProbe probe = TestProbe.apply(getSystem());
        final ActorSelection selection = ActorSelection.apply(probe.ref(), "");
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.of(selection));
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(20, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet("rebalancing_1"));
        Assert.assertEquals(1, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });
        final ParallelLeastShardAllocationStrategy.RebalanceNotification notification = probe.expectMsgClass(
                FiniteDuration.apply(3, TimeUnit.SECONDS),
                ParallelLeastShardAllocationStrategy.RebalanceNotification.class);

        Assert.assertEquals(1, notification.getInflightRebalances().size());
        Assert.assertTrue(notification.getInflightRebalances().contains("rebalancing_1"));
        currentAllocations.forEach((k, v) -> {
                    final Set<String> actualAllocations = notification.getCurrentAllocations().get(k);
                    Assert.assertNotNull(actualAllocations);
                    Assert.assertEquals(v.size(), actualAllocations.size());
                    final List<String> shardsList = JavaConversions.seqAsJavaList(v);
                    final String[] shards = shardsList.toArray(new String[shardsList.size()]);
                    Assert.assertThat(actualAllocations, Matchers.containsInAnyOrder(shards));
                });
        Assert.assertEquals(1, notification.getPendingRebalances().size());
        rebalance.forEach(shard -> Assert.assertTrue(notification.getPendingRebalances().containsKey(shard)));
    }

    /** This test makes sure that the maximum parallel inflight migrations is honored if that limit is hit completely in
     * the single remap.
     */
    @Test
    public void testMaxParallelSingleCallRemap() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(30, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertEquals(5, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });
    }

    /**
     * This test makes sure that the maximum prallel inflight migrations is honored when there are previous inflight
     * remappings.
     */
    @Test
    public void testHonorsInflightRemappings() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(30, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(
                currentAllocations,
                Sets.newHashSet("remapping_shard_1", "remapping_shard_2", "remapping_shard_3"));
        Assert.assertEquals(2, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });
    }

    /**
     * This test makes sure that the balancing algorithm takes into account shards migrated during the current
     * rebalance.
     */
    @Test
    public void testWontOvershootTest() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                100,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(10, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(39, currentAllocations);

        final Set<String> rebalance = realloc.rebalance(currentAllocations, Sets.newHashSet());
        Assert.assertEquals(10, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });
    }

    /**
     * This test makes sure that in-flight remappings are not counted towards a region's shard ownership.
     * Setting the "larger" shard to have 50, but having 25 in-flight leaves 25 allocated.  With the
     * max difference &lt;5, this will cause 11 remaps.  None of those remaps should come from the already
     * remapping set.
     */
    @Test
    public void testWontCountAlreadyRemappingShards() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                100,
                5,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        allocateShardsToNewActor(0, currentAllocations);
        final TestActorRef<DoNothingActor> rebalancedActor = allocateShardsToNewActor(50, currentAllocations);
        final HashSet<String> remapping = Sets.newHashSet();
        for (int x = 11; x <= 35; x++) {
            remapping.add("shard_" + x);
        }

        final Set<String> rebalance = realloc.rebalance(currentAllocations, remapping);
        Assert.assertEquals(11, rebalance.size());
        rebalance.forEach(
                e -> {
                    Assert.assertThat(
                            JavaConversions.setAsJavaSet(currentAllocations.get(rebalancedActor).toSet()),
                            Matchers.hasItem(e));
                });

        final String remappedShard = rebalance.stream().findFirst().get();
        Assert.assertThat(remapping, Matchers.not(Matchers.hasItem(remappedShard)));
    }

    @Test
    public void testAllocatesSuccessfully() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        final TestActorRef<DoNothingActor> first = allocateShardsToNewActor(0, currentAllocations);
        final TestActorRef<DoNothingActor> sender = createShardRegion();

        final ActorRef allocatedTo = realloc.allocateShard(sender, "shard_1", currentAllocations);
        Assert.assertEquals(first, allocatedTo);
    }

    @Test
    public void testAllocatesEvenly() {
        final ParallelLeastShardAllocationStrategy realloc = new ParallelLeastShardAllocationStrategy(
                5,
                10,
                Optional.<ActorSelection>absent());
        final Map<ActorRef, IndexedSeq<String>> currentAllocations = Maps.newHashMap();
        final TestActorRef<DoNothingActor> first = allocateShardsToNewActor(0, currentAllocations);
        final TestActorRef<DoNothingActor> second = allocateShardsToNewActor(2, currentAllocations);
        final TestActorRef<DoNothingActor> sender = createShardRegion();

        final Map<ActorRef, Integer> allocations = Maps.newHashMap();
        for (int x = 0; x < 4; x++) {
            final String shardName = "shard_" + x + 10;
            final ActorRef allocatedTo = realloc.allocateShard(sender, shardName, currentAllocations);
            allocations.compute(allocatedTo, (k, v) -> v == null ? 1 : v + 1);
            final IndexedSeq<String> previousAllocations = currentAllocations.get(allocatedTo);
            final List<String> newAllocations = Lists.newArrayList(JavaConversions.seqAsJavaList(previousAllocations));
            newAllocations.add(shardName);
            currentAllocations.put(allocatedTo, JavaConversions.asScalaBuffer(newAllocations).toIndexedSeq());
        }
        Assert.assertEquals(3, (long) allocations.get(first));
        Assert.assertEquals(1, (long) allocations.get(second));
    }

    private TestActorRef<DoNothingActor> allocateShardsToNewActor(
            final int shards,
            final Map<ActorRef, IndexedSeq<String>> allocations) {
        final TestActorRef<DoNothingActor> region = createShardRegion();
        final List<String> shardNames = Lists.newArrayList();
        for (int x = 0; x < shards; x++) {
            shardNames.add("shard_" + _shardId);
            _shardId++;
        }

        allocations.put(region, JavaConversions.collectionAsScalaIterable(shardNames).toIndexedSeq());
        return region;
    }

    private TestActorRef<DoNothingActor> createShardRegion() {
        return TestActorRef.create(getSystem(), Props.create(DoNothingActor.class));
    }

    private int _shardId = 1;

    private static class DoNothingActor extends UntypedActor {
        @Override
        public void onReceive(final Object message) throws Exception {
        }
    }
}
