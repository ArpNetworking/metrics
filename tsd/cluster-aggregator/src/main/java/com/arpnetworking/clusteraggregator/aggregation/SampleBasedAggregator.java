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
import akka.contrib.pattern.ShardRegion;
import com.arpnetworking.clusteraggregator.AggDataUnifier;
import com.arpnetworking.clusteraggregator.AggregatorLifecycle;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.Lists;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Actual actor responsible for aggregating.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SampleBasedAggregator extends UntypedActor {

    /**
     * Creates a <code>Props</code> for use in Akka.
     *
     * @param lifecycleTracker Where to register the liveliness of this aggregator.
     * @param metricsListener Where to send metrics about aggregation computations.
     * @param emitter Where to send the metrics data.
     * @return A new <code>Props</code>.
     */
    public static Props props(final ActorRef lifecycleTracker, final ActorRef metricsListener, final ActorRef emitter) {
        return Props.create(SampleBasedAggregator.class, lifecycleTracker, metricsListener, emitter);
    }

    /**
     * Public constructor.
     *
     * @param lifecycleTracker Where to register the liveliness of this aggregator.
     * @param periodicStatistics Where to send metrics about aggregation computations.
     * @param emitter Where to send the metrics data.
     */
    @Inject
    public SampleBasedAggregator(
            @Named("bookkeeper-proxy") final ActorRef lifecycleTracker,
            @Named("periodic-statistics") final ActorRef periodicStatistics,
            @Named("emitter") final ActorRef emitter) {
        _lifecycleTracker = lifecycleTracker;
        _periodicStatistics = periodicStatistics;
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
        if (message instanceof AggregatedData) {
            LOGGER.debug()
                    .setMessage("Processing an AggregatedData")
                    .addData("workItem", message)
                    .addContext("actor", self())
                    .log();
            processAggregationMessage((AggregatedData) message);
        } else if (message instanceof BucketCheck) {
            if (_initialized) {
                while (_aggBuckets.size() > 0) {
                    final SampledAggregationBucket bucket = _aggBuckets.getFirst();
                    if (bucket.getPeriodStart().plus(_period).plus(AGG_TIMEOUT).isBeforeNow()) {
                        _aggBuckets.removeFirst();
                        //The units may be different coming from different machines
                        //Need to unify them
                        final List<AggregatedData> aggData = AggDataUnifier.unify(bucket.getAggregatedData());
                        final Quantity computed = _statistic.calculateAggregations(aggData);
                        long sampleCount = 0;
                        for (final AggregatedData aggDatum : aggData) {
                            sampleCount += aggDatum.getSamples().size();
                        }

                        final AggregatedData result = _resultBuilder
                                .setStart(bucket.getPeriodStart())
                                .setValue(computed)
                                .setPopulationSize(sampleCount)
                                .setIsSpecified(bucket.isSpecified())
                                .build();
                        LOGGER.info()
                                .setMessage("Computed result")
                                .addData("result", result)
                                .addContext("actor", self())
                                .log();
                        _emitter.tell(result, getSelf());

                        _periodicStatistics.tell(result, getSelf());
                    } else {
                        //Walk of the list is complete
                        break;
                    }
                }
            }
        } else if (message instanceof UpdateBookkeeper) {
            if (_resultBuilder != null) {
                _lifecycleTracker.tell(new AggregatorLifecycle.NotifyAggregatorStarted(_resultBuilder.build()), getSelf());
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

    private void processAggregationMessage(final AggregatedData data) {
        //First message sets the data we know about this actor
        if (!_initialized) {
            _period = data.getPeriod();
            _cluster = data.getFQDSN().getCluster();
            _metric = data.getFQDSN().getMetric();
            _service = data.getFQDSN().getService();
            _statistic = data.getFQDSN().getStatistic();
            _resultBuilder = new AggregatedData.Builder()
                    .setFQDSN(
                            new FQDSN.Builder()
                                    .setCluster(_cluster)
                                    .setMetric(_metric)
                                    .setService(_service)
                                    .setStatistic(_statistic)
                                    .build())
                    .setHost(_cluster + "-cluster")
                    .setPeriod(_period)
                    .setPopulationSize(1L)
                    .setSamples(Collections.<Quantity>emptyList())
                    .setStart(DateTime.now().hourOfDay().roundFloorCopy())
                    .setValue(new Quantity.Builder().setValue(0d).build())
                    .setIsSpecified(data.isSpecified());

            _lifecycleTracker.tell(new AggregatorLifecycle.NotifyAggregatorStarted(_resultBuilder.build()), getSelf());

            _initialized = true;
            LOGGER.debug()
                    .setMessage("Initialized aggregator")
                    .addContext("actor", self())
                    .log();
        } else if (!(_period.equals(data.getPeriod()))
                && _cluster.equals(data.getFQDSN().getCluster())
                && _service.equals(data.getFQDSN().getService())
                && _metric.equals(data.getFQDSN().getMetric())
                && _statistic.equals(data.getFQDSN().getStatistic())) {
            LOGGER.error()
                    .setMessage("Received a work item for another aggregator")
                    .addData("workItem", data)
                    .addContext("actor", self())
                    .log();
        }
        //Find the time bucket to dump this in
        if (_aggBuckets.size() > 0 && _aggBuckets.getFirst().getPeriodStart().isAfter(data.getPeriodStart())) {
            //We got a bit of data that is too old for us to aggregate.
            LOGGER.warn()
                    .setMessage("Received a work item that is too old to aggregate")
                    .addData("bucketStart", _aggBuckets.getFirst().getPeriodStart())
                    .addData("workItem", data)
                    .addContext("actor", self())
                    .log();
        } else {
            if (_aggBuckets.size() == 0 || _aggBuckets.getLast().getPeriodStart().isBefore(data.getPeriodStart())) {
                //We need to create a new bucket to hold this data.
                LOGGER.debug()
                        .setMessage("Creating new aggregation bucket for period")
                        .addData("period", data.getPeriodStart())
                        .addContext("actor", self())
                        .log();
                _aggBuckets.add(new SampledAggregationBucket(data.getPeriodStart()));
            }
            final Iterator<SampledAggregationBucket> bucketIterator = _aggBuckets.iterator();
            SampledAggregationBucket currentBucket;
            SampledAggregationBucket correctBucket = null;
            while (bucketIterator.hasNext()) {
                currentBucket = bucketIterator.next();
                if (currentBucket.getPeriodStart().equals(data.getPeriodStart())) {
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
                correctBucket.addAggregatedData(data);
            }
        }
    }

    private final LinkedList<SampledAggregationBucket> _aggBuckets = Lists.newLinkedList();
    private final ActorRef _emitter;
    private final ActorRef _lifecycleTracker;
    private final ActorRef _periodicStatistics;
    private boolean _initialized = false;
    private Period _period;
    private String _cluster;
    private String _metric;
    private String _service;
    private Statistic _statistic;
    private AggregatedData.Builder _resultBuilder;
    private static final Duration AGG_TIMEOUT = Duration.standardMinutes(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleBasedAggregator.class);

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
