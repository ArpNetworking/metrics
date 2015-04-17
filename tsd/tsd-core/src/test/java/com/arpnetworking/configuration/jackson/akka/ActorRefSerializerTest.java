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
package com.arpnetworking.configuration.jackson.akka;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.utility.BaseActorTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the akka actor reference serializer.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ActorRefSerializerTest extends BaseActorTest {
    @Before
    public void setupMapper() {
        _mapper = ObjectMapperFactory.createInstance();
        _mapper.registerModule(new AkkaModule(getSystem()));
    }

    @Test
    public void testSerializesToString() throws JsonProcessingException {
        final TestActorRef<Actor> ref = TestActorRef.create(getSystem(), Props.create(DoNothingActor.class));

        final JsonNode serialized = _mapper.valueToTree(ref);
        Assert.assertTrue(serialized.isTextual());
    }

    @Test
    public void testSerializeDeserialize() throws IOException {
        final TestProbe probe = TestProbe.apply(getSystem());
        final JsonNode serialized = _mapper.valueToTree(probe.ref());

        final ActorSystem system2 = ActorSystem.create("Test");
        final ObjectMapper system2Mapper = ObjectMapperFactory.createInstance();
        system2Mapper.registerModule(new AkkaModule(system2));
        final ActorRef remoteRef = system2Mapper.readValue(serialized.toString(), ActorRef.class);

        remoteRef.tell("OK", ActorRef.noSender());
        probe.expectMsg(FiniteDuration.apply(3L, TimeUnit.SECONDS), "OK");
    }

    private ObjectMapper _mapper;

    private static class DoNothingActor extends UntypedActor {
        @Override
        public void onReceive(final Object o) throws Exception {  }
    }
}
