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
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.statistics.ExpressionStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import java.util.Collection;
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
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final List<AggregatedData> filteredData = Lists.newArrayList(
                Iterables.filter(data, new Predicate<AggregatedData>() {
                    @Override
                    public boolean apply(final AggregatedData input) {
                        if (input != null) {
                            return !input.getFQDSN().getStatistic().equals(EXPRESSION_STATISTIC);
                        }
                        return false;
                    }
                }));
        super.recordAggregateData(filteredData, conditions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConnect(final NetSocket socket) {
        _sentHandshake = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Buffer serialize(final AggregatedData datum) {
        final Buffer buffer = new Buffer();
        if (!_sentHandshake) {
            // TODO(barp): Revise aggregator sink protocol for host and cluster support [MAI-443]
            final String host = datum.getHost();
            final String cluster = datum.getFQDSN().getCluster();
            final Messages.HostIdentification identifyHostMessage =
                    Messages.HostIdentification.newBuilder()
                            .setHostName(host)
                            .setClusterName(cluster)
                            .build();
            buffer.appendBuffer(AggregationMessage.create(identifyHostMessage).serialize());
            LOGGER.debug(String.format("writing host identification message; hostName=%s, clusterName=%s", host, cluster));
            _sentHandshake = true;
        }

        final List<Double> sampleValues = FluentIterable.from(datum.getSamples()).transform(EXTRACT_VALUES_FROM_SAMPLES).toList();
        final String unit;
        if (datum.getValue().getUnit().isPresent()) {
            unit = datum.getValue().getUnit().get().toString();
        } else {
            unit = "";
        }
        final Messages.AggregationRecord recordMessage = Messages.AggregationRecord.newBuilder()
                .setMetric(datum.getFQDSN().getMetric())
                .setPeriod(datum.getPeriod().toString())
                .setPeriodStart(datum.getPeriodStart().toString())
                .setService(datum.getFQDSN().getService())
                .setStatistic(datum.getFQDSN().getStatistic().getName())
                .setStatisticValue(datum.getValue().getValue())
                .setPopulationSize(datum.getPopulationSize())
                .addAllSamples(sampleValues)
                .setUnit(unit)
                .build();

        buffer.appendBuffer(AggregationMessage.create(recordMessage).serialize());
        return buffer;
    }

    private void heartbeat() {

        final Messages.HeartbeatRecord message = Messages.HeartbeatRecord.newBuilder()
                .setTimestamp(DateTime.now().toString())
                .build();
        sendRawData(AggregationMessage.create(message).serialize());
        LOGGER.debug(getName() + ": Heartbeat sent to aggregation server");
    }

    private AggregationServerSink(final Builder builder) {
        super(builder);
        super.getVertx().setPeriodic(15000, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                LOGGER.trace("Heartbeat tick.");
                heartbeat();
            }
        });
    }

    private boolean _sentHandshake = false;

    private static final Function<Quantity, Double> EXTRACT_VALUES_FROM_SAMPLES = new Function<Quantity, Double>() {
        @Override
        public Double apply(final Quantity input) {
            return input != null ? input.getValue() : null;
        }
    };

    private static final Statistic EXPRESSION_STATISTIC = new ExpressionStatistic();
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationServerSink.class);

    /**
     * Implementation of builder pattern for <code>AggreationServerSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends VertxSink.Builder<Builder, AggregationServerSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(AggregationServerSink.class);
            setServerPort(7065);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }
    }
}
