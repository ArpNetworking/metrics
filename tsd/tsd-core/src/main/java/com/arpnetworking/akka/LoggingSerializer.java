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
package com.arpnetworking.akka;

import akka.actor.ExtendedActorSystem;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogReferenceOnly;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;

/**
 * Serializer that logs the size of serialized objects.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LoggingSerializer extends akka.serialization.JavaSerializer {
    /**
     * Public constructor.
     *
     * @param system the actor system
     */
    public LoggingSerializer(final ExtendedActorSystem system) {
        super(system);
        _system = system;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int identifier() {
        return 1892872;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedActorSystem system() {
        return _system;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toBinary(final Object o) {
        final byte[] bytes = super.toBinary(o);
        LOGGER.debug()
                .setMessage("Serialized object")
                .addData("class", o.getClass().getCanonicalName())
                .addData("size", bytes.length)
                .log();
        return bytes;
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "id", Integer.toHexString(System.identityHashCode(this)),
                "class", this.getClass(),
                "ActorSystem", LogReferenceOnly.of(_system));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private final ExtendedActorSystem _system;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSerializer.class);
}
