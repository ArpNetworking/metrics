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

import akka.actor.ActorSystem;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jdk8.PackageVersion;

/**
 * Jackson module for serializing and deserializing Akka objects.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AkkaModule extends Module {
    /**
     * Public constructor.
     *
     * @param system the actor system to resolve references
     */
    public AkkaModule(final ActorSystem system) {
        _system = system;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupModule(final SetupContext context) {
        context.addSerializers(new AkkaSerializers(_system));
        context.addDeserializers(new AkkaDeserializers(_system));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModuleName() {
        return "AkkaModule";
    }

    private final ActorSystem _system;
}
