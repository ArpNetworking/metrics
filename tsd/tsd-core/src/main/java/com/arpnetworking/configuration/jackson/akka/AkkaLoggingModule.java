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
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogReferenceOnly;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module for serializing and deserializing Akka objects.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class AkkaLoggingModule extends SimpleModule {

    /**
     * Public constructor.
     */
    public AkkaLoggingModule() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupModule(final SetupContext context) {
        addSerializer(ActorRef.class, new ActorRefLoggingSerializer());
        super.setupModule(context);
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogReferenceOnly.of(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }
}
