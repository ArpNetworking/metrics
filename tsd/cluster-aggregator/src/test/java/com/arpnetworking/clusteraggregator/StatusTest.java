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
import akka.actor.Address;
import akka.actor.UnhandledMessage;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterReadView;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.UniqueAddress;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.arpnetworking.utility.BaseActorTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import scala.collection.immutable.Set;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the {@link Status} actor.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StatusTest extends BaseActorTest {
    @Before
    public void setUp() {
        final Address localAddress = Address.apply("tcp", "default");
        final Member selfMember = new Member(UniqueAddress.apply(localAddress, 1821),
                                             1,
                                             MemberStatus.up(),
                                             new Set.Set1<>("test"));
        Mockito.when(_clusterMock.selfAddress()).thenReturn(localAddress);
        final ClusterReadView readView = Mockito.mock(ClusterReadView.class);
        Mockito.when(readView.self()).thenReturn(selfMember);
        Mockito.when(_clusterMock.readView()).thenReturn(readView);
        final ClusterEvent.CurrentClusterState state = Mockito.mock(ClusterEvent.CurrentClusterState.class);
        Mockito.when(state.getMembers()).thenReturn(Collections.singletonList(
                selfMember));
        Mockito.doAnswer(
                invocation -> {
                    final ActorRef ref = (ActorRef) invocation.getArguments()[0];
                    ref.tell(state, ActorRef.noSender());
                    return null;
                })
                .when(_clusterMock).sendCurrentClusterState(Mockito.<ActorRef>any());
    }

    @Test
    public void createProps() {
        TestActorRef.create(getSystem(), Status.props(_listenerProbe.ref(), _clusterMock, _listenerProbe.ref(), _listenerProbe.ref()));
    }

    @Test
    public void doesNotSwallowUnhandled() {
        final TestProbe probe = TestProbe.apply(getSystem());
        final TestActorRef<Actor> ref = TestActorRef.create(
                getSystem(),
                Status.props(_listenerProbe.ref(), _clusterMock, _listenerProbe.ref(), _listenerProbe.ref()));
        getSystem().eventStream().subscribe(probe.ref(), UnhandledMessage.class);
        ref.tell("notAValidMessage", ActorRef.noSender());
        probe.expectMsgClass(FiniteDuration.apply(3, TimeUnit.SECONDS), UnhandledMessage.class);
    }

    @Mock
    private TestProbe _listenerProbe;

    @Mock
    private Cluster _clusterMock;
}

