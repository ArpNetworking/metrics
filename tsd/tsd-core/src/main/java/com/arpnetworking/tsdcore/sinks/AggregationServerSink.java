/**
 * Copyright 2014 Brandon Arp
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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.AggregationMessage;
import com.arpnetworking.tsdcore.model.Quantity;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import java.util.List;

/**
 * Publisher to send data to an upstream aggregation server.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggregationServerSink extends VertxSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("Host", _host)
                .add("Cluster", _cluster)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConnect(final NetSocket socket) {
        // TODO(vkoskela): Use AggregatedData host [MAI-104]
        final Messages.HostIdentification identifyHostMessage =
                Messages.HostIdentification.newBuilder()
                        .setHostName(_host)
                        .setClusterName(_cluster)
                        .build();
        socket.write(AggregationMessage.create(identifyHostMessage).serialize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Buffer serialize(final AggregatedData datum) {
        final List<Double> sampleValues = FluentIterable.from(datum.getSamples()).transform(EXTRACT_VALUES_FROM_SAMPLES).toList();
        final Messages.AggregationRecord recordMessage = Messages.AggregationRecord.newBuilder()
                .setMetric(datum.getMetric())
                .setPeriod(datum.getPeriod().toString())
                .setPeriodStart(datum.getPeriodStart().toString())
                .setService(datum.getService())
                .setStatistic(datum.getStatistic().getName())
                .setStatisticValue(datum.getValue())
                .setPopulationSize(datum.getPopulationSize())
                .addAllSamples(sampleValues)
                .build();

        return AggregationMessage.create(recordMessage).serialize();
    }

    private AggregationServerSink(final Builder builder) {
        super(builder);
        _host = builder._host;
        _cluster = builder._cluster;
    }

    private final String _host;
    private final String _cluster;
    private static final Function<Quantity, Double> EXTRACT_VALUES_FROM_SAMPLES = new Function<Quantity, Double>() {
        @Override
        public Double apply(final Quantity input) {
            return input != null ? Double.valueOf(input.getValue()) : null;
        }
    };

    /**
     * Implementation of builder pattern for <code>AggreationServerSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends VertxSink.Builder<Builder> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(AggregationServerSink.class);
            setServerPort(Integer.valueOf(7065));
        }

        // CHECKSTYLE.OFF: DuplicateCode - These values are used in Monitord
        // sink as well; however, both should be removed/reduced with completion
        // of Jiras MAI-103 and MAI-104.

        /**
         * The cluster identifier. Cannot be null or empty.
         *
         * @param value The cluster identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return self();
        }

        /**
         * The host identified. Cannot be null or empty.
         *
         * @param value The host identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
            return self();
        }

        // CHECKSTYLE.ON: DuplicateCode

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        @NotEmpty
        private String _cluster;
        @NotNull
        @NotEmpty
        private String _host;
    }
}
