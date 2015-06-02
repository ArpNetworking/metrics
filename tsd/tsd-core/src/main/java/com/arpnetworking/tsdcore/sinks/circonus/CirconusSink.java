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
package com.arpnetworking.tsdcore.sinks.circonus;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.sinks.BaseSink;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.net.URI;
import java.util.Collection;

/**
 * A traditional tsdcore single threaded sink to act as an adapter for the actor-based sink.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class CirconusSink extends BaseSink {
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        _sinkActor.tell(new CirconusSinkActor.EmitAggregation(data), ActorRef.noSender());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {  }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "super", super.toLogValue(),
                "SinkActor", _sinkActor);
    }

    private CirconusSink(final Builder builder) {
        super(builder);
        final ActorSystem actorSystem = builder._actorSystem;
        final CirconusClient client = new CirconusClient.Builder()
                .setUri(builder._uri)
                .setAppName(builder._appName)
                .setAuthToken(builder._authToken)
                .setExecutionContext(builder._actorSystem.dispatcher())
                .build();
        _sinkActor = actorSystem.actorOf(CirconusSinkActor.props(client, builder._broker));
    }

    private final ActorRef _sinkActor;

    /**
     * Builder for {@link CirconusSink}.
     */
    public static class Builder extends BaseSink.Builder<Builder, CirconusSink> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(CirconusSink.class);
        }

        /**
         * Sets the actor system to create the sink actor in.
         *
         * @param value the actor system
         * @return this builder
         */
        public Builder setActorSystem(final ActorSystem value) {
            _actorSystem = value;
            return this;
        }

        /**
         * Sets the base url for the Circonus API.
         *
         * @param value the base uri
         * @return this builder
         */
        public Builder setUri(final URI value) {
            _uri = value;
            return this;
        }

        /**
         * Sets the broker name to push metrics to.
         *
         * @param value the Circonus broker to push to
         * @return this builder
         */
        public Builder setBroker(final String value) {
            _broker = value;
            return this;
        }

        /**
         * Sets the app name in Circonus.
         *
         * @param value the name of the app
         * @return this builder
         */
        public Builder setAppName(final String value) {
            _appName = value;
            return this;
        }

        /**
         * Sets the auth token.
         *
         * @param value the auth token
         * @return this builder
         */
        public Builder setAuthToken(final String value) {
            _authToken = value;
            return this;
        }

        /**
         * Called by setters to always return appropriate subclass of
         * <code>Builder</code>, even from setters of base class.
         *
         * @return instance with correct <code>Builder</code> class type.
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private ActorSystem _actorSystem;
        @NotNull
        @NotEmpty
        private URI _uri;
        @NotNull
        @NotEmpty
        private String _broker;
        @NotNull
        @NotEmpty
        private String _appName;
        @NotNull
        @NotEmpty
        private String _authToken;
    }

}
