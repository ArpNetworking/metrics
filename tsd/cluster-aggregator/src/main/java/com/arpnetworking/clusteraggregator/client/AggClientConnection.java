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
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.model.AggregationMessage;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.statistics.CountStatistic;
import com.arpnetworking.tsdcore.statistics.HistogramStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.GeneratedMessage;
import scala.concurrent.duration.FiniteDuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
            LOGGER.debug()
                    .setMessage("Received a tcp message")
                    .addData("length", data.length())
                    .addContext("actor", self())
                    .log();
            _buffer = _buffer.concat(data);
            processMessages();
        } else if (message instanceof Tcp.CloseCommand) {
            LOGGER.info()
                    .setMessage("Connection timeout hit, cycling connection")
                    .addData("remote", _remoteAddress)
                    .addContext("actor", self())
                    .log();
            if (_connection != null) {
                _connection.tell(message, self());
            }
        } else if (message instanceof Tcp.ConnectionClosed) {
            getContext().stop(getSelf());
        } else if (message instanceof Terminated) {
            final Terminated terminated = (Terminated) message;
            LOGGER.info()
                    .setMessage("Connection actor terminated")
                    .addData("terminated", terminated.actor())
                    .addContext("actor", self())
                    .log();
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
                LOGGER.info()
                        .setMessage("Handshake received")
                        .addData("host", _hostName.or(""))
                        .addData("cluster", _clusterName.or(""))
                        .addContext("actor", self())
                        .log();
            } else if (gm instanceof Messages.StatisticSetRecord) {
                final Messages.StatisticSetRecord setRecord = (Messages.StatisticSetRecord) gm;
                LOGGER.debug()
                        .setMessage("StatisticSet record received")
                        .addData("aggregation", setRecord)
                        .addContext("actor", self())
                        .log();
                getContext().parent().tell(setRecord, getSelf());

            } else if (gm instanceof Messages.AggregationRecord && IS_ENABLED) {
                final Messages.AggregationRecord aggRecord = (Messages.AggregationRecord) gm;
                LOGGER.debug()
                        .setMessage("Aggregation received")
                        .addData("aggregation", aggRecord)
                        .addContext("actor", self())
                        .log();
                final Optional<Messages.StatisticSetRecord> record = createStatisticSetRecord(aggRecord);
                if (record.isPresent()) {
                    getContext().parent().tell(record.get(), getSelf());
                }
            } else if (gm instanceof Messages.HeartbeatRecord) {
                LOGGER.debug()
                        .setMessage("Heartbeat received")
                        .addData("host", _hostName.or(""))
                        .addData("cluster", _clusterName.or(""))
                        .addContext("actor", self())
                        .log();
            } else {
                LOGGER.warn()
                        .setMessage("Unknown message type")
                        .addData("type", gm.getClass())
                        .addContext("actor", self())
                        .log();
            }
            messageOptional = AggregationMessage.deserialize(current);
            if (!messageOptional.isPresent() && current.length() > 4) {
                LOGGER.debug()
                        .setMessage("buffer did not deserialize completely")
                        .addData("remainingBytes", current.length())
                        .addContext("actor", self())
                        .log();
            }
        }
        //TODO(barp): Investigate using a ring buffer [MAI-196]
        _buffer = current;
    }

    private Optional<Messages.StatisticSetRecord> createStatisticSetRecord(final Messages.AggregationRecord aggRecord) {
        final Optional<Statistic> statisticOptional = _statisticFactory.tryGetStatistic(aggRecord.getStatistic());
        if (!statisticOptional.isPresent()) {
            LOGGER.error()
                    .setMessage("Unsupported statistic")
                    .addData("name", aggRecord.getStatistic())
                    .addContext("actor", self())
                    .log();
            return Optional.absent();
        }

        final Messages.StatisticSetRecord.Builder setBuilder = Messages.StatisticSetRecord.newBuilder()
                .setCluster(_clusterName.get())
                .setMetric(aggRecord.getMetric())
                .setPeriod(aggRecord.getPeriod())
                .setPeriodStart(aggRecord.getPeriodStart())
                .setService(aggRecord.getService());

        // Counts dont have units, we dont want to mix a non-unit with a unit
        if (aggRecord.getSamplesList().size() > 0 && !(statisticOptional.get() instanceof CountStatistic)) {
            double sum = 0;
            final HistogramStatistic.Histogram histogram = new HistogramStatistic.Histogram();
            for (final Double val : aggRecord.getSamplesList()) {
                histogram.recordValue(val);
                sum += val;
            }
            final Messages.SparseHistogramSupportingData.Builder builder = Messages.SparseHistogramSupportingData.newBuilder();
            builder.setUnit(aggRecord.getUnit());
            for (final Map.Entry<Double, Integer> entry : histogram.getValues()) {
                builder.addEntriesBuilder()
                        .setBucket(entry.getKey())
                        .setCount(entry.getValue())
                        .build();
            }
            final com.google.protobuf.ByteString byteString = com.google.protobuf.ByteString.copyFrom(
                    AggregationMessage.create(builder.build()).serialize().getBytes());
            setBuilder.addStatisticsBuilder()
                    .setStatistic(aggRecord.getStatistic())
                    .setUnit(aggRecord.getUnit())
                    .setValue(aggRecord.getStatisticValue())
                    .setUserSpecified(true)
                    .build();
            setBuilder.addStatisticsBuilder()
                    .setStatistic("histogram")
                    .setUnit(aggRecord.getUnit())
                    .setValue(aggRecord.getStatisticValue())
                    .setSupportingData(byteString)
                    .setUserSpecified(false)
                    .build();
            setBuilder.addStatisticsBuilder()
                    .setStatistic("count")
                    .setUnit("")
                    .setValue(aggRecord.getPopulationSize())
                    .setUserSpecified(false)
                    .build();
            setBuilder.addStatisticsBuilder()
                    .setStatistic("sum")
                    .setUnit(aggRecord.getUnit())
                    .setValue(sum)
                    .setUserSpecified(false)
                    .build();
        } else {
            setBuilder.addStatisticsBuilder()
                    .setStatistic(aggRecord.getStatistic())
                    .setUnit(aggRecord.getUnit())
                    .setValue(aggRecord.getStatisticValue())
                    .setUserSpecified(true)
                    .build();
        }

        return Optional.of(setBuilder.build());
    }

    private List<Quantity> sampleizeDoubles(final List<Double> samplesList, final Optional<Unit> recordUnit) {
        final Quantity.Builder builder = new Quantity.Builder().setUnit(recordUnit.orNull());
        final int skipFactor = Math.max(samplesList.size() / 1000, 1); // Determine number to increment the index by

        // Target for 1k samples
        final Quantity[] samples = new Quantity[(int) Math.ceil(samplesList.size() / (double) skipFactor)];
        int index = 0;
        for (int samplesIndex = 0; samplesIndex < samplesList.size(); samplesIndex += skipFactor) {
            samples[index++] = builder.setValue(samplesList.get(samplesIndex)).build();
        }
        if (samplesList.size() != samples.length) {
            LOGGER.debug()
                    .setMessage("Downsampling AggregatedData")
                    .addData("originalSize", samplesList.size())
                    .addData("resampledSize", samples.length)
                    .addContext("actor", self())
                    .log();
        }
        return createImmutableListFromArray(samples);
    }

    private static <T> ImmutableList<T> createImmutableListFromArray(final T[] array) {
        try {
            @SuppressWarnings("unchecked")
            final ImmutableList<T> list = (ImmutableList<T>) IMMUTABLE_FROM_ARRAY.invoke(null, new Object[]{array});
            return list;
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<String> _hostName = Optional.absent();
    private Optional<String> _clusterName = Optional.absent();
    private ByteString _buffer = ByteString.empty();
    private final ActorRef _connection;
    private final InetSocketAddress _remoteAddress;
    private final StatisticFactory _statisticFactory = new StatisticFactory();
    private static final Logger LOGGER = LoggerFactory.getLogger(AggClientConnection.class);
    private static final Method IMMUTABLE_FROM_ARRAY;

    private static final boolean IS_ENABLED = true;

    static {
        try {
            IMMUTABLE_FROM_ARRAY = ImmutableList.class.getDeclaredMethod("asImmutableList", Object[].class);
            IMMUTABLE_FROM_ARRAY.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }
}
