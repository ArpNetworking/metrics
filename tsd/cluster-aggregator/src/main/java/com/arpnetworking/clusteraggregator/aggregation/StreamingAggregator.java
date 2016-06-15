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
package com.arpnetworking.clusteraggregator.aggregation;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;
import akka.cluster.sharding.ShardRegion;
import com.arpnetworking.clusteraggregator.AggregatorLifecycle;
import com.arpnetworking.clusteraggregator.models.CombinedMetricData;
import com.arpnetworking.metrics.aggregation.protocol.Messages;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.CalculatedValue;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import scala.Option;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Actual actor responsible for aggregating.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StreamingAggregator extends UntypedActor {

    /**
     * Creates a <code>Props</code> for use in Akka.
     *
     * @param lifecycleTracker Where to register the liveliness of this aggregator.
     * @param metricsListener Where to send metrics about aggregation computations.
     * @param emitter Where to send the metrics data.
     * @param clusterHostSuffix The suffix to append to the hostname for cluster aggregations.
     * @return A new <code>Props</code>.
     */
    public static Props props(
            final ActorRef lifecycleTracker,
            final ActorRef metricsListener,
            final ActorRef emitter,
            final String clusterHostSuffix) {
        return Props.create(StreamingAggregator.class, lifecycleTracker, metricsListener, emitter, clusterHostSuffix);
    }

    /**
     * Public constructor.
     *
     * @param lifecycleTracker Where to register the liveliness of this aggregator.
     * @param periodicStatistics Where to send metrics about aggregation computations.
     * @param emitter Where to send the metrics data.
     * @param clusterHostSuffix The suffix to append to the hostname for cluster aggregations.
     */
    @Inject
    public StreamingAggregator(
            @Named("bookkeeper-proxy") final ActorRef lifecycleTracker,
            @Named("periodic-statistics") final ActorRef periodicStatistics,
            @Named("cluster-emitter") final ActorRef emitter,
            @Named("cluster-host-suffix") final String clusterHostSuffix) {
        _lifecycleTracker = lifecycleTracker;
        _periodicStatistics = periodicStatistics;
        _clusterHostSuffix = clusterHostSuffix;
        context().setReceiveTimeout(FiniteDuration.apply(30, TimeUnit.MINUTES));

        final Scheduler scheduler = getContext().system().scheduler();
        scheduler.schedule(
                FiniteDuration.apply(5, TimeUnit.SECONDS),
                FiniteDuration.apply(5, TimeUnit.SECONDS),
                getSelf(),
                new BucketCheck(),
                getContext().dispatcher(),
                getSelf());
        scheduler.schedule(
                FiniteDuration.apply(5, TimeUnit.SECONDS),
                FiniteDuration.apply(1, TimeUnit.HOURS),
                getSelf(),
                new UpdateBookkeeper(),
                getContext().dispatcher(),
                getSelf());
        _emitter = emitter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof Messages.StatisticSetRecord) {
            LOGGER.debug()
                    .setMessage("Processing a StatisticSetRecord")
                    .addData("workItem", message)
                    .addContext("actor", self())
                    .log();
            processAggregationMessage((Messages.StatisticSetRecord) message);
        } else if (message instanceof BucketCheck) {
            if (_initialized) {
                while (_aggBuckets.size() > 0) {
                    final StreamingAggregationBucket bucket = _aggBuckets.getFirst();
                    if (bucket.getPeriodStart().plus(_period).plus(AGG_TIMEOUT).isBeforeNow()) {
                        _aggBuckets.removeFirst();

                        // Walk over every statistic in the bucket
                        final Map<Statistic, CalculatedValue<?>> values = bucket.compute();
                        final ImmutableList.Builder<AggregatedData> builder = ImmutableList.builder();
                        for (final Map.Entry<Statistic, CalculatedValue<?>> entry : values.entrySet()) {
                            _statistics.add(entry.getKey());
                            final AggregatedData result = _resultBuilder
                                    .setFQDSN(
                                            new FQDSN.Builder()
                                                    .setCluster(_cluster)
                                                    .setMetric(_metric)
                                                    .setService(_service)
                                                    .setStatistic(entry.getKey())
                                                    .build())
                                    .setStart(bucket.getPeriodStart())
                                    .setValue(entry.getValue().getValue())
                                    .setSupportingData(entry.getValue().getData())
                                    .setPopulationSize(0L)
                                    .setIsSpecified(bucket.isSpecified(entry.getKey()))
                                    .build();
                            LOGGER.debug()
                                    .setMessage("Computed result")
                                    .addData("result", result)
                                    .addContext("actor", self())
                                    .log();
                            builder.add(result);

                            _periodicStatistics.tell(result, getSelf());
                        }
                        final PeriodicData periodicData = new PeriodicData.Builder()
                                .setData(builder.build())
                                .setDimensions(ImmutableMap.of("host", createHost()))
                                .setConditions(ImmutableList.of())
                                .setPeriod(_period)
                                .setStart(bucket.getPeriodStart())
                                .build();
                        _emitter.tell(periodicData, getSelf());
                    } else {
                        //Walk of the list is complete
                        break;
                    }
                }
            }
        } else if (message instanceof UpdateBookkeeper) {
            if (_resultBuilder != null) {
                for (final Statistic statistic : _statistics) {
                    _resultBuilder.setFQDSN(new FQDSN.Builder()
                                    .setCluster(_cluster)
                                    .setMetric(_metric)
                                    .setService(_service)
                                    .setStatistic(statistic)
                                    .build());
                    _lifecycleTracker.tell(new AggregatorLifecycle.NotifyAggregatorStarted(_resultBuilder.build()), getSelf());
                }
                _statistics.clear();
            }
        } else if (message instanceof ShutdownAggregator) {
            context().stop(self());
        } else if (message.equals(ReceiveTimeout.getInstance())) {
            getContext().parent().tell(new ShardRegion.Passivate(new ShutdownAggregator()), getSelf());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void preRestart(final Throwable reason, final Option<Object> message) throws Exception {
        LOGGER.error()
                .setMessage("Aggregator crashing")
                .setThrowable(reason)
                .addData("triggeringMessage", message.getOrElse(null))
                .addContext("actor", self())
                .log();
        super.preRestart(reason, message);
    }

    private void processAggregationMessage(final Messages.StatisticSetRecord data) {
        final CombinedMetricData metricData = CombinedMetricData.Builder.fromStatisticSetRecord(data).build();
        //First message sets the data we know about this actor
        if (!_initialized) {
            _period = metricData.getPeriod();
            _cluster = metricData.getCluster();
            _metric = metricData.getMetricName();
            _service = metricData.getService();
            _resultBuilder = new AggregatedData.Builder()
                    .setHost(createHost())
                    .setPeriod(_period)
                    .setPopulationSize(1L)
                    .setSamples(Collections.<Quantity>emptyList())
                    .setStart(DateTime.now().hourOfDay().roundFloorCopy())
                    .setValue(new Quantity.Builder().setValue(0d).build());


            _initialized = true;
            LOGGER.debug()
                    .setMessage("Initialized aggregator")
                    .addContext("actor", self())
                    .log();
        } else if (!(_period.equals(metricData.getPeriod()))
                && _cluster.equals(metricData.getCluster())
                && _service.equals(metricData.getService())
                && _metric.equals(metricData.getMetricName())) {
            LOGGER.error()
                    .setMessage("Received a work item for another aggregator")
                    .addData("workItem", data)
                    .addContext("actor", self())
                    .log();
        }
        //Find the time bucket to dump this in
        final DateTime periodStart = DateTime.parse(data.getPeriodStart());
        if (_aggBuckets.size() > 0 && _aggBuckets.getFirst().getPeriodStart().isAfter(periodStart)) {
            //We got a bit of data that is too old for us to aggregate.
            LOGGER.warn()
                    .setMessage("Received a work item that is too old to aggregate")
                    .addData("bucketStart", _aggBuckets.getFirst().getPeriodStart())
                    .addData("workItem", data)
                    .addContext("actor", self())
                    .log();
        } else {
            if (_aggBuckets.size() == 0 || _aggBuckets.getLast().getPeriodStart().isBefore(periodStart)) {
                //We need to create a new bucket to hold this data.
                LOGGER.debug()
                        .setMessage("Creating new aggregation bucket for period")
                        .addData("period", periodStart)
                        .addContext("actor", self())
                        .log();
                _aggBuckets.add(new StreamingAggregationBucket(periodStart));
            }
            final Iterator<StreamingAggregationBucket> bucketIterator = _aggBuckets.iterator();
            StreamingAggregationBucket currentBucket;
            StreamingAggregationBucket correctBucket = null;
            while (bucketIterator.hasNext()) {
                currentBucket = bucketIterator.next();
                if (currentBucket.getPeriodStart().equals(periodStart)) {
                    //We found the correct bucket
                    correctBucket = currentBucket;
                    break;
                }
            }

            if (correctBucket == null) {
                LOGGER.error()
                        .setMessage("No bucket found to aggregate into, bug in the bucket walk")
                        .addContext("actor", self())
                        .log();
            } else {
                LOGGER.debug()
                        .setMessage("Updating bucket")
                        .addData("bucket", correctBucket)
                        .addData("data", metricData)
                        .addContext("actor", self())
                        .log();
                correctBucket.update(metricData);
                LOGGER.debug()
                        .setMessage("Done updating bucket")
                        .addData("bucket", correctBucket)
                        .addContext("actor", self())
                        .log();
            }
        }
    }

    private String createHost() {
        return _cluster + "-cluster" + _clusterHostSuffix;
    }

    private final LinkedList<StreamingAggregationBucket> _aggBuckets = Lists.newLinkedList();
    private final ActorRef _emitter;
    private final ActorRef _lifecycleTracker;
    private final ActorRef _periodicStatistics;
    private final String _clusterHostSuffix;
    private final Set<Statistic> _statistics = Sets.newHashSet();
    private boolean _initialized = false;
    private Period _period;
    private String _cluster;
    private String _metric;
    private String _service;
    private AggregatedData.Builder _resultBuilder;
    private static final Duration AGG_TIMEOUT = Duration.standardMinutes(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingAggregator.class);

    private static final class BucketCheck implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static final class UpdateBookkeeper implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static final class ShutdownAggregator implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
