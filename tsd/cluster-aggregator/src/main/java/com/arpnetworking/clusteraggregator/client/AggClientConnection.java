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
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.AggregationMessage;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;
import org.joda.time.DateTime;
import org.joda.time.Period;
import scala.concurrent.duration.FiniteDuration;

import java.net.InetSocketAddress;
import java.util.Collections;
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
     * @param maxConnectionAge The maximum duration to keep a connection open before cycling it.
     * @return A new <code>Props</code>.
     */
    public static Props props(final ActorRef connection, final InetSocketAddress remote, final FiniteDuration maxConnectionAge) {
        return Props.create(AggClientConnection.class, connection, remote, maxConnectionAge);
    }

    /**
     * Public constructor.
     *
     * @param connection Reference to the client connection actor.
     * @param remote The address of the client socket.
     * @param maxConnectionAge The maximum duration to keep a connection open before cycling it.
     */
    public AggClientConnection(
            final ActorRef connection,
            final InetSocketAddress remote,
            final FiniteDuration maxConnectionAge) {
        _connection = connection;
        _remoteAddress = remote;

        getContext().watch(connection);

        context().system().scheduler().scheduleOnce(
                maxConnectionAge,
                self(),
                TcpMessage.close(),
                context().dispatcher(),
                self());
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
            _buffer = _buffer.concat(data);
            processMessages();
        } else if (message instanceof Tcp.CloseCommand) {
            _log.info(String.format("connection timeout hit, cycling connection to %s", _remoteAddress));
            if (_connection != null) {
                _connection.tell(message, self());
            }
        } else if (message instanceof Tcp.ConnectionClosed) {
            getContext().stop(getSelf());
        } else if (message instanceof Terminated) {
            final Terminated terminated = (Terminated) message;
            _log.info(String.format("connection actor terminated; self=%s, terminated=%s", getSelf(), terminated.actor()));
            if (terminated.actor().equals(_connection)) {
                getContext().stop(getSelf());
            } else {
                unhandled(message);
            }
        } else {
            unhandled(message);
        }

    }

    private void processMessages() {
        ByteString current = _buffer;
        Optional<AggregationMessage> messageOptional = AggregationMessage.deserialize(current);
        while (messageOptional.isPresent()) {
            final AggregationMessage message = messageOptional.get();
            current = current.drop(message.getLength());
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
                if (_log.isDebugEnabled()) {
                    _log.debug(
                            String.format(
                                    "Aggregation received; host=%s, cluster=%s, service=%s, metric=%s, statistic=%s, period=%s, ",
                                    _hostName.or(""),
                                    _clusterName.or(""),
                                    aggRecord.getService(),
                                    aggRecord.getMetric(),
                                    aggRecord.getStatistic(),
                                    aggRecord.getPeriod()));
                }
                final Optional<AggregatedData> aggData = getAggData(aggRecord);
                if (aggData.isPresent()) {
                    getContext().parent().tell(aggData.get(), getSelf());
                }
            } else if (gm instanceof Messages.LegacyAggRecord) {
                final Messages.LegacyAggRecord aggRecord = (Messages.LegacyAggRecord) gm;
                if (_log.isDebugEnabled()) {
                    _log.debug(
                            String.format(
                                    "Legacy aggregation received; host=%s, cluster=%s, service=%s, metric=%s, statistic=%s, period=%s, ",
                                    _hostName.or(""),
                                    _clusterName.or(""),
                                    aggRecord.getService(),
                                    aggRecord.getMetric(),
                                    aggRecord.getStatistic(),
                                    aggRecord.getPeriod()));
                }
                final Optional<AggregatedData> aggData = getAggData(aggRecord);
                if (aggData.isPresent()) {
                    getContext().parent().tell(aggData.get(), getSelf());
                }
            } else if (gm instanceof Messages.HeartbeatRecord) {
                final Messages.HeartbeatRecord heartbeatRecord = (Messages.HeartbeatRecord) gm;
                if (_log.isDebugEnabled()) {
                    _log.debug(
                            String.format(
                                    "Heartbeat record; timestamp=%s from host %s in cluster %s",
                                    heartbeatRecord.getTimestamp(),
                                    _hostName.or(""),
                                    _clusterName.or("")));
                }
            } else {
                _log.warning(String.format("Unknown message type! type=%s", gm.getClass()));
            }
            messageOptional = AggregationMessage.deserialize(current);
            if (!messageOptional.isPresent() && current.length() > 4) {
                _log.debug(String.format("buffer did not deserialize with %d bytes left", current.length()));
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
                .setValue(new Quantity.Builder()
                        .setValue(aggRecord.getStatisticValue())
                        .build())
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
        final Quantity quantity = new Quantity.Builder()
                .setValue(aggRecord.getStatisticValue())
                .setUnit(recordUnit.orNull())
                .build();
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
        final Quantity.Builder builder = new Quantity.Builder().setUnit(recordUnit.orNull());
        final List<Quantity> samples = Lists.newArrayListWithCapacity(samplesList.size());
        for (final Double sample : samplesList) {
            samples.add(builder.setValue(sample).build());
        }
        return Collections.unmodifiableList(samples);
    }


    private Optional<String> _hostName = Optional.absent();
    private Optional<String> _clusterName = Optional.absent();
    private ByteString _buffer = ByteString.empty();
    private final ActorRef _connection;
    private final InetSocketAddress _remoteAddress;
    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);
    private final StatisticFactory _statisticFactory = new StatisticFactory();
}
