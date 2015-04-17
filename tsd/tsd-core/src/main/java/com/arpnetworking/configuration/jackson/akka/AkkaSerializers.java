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
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;

/**
 * Serializers for Akka types.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AkkaSerializers extends Serializers.Base {
    /**
     * Public constructor.
     *
     * @param system actor system to use to resolve references
     */
    public AkkaSerializers(final ActorSystem system) {
        _actorRefSerializer = new ActorRefSerializer(system);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonSerializer<?> findSerializer(
            final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        final Class<?> raw = type.getRawClass();
        if (ActorRef.class.isAssignableFrom(raw)) {
            return _actorRefSerializer;
        }
        return super.findSerializer(config, type, beanDesc);
    }

    private final ActorRefSerializer _actorRefSerializer;
}
