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
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;

/**
 * Deserializers for Akka types.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AkkaDeserializers extends Deserializers.Base {
    /**
     * Public constructor.
     *
     * @param system actor system to resolve references
     */
    public AkkaDeserializers(final ActorSystem system) {
        _actorRefDeserializer = new ActorRefDeserializer(system);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonDeserializer<?> findBeanDeserializer(
            final JavaType type,
            final DeserializationConfig config,
            final BeanDescription beanDesc) throws JsonMappingException {
        final Class<?> raw = type.getRawClass();
        if (raw == ActorRef.class) {
            return _actorRefDeserializer;
        }
        return super.findBeanDeserializer(type, config, beanDesc);
    }

    private final ActorRefDeserializer _actorRefDeserializer;
}
