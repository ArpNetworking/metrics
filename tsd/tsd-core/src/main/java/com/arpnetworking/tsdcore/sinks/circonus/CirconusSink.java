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
import akka.actor.PoisonPill;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.sinks.BaseSink;
import com.arpnetworking.tsdcore.statistics.HistogramStatistic;
import com.fasterxml.jackson.annotation.JacksonInject;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;

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
    public void recordAggregateData(final PeriodicData periodicData) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", periodicData.getData().size())
                .addData("conditionsSize", periodicData.getConditions().size())
                .log();

        final Collection<AggregatedData> specifiedData = periodicData.getData()
                .stream()
                .filter(this::isHistogramOrSpecified)
                .collect(Collectors.toList());
        _sinkActor.tell(new CirconusSinkActor.EmitAggregation(specifiedData), ActorRef.noSender());
    }

    private boolean isHistogramOrSpecified(final AggregatedData data) {
        return data.isSpecified() || (_enableHistograms && data.getFQDSN().getStatistic() instanceof HistogramStatistic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        _sinkActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("super", super.toLogValue())
                .put("sinkActor", _sinkActor)
                .build();
    }

    private CirconusSink(final Builder builder) {
        super(builder);
        final ActorSystem actorSystem = builder._actorSystem;
        final CirconusClient client = new CirconusClient.Builder()
                .setUri(builder._uri)
                .setAppName(builder._appName)
                .setAuthToken(builder._authToken)
                .setExecutionContext(builder._actorSystem.dispatcher())
                .setSafeHttps(builder._safeHttps)
                .build();
        _enableHistograms = builder._enableHistograms;
        _sinkActor = actorSystem.actorOf(
                CirconusSinkActor.props(
                        client,
                        builder._broker,
                        builder._maximumConcurrency,
                        builder._maximumQueueSize,
                        builder._spreadPeriod,
                        builder._enableHistograms));
    }

    private final ActorRef _sinkActor;
    private final boolean _enableHistograms;

    private static final Logger LOGGER = LoggerFactory.getLogger(CirconusSink.class);

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
         * Sets the safety of HTTPS. Optional. Default is true. Setting this to false
         * will accept any certificate and disables the hostname verifier. You may also
         * need to supply the "-Djsse.enableSNIExtension=false" JVM argument to disable
         * SNI.
         *
         * @param value the authentication token
         * @return this Builder
         */
        public Builder setSafeHttps(final Boolean value) {
            _safeHttps = value;
            return this;
        }

        /**
         * Sets the maximum concurrency of the http requests. Optional. Cannot be null.
         * Default is 1. Minimum is 1.
         *
         * @param value the maximum concurrency
         * @return this builder
         */
        public Builder setMaximumConcurrency(final Integer value) {
            _maximumConcurrency = value;
            return this;
        }

        /**
         * Sets the maximum delay before starting to send data to the server. Optional. Cannot be null.
         * Default is 0.
         *
         * @param value the maximum delay before sending new data
         * @return this builder
         */
        public Builder setSpreadPeriod(final Period value) {
            _spreadPeriod = value;
            return this;
        }

        /**
         * Sets the maximum pending queue size. Optional Cannot be null.
         * Default is 2500. Minimum is 1.
         *
         * @param value the maximum pending queue size
         * @return this builder
         */
        public Builder setMaximumQueueSize(final Integer value) {
            _maximumQueueSize = value;
            return this;
        }

        /**
         * Controls publication of histograms. Optional. Default is false.
         *
         * @param value true to enable histograms
         * @return this Builder
         */
        public Builder setEnableHistograms(final Boolean value) {
            _enableHistograms = value;
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

        @JacksonInject
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
        @NotNull
        private Boolean _safeHttps = true;
        @NotNull
        @Min(1)
        private Integer _maximumConcurrency = 1;
        @NotNull
        @Min(1)
        private Integer _maximumQueueSize = 2500;
        @NotNull
        private Period _spreadPeriod = Period.ZERO;
        @NotNull
        private Boolean _enableHistograms = false;
    }

}
