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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializer for an Akka ActorRef.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ActorRefSerializer extends JsonSerializer<ActorRef> {
    /**
     * Public constructor.
     *
     * @param system actor system to use to resolve references
     */
    public ActorRefSerializer(final ActorSystem system) {
        _system = system;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(
            final ActorRef value,
            final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException {
        gen.writeString(value.path().toStringWithAddress(_system.provider().getDefaultAddress()));
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("actorSystem", _system)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private final ActorSystem _system;
}
