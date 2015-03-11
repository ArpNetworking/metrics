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
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.Launchable;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.observer.Observable;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Performs aggregation of <code>Record</code> instances per <code>Period</code>.
 * This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class Aggregator implements Observer, Launchable {

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch() {
        LOGGER.debug()
                .setMessage("Launching aggregator")
                .addData("aggregator", this)
                .log();

        _periodClosers.clear();
        if (!_periods.isEmpty()) {
            _periodCloserExecutor = Executors.newFixedThreadPool(_periods.size());
            // TODO(vkoskela): Convert to scheduled thread executor [MAI-468]
            for (final Period period : _periods) {
                final PeriodCloser periodCloser = new PeriodCloser(period);
                _periodClosers.add(periodCloser);
                _periodCloserExecutor.submit(periodCloser);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        LOGGER.debug()
                .setMessage("Stopping aggregator")
                .addData("aggregator", this)
                .log();

        for (final PeriodCloser periodCloser : _periodClosers) {
            periodCloser.shutdown();
        }
        _periodClosers.clear();
        if (_periodCloserExecutor != null) {
            _periodCloserExecutor.shutdown();
            _periodCloserExecutor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notify(final Observable observable, final Object event) {
        if (!(event instanceof Record)) {
            LOGGER.error()
                    .setMessage("Observed unsupported event")
                    .addData("event", event)
                    .log();
            return;
        }
        final Record record = (Record) event;
        LOGGER.trace()
                .setMessage("Processing record")
                .addData("record", record)
                .log();
        for (final Map.Entry<Period, ConcurrentSkipListMap<DateTime, Bucket>> entry : _openBuckets.entrySet()) {
            final Period period = entry.getKey();
            final ConcurrentSkipListMap<DateTime, Bucket> periodBuckets = entry.getValue();

            // Discard records prior to earliest acceptable date
            final DateTime periodThreshold = _periodThresholds.get(period);
            if (periodThreshold.isAfter(record.getTime())) {
                LOGGER.warn()
                        .setMessage("Discarding record")
                        .addData("reason", "Before period threshold")
                        .addData("record", record)
                        .addData("period", period)
                        .addData("threshold", periodThreshold)
                        .log();
                return;
            }

            // Either find an existing bucket for the record or create a new one
            final DateTime start = getStartTime(record.getTime(), period);
            Bucket bucket = periodBuckets.get(start);
            if (bucket == null) {
                final Bucket newBucket = new Bucket.Builder()
                        .setSink(_sink)
                        .setCluster(_cluster)
                        .setService(_service)
                        .setHost(_host)
                        .setStart(start)
                        .setPeriod(period)
                        .setCounterStatistics(_counterStatistics)
                        .setGaugeStatistics(_gaugeStatistics)
                        .setTimerStatistics(_timerStatistics)
                        .build();
                bucket = periodBuckets.putIfAbsent(start, newBucket);
                if (bucket == null) {
                    LOGGER.debug()
                            .setMessage("Created new bucket")
                            .addData("bucket", newBucket)
                            .log();
                    bucket = newBucket;
                }
            }

            // Add the record to the bucket
            bucket.add(record);

            // Track the latest record start time
            _lastRecordTime.accumulate(record.getTime().getMillis());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Service", _service)
                .add("Cluster", _cluster)
                .add("Host", _host)
                .add("Sink", _sink)
                .add("TimerStatistics", _timerStatistics)
                .add("CounterStatistics", _counterStatistics)
                .add("GaugeStatistics", _gaugeStatistics)
                .add("PeriodThresholds", _periodThresholds)
                .add("OpenBuckets", _openBuckets)
                .add("LastRecordTime", _lastRecordTime)
                .toString();

    }

    /* package private */ static void rotate(
            final DateTime now,
            final Period period,
            final ConcurrentSkipListMap<DateTime, Bucket> periodBuckets,
            final Map<Period, DateTime> periodThresholds) {
        final DateTime expirationDate = now.minus(getPeriodTimeout(period));
        final SortedMap<DateTime, Bucket> expiredBuckets = periodBuckets.headMap(expirationDate);
        final int expiredCount = expiredBuckets.size();
        for (final Map.Entry<DateTime, Bucket> expiredBucketEntry : expiredBuckets.entrySet()) {
            final Bucket bucket = expiredBucketEntry.getValue();

            // Update the period threshold
            final DateTime bucketThreshold = bucket.getThreshold();
            final DateTime periodThreshold = periodThresholds.get(period);
            if (bucketThreshold.isAfter(periodThreshold)) {
                periodThresholds.put(period, bucketThreshold);
            }

            // Remove and close the bucket
            periodBuckets.remove(expiredBucketEntry.getKey());
            bucket.close();

            LOGGER.debug()
                    .setMessage("Closed bucket")
                    .addData("bucket", bucket)
                    .log();
        }

        LOGGER.debug().setMessage("Rotated").addData("count", expiredCount).log();
    }

    /* package private */ static DateTime getStartTime(final DateTime dateTime, final Period period) {
        // This effectively uses Jan 1, 1970 at 00:00:00 as the anchor point
        // for non-standard bucket sizes (e.g. 18 min) that do not divide
        // equally into an hour or day. Such use cases are rather uncommon.
        final long periodMillis = period.toStandardDuration().getMillis();
        final long dateTimeMillis = dateTime.getMillis();
        return new DateTime(dateTimeMillis - (dateTimeMillis % periodMillis), DateTimeZone.UTC);
    }

    /* package private */ static Duration getPeriodTimeout(final Period period) {
        // TODO(vkoskela): Support separate configurable timeouts per period. [MAI-?]
        final Duration halfPeriodDuration = period.toStandardDuration().dividedBy(2);
        if (MAXIMUM_PERIOD_TIMEOUT.isShorterThan(halfPeriodDuration)) {
            return period.toStandardDuration().plus(MAXIMUM_PERIOD_TIMEOUT);
        }
        return period.toStandardDuration().plus(halfPeriodDuration);
    }

    private Aggregator(final Builder builder) {
        _periods = ImmutableSet.copyOf(builder._periods);
        _service = builder._service;
        _cluster = builder._cluster;
        _host = builder._host;
        _sink = builder._sink;
        _counterStatistics = ImmutableSet.copyOf(builder._counterStatistics);
        _gaugeStatistics = ImmutableSet.copyOf(builder._gaugeStatistics);
        _timerStatistics = ImmutableSet.copyOf(builder._timerStatistics);

        final DateTime theBeginning = new DateTime(0);
        _periodThresholds = Maps.newConcurrentMap();
        final Map<Period, ConcurrentSkipListMap<DateTime, Bucket>> bucketsByPeriod = Maps.newHashMap();
        for (final Period period : _periods) {
            bucketsByPeriod.put(period, new ConcurrentSkipListMap<DateTime, Bucket>());
            _periodThresholds.put(period, theBeginning);
        }
        _openBuckets = Collections.unmodifiableMap(bucketsByPeriod);
    }

    private final ImmutableSet<Period> _periods;
    private final String _service;
    private final String _cluster;
    private final String _host;
    private final Sink _sink;
    private final ImmutableSet<Statistic> _timerStatistics;
    private final ImmutableSet<Statistic> _counterStatistics;
    private final ImmutableSet<Statistic> _gaugeStatistics;
    private final Map<Period, ConcurrentSkipListMap<DateTime, Bucket>> _openBuckets;
    private final Map<Period, DateTime> _periodThresholds;
    private final ArrayList<PeriodCloser> _periodClosers = Lists.newArrayList();
    private final LongAccumulator _lastRecordTime = new LongAccumulator(Math::max, 0L);

    private ExecutorService _periodCloserExecutor = null;

    private static final Duration MAXIMUM_PERIOD_TIMEOUT = Duration.standardMinutes(10);
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);

    private final class PeriodCloser implements Runnable {

        public PeriodCloser(final Period period) {
            _period = period;
        }

        @Override
        public void run() {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final Thread thread, final Throwable throwable) {
                    LOGGER.error()
                            .setMessage("Unhandled exception")
                            .addData("aggregator", Aggregator.this)
                            .setThrowable(throwable)
                            .log();
                }
            });

            while (_isRunning) {
                try {
                    DateTime recordNow = new DateTime(_lastRecordTime.get());
                    final DateTime rotateAt = getRotateAt(recordNow);
                    Duration timeToRotate = new Duration(recordNow, rotateAt);
                    while (_isRunning && timeToRotate.isLongerThan(Duration.ZERO)) {
                        Thread.sleep(Math.min(timeToRotate.getMillis(), 100));
                        recordNow = new DateTime(_lastRecordTime.get());
                        timeToRotate = new Duration(recordNow, rotateAt);
                    }
                    rotate(recordNow, _period, _openBuckets.get(_period), _periodThresholds);
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                    LOGGER.warn()
                            .setMessage("Interrupted waiting to close buckets")
                            .setThrowable(e)
                            .log();
                    // CHECKSTYLE.OFF: IllegalCatch - Top level catch to prevent thread death
                } catch (final Exception e) {
                    // CHECKSTYLE.ON: IllegalCatch
                    LOGGER.error()
                            .setMessage("Aggregator failure")
                            .addData("aggregator", Aggregator.this)
                            .setThrowable(e)
                            .log();
                }
            }
        }

        private DateTime getRotateAt(final DateTime now) {
            final DateTime threshold = Aggregator.this._periodThresholds.get(_period);
            if (threshold != null) {
                final Duration periodTimeout = getPeriodTimeout(_period);
                final DateTime rotateAt = threshold.plus(periodTimeout).plus(_rotationCheck);
                if (rotateAt.isAfter(now)) {
                    return rotateAt;
                }
            }
            return now.plus(_rotationCheck);
        }

        public void shutdown() {
            _isRunning = false;
        }

        private volatile boolean _isRunning = true;
        private final Period _period;
        private final Duration _rotationCheck = Duration.millis(100);
    }

    /**
     * <code>Builder</code> implementation for <code>Aggregator</code>.
     */
    public static final class Builder extends OvalBuilder<Aggregator> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(Aggregator.class);
        }

        /**
         * Set the service. Cannot be null or empty.
         *
         * @param value The service.
         * @return This <code>Builder</code> instance.
         */
        public Builder setService(final String value) {
            _service = value;
            return this;
        }

        /**
         * Set the cluster. Cannot be null or empty.
         *
         * @param value The cluster.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * Set the host. Cannot be null or empty.
         *
         * @param value The host.
         * @return This <code>Builder</code> instance.
         */
        public Builder setHost(final String value) {
            _host = value;
            return this;
        }

        /**
         * Set the sink. Cannot be null or empty.
         *
         * @param value The sink.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * Set the periods. Cannot be null or empty.
         *
         * @param value The periods.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPeriods(final Set<Period> value) {
            _periods = value;
            return this;
        }

        /**
         * Set the timer statistics. Cannot be null or empty.
         *
         * @param value The timer statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setTimerStatistics(final Set<Statistic> value) {
            _timerStatistics = value;
            return this;
        }

        /**
         * Set the counter statistics. Cannot be null or empty.
         *
         * @param value The counter statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCounterStatistics(final Set<Statistic> value) {
            _counterStatistics = value;
            return this;
        }

        /**
         * Set the gauge statistics. Cannot be null or empty.
         *
         * @param value The gauge statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setGaugeStatistics(final Set<Statistic> value) {
            _gaugeStatistics = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _service;
        @NotNull
        @NotEmpty
        private String _cluster;
        @NotNull
        @NotEmpty
        private String _host;
        @NotNull
        private Sink _sink;
        @NotNull
        private Set<Period> _periods;
        @NotNull
        private Set<Statistic> _timerStatistics;
        @NotNull
        private Set<Statistic> _counterStatistics;
        @NotNull
        private Set<Statistic> _gaugeStatistics;
    }
}
