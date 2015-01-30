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

package com.arpnetworking.clusteraggregator.client;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.util.ByteString;
import com.arpnetworking.clusteraggregator.StatisticFactory;
import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.AggregationMessage;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.protobuf.GeneratedMessage;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.vertx.java.core.buffer.Buffer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * An actor that handles the data sent from an agg client.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggClientConnection extends UntypedActor {
    /**
     * Creates a <code>Props</code> for use in Akka.
     *
     * @param connection Reference to the client connection actor.
     * @param remote The address of the client socket.
     * @return A new <code>Props</code>.
     */
    public static Props props(final ActorRef connection, final InetSocketAddress remote) {
        return Props.create(AggClientConnection.class, connection, remote);
    }

    /**
     * Public constructor.
     *
     * @param connection Reference to the client connection actor.
     * @param remote The address of the client socket.
     */
    public AggClientConnection(final ActorRef connection, final InetSocketAddress remote) {
        _connection = connection;
        _remoteAddress = remote;

        getContext().watch(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof Tcp.Received) {
            final Tcp.Received received = (Tcp.Received) message;
            final ByteString data = received.data();
            _log.debug(String.format("received a message of %d bytes", data.length()));
            _buffer.appendBytes(data.toArray());
            processMessages();
//            throw new IllegalArgumentException("testing supervisor");
        } else if (message instanceof Tcp.ConnectionClosed) {
            getContext().stop(getSelf());
        } else {
            unhandled(message);
        }

    }

    private void processMessages() {
        Buffer current = _buffer;
        Optional<AggregationMessage> messageOptional = AggregationMessage.deserialize(current);
        while (messageOptional.isPresent()) {
            final AggregationMessage message = messageOptional.get();
            current = current.getBuffer(message.getLength(), current.length());
            final GeneratedMessage gm = message.getMessage();
            if (gm instanceof Messages.HostIdentification) {
                final Messages.HostIdentification hostIdent = (Messages.HostIdentification) gm;
                if (hostIdent.hasHostName()) {
                    _hostName = Optional.fromNullable(hostIdent.getHostName());
                }
                if (hostIdent.hasClusterName()) {
                    _clusterName = Optional.fromNullable(hostIdent.getClusterName());
                }
                _log.info(String.format("Handshake from host %s in cluster %s", _hostName.or(""), _clusterName.or("")));
            } else if (gm instanceof Messages.AggregationRecord) {
                final Messages.AggregationRecord aggRecord = (Messages.AggregationRecord) gm;
                _log.info(String.format("Aggregation from host %s in cluster %s",
                                          _hostName.or(""),
                                          _clusterName.or("")));
                final Optional<AggregatedData> aggData = getAggData(aggRecord);
                if (aggData.isPresent()) {
                    getContext().parent().tell(aggData.get(), getSelf());
                }
            } else if (gm instanceof Messages.LegacyAggRecord) {
                final Messages.LegacyAggRecord aggRecord = (Messages.LegacyAggRecord) gm;
                _log.info(String.format("Legacy aggregation from host %s in cluster %s",
                                          _hostName.or(""),
                                          _clusterName.or("")));
                final Optional<AggregatedData> aggData = getAggData(aggRecord);
                if (aggData.isPresent()) {
                    getContext().parent().tell(aggData.get(), getSelf());
                }
            } else if (gm instanceof Messages.HeartbeatRecord) {
                final Messages.HeartbeatRecord heartbeatRecord = (Messages.HeartbeatRecord) gm;
                _log.info(String.format(
                        "Heartbeat record; timestamp=%s from host %s in cluster %s",
                        heartbeatRecord.getTimestamp(),
                        _hostName.or(""),
                        _clusterName.or("")));
            } else {
                _log.warning(String.format("Unknown message type! type=%s", gm.getClass()));
            }
            messageOptional = AggregationMessage.deserialize(current);
            if (!messageOptional.isPresent() && current.length() > 4) {
                _log.warning(String.format("buffer did not deserialize with %d bytes left", current.length()));
            }
        }
        //TODO(barp): Investigate using a ring buffer [MAI-196]
        _buffer = current;
    }


    private Optional<AggregatedData> getAggData(final Messages.LegacyAggRecord aggRecord) {
        try {
            long sampleCount = 1;
            if (aggRecord.hasRawSampleCount()) {
                sampleCount = aggRecord.getRawSampleCount();
            } else if (aggRecord.getStatisticSamplesCount() > 0) {
                sampleCount = aggRecord.getStatisticSamplesCount();
            }


            final Period period = Period.parse(aggRecord.getPeriod());
            DateTime periodStart;
            if (aggRecord.hasPeriodStart()) {
                periodStart = DateTime.parse(aggRecord.getPeriodStart());
            } else {
                periodStart = DateTime.now().withTime(DateTime.now().getHourOfDay(), 0, 0, 0);
                while (periodStart.plus(period).isBeforeNow()) {
                    periodStart = periodStart.plus(period);
                }
            }

            final Optional<Statistic> statisticOptional = _statisticFactory.createStatistic(aggRecord.getStatistic());
            if (!statisticOptional.isPresent()) {
                _log.error(String.format("Unsupported statistic %s", aggRecord.getStatistic()));
                return Optional.absent();
            }

            return Optional.of(new AggregatedData.Builder().setHost(_hostName.get())
                .setFQDSN(new FQDSN.Builder()
                        .setCluster(_clusterName.get())
                        .setService(aggRecord.getService())
                        .setMetric(aggRecord.getMetric())
                        .setStatistic(statisticOptional.get())
                        .build())
                .setPeriod(Period.parse(aggRecord.getPeriod()))
                .setStart(periodStart)
                .setPopulationSize(sampleCount)
                .setSamples(sampleizeDoubles(aggRecord.getStatisticSamplesList(), Optional.<Unit>absent()))
                .setValue(new Quantity(aggRecord.getStatisticValue(), Optional.<Unit>absent()))
                .build());
        // CHECKSTYLE.OFF: IllegalCatch - The legacy parsing can throw a variety of runtime exceptions
        } catch (final RuntimeException e) {
        // CHECKSTYLE.ON: IllegalCatch
            _log.error("Caught an error parsing legacy agg record", e);
            return Optional.absent();
        }
    }

    private Optional<AggregatedData> getAggData(final Messages.AggregationRecord aggRecord) {
        final Optional<Statistic> statisticOptional = _statisticFactory.createStatistic(aggRecord.getStatistic());
        if (!statisticOptional.isPresent()) {
            _log.error(String.format("Unsupported statistic %s", aggRecord.getStatistic()));
            return Optional.absent();
        }
        final Optional<Unit> recordUnit;
        if (Strings.isNullOrEmpty(aggRecord.getUnit())) {
            recordUnit = Optional.absent();
        } else {
            recordUnit = Optional.fromNullable(Unit.valueOf(aggRecord.getUnit()));
        }
        final Quantity quantity = new Quantity(aggRecord.getStatisticValue(), recordUnit);
        return Optional.of(
                new AggregatedData.Builder()
                        .setHost(_hostName.get())
                        .setFQDSN(new FQDSN.Builder()
                            .setService(aggRecord.getService())
                            .setMetric(aggRecord.getMetric())
                            .setCluster(_clusterName.get())
                            .setStatistic(statisticOptional.get())
                            .build())
                        .setPeriod(Period.parse(aggRecord.getPeriod()))
                        .setStart(DateTime.parse(aggRecord.getPeriodStart()))
                        .setPopulationSize(aggRecord.getPopulationSize())
                        .setSamples(sampleizeDoubles(aggRecord.getSamplesList(), recordUnit))
                        .setValue(quantity)
                        .build());
    }

    private List<Quantity> sampleizeDoubles(final List<Double> samplesList, final Optional<Unit> recordUnit) {
        return FluentIterable.from(samplesList).transform(new Function<Double, Quantity>() {
            @Override
            public Quantity apply(final Double input) {
                return new Quantity(input, recordUnit.or(Optional.<Unit>absent()));
            }
        }).toList();
    }


    private Optional<String> _hostName = Optional.absent();
    private Optional<String> _clusterName = Optional.absent();
    private Buffer _buffer = new Buffer();
    private final ActorRef _connection;
    private final InetSocketAddress _remoteAddress;
    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);
    private final StatisticFactory _statisticFactory = new StatisticFactory();
}
