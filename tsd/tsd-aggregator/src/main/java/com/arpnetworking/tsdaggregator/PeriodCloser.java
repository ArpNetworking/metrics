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
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Responsible for managing aggregation buckets for a period.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
/* package private */ final class PeriodCloser implements Runnable {

    /**
     * Shutdown this <code>PeriodCloser</code>. Cannot be restarted.
     */
    public void shutdown() {
        _isRunning = false;
    }

    /**
     * Process a <code>Record</code>.
     *
     * @param record Instance of <code>Record</code> to process.
     */
    public void record(final Record record) {
        // Find an existing bucket for the record
        final Duration timeout = getPeriodTimeout(_period);
        final DateTime start = getStartTime(record.getTime(), _period);
        final DateTime expiration = max(DateTime.now().plus(timeout), start.plus(_period).plus(timeout));
        Bucket bucket = _bucketsByStart.get(start);

        // Create a new bucket if one does not exist
        if (bucket == null) {
            // Pre-emptively add the record to the _new_ bucket. This avoids
            // the race condition after indexing by expiration between adding
            // the record and closing the bucket.
            final Bucket newBucket = _bucketBuilder
                    .setStart(start)
                    .build();
            newBucket.add(record);

            // Resolve bucket creation race condition; either:
            // 1) We won and can proceed to index the new bucket
            // 2) We lost and can proceed to add data to the existing bucket
            bucket = _bucketsByStart.putIfAbsent(start, newBucket);
            if (bucket == null) {
                LOGGER.debug()
                        .setMessage("Created new bucket")
                        .addData("bucket", newBucket)
                        .log();

                // Index the bucket by its expiration date; the expiration date is always in the future
                List<Bucket> bucketsAtExpiration = _bucketsByExpiration.get(expiration);
                if (bucketsAtExpiration == null) {
                    final List<Bucket> newBucketsAtExpiration = Lists.newCopyOnWriteArrayList();
                    newBucketsAtExpiration.add(newBucket);

                    bucketsAtExpiration = _bucketsByExpiration.putIfAbsent(
                            expiration,
                            newBucketsAtExpiration);
                    if (bucketsAtExpiration != null) {
                        bucketsAtExpiration.add(newBucket);
                    }
                }

                // New bucket created and indexed with record
                return;
            }
        }

        // Add the record to the _existing_ bucket
        bucket.add(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> LOGGER.error()
                        .setMessage("Unhandled exception")
                        .addData("periodCloser", PeriodCloser.this)
                        .setThrowable(throwable)
                        .log());

        while (_isRunning) {
            try {
                DateTime now = DateTime.now();
                final DateTime rotateAt = getRotateAt(now);
                Duration timeToRotate = new Duration(now, rotateAt);
                while (_isRunning && timeToRotate.isLongerThan(Duration.ZERO)) {
                    Thread.sleep(Math.min(timeToRotate.getMillis(), 100));
                    now = DateTime.now();
                    timeToRotate = new Duration(now, rotateAt);
                }
                rotate(now);
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
                        .addData("periodCloser", this)
                        .setThrowable(e)
                        .log();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Period", _period)
                .add("BucketBuilder", _bucketBuilder)
                .toString();

    }

    /* package private */ void rotate(final DateTime now) {
        final Map<DateTime, List<Bucket>> expiredBucketMap = _bucketsByExpiration.headMap(now);
        int closedBucketCount = 0;

        // Phase 1: Update thresholds and close expired buckets
        for (final Map.Entry<DateTime, List<Bucket>> entry : expiredBucketMap.entrySet()) {
            for (final Bucket bucket : entry.getValue()) {
                // Close the bucket
                bucket.close();
                ++closedBucketCount;

                LOGGER.debug()
                        .setMessage("Bucket closed")
                        .addData("periodCloser", this)
                        .addData("bucket", bucket)
                        .addData("now", now)
                        .log();
            }
        }

        // Phase 2: Remove expired buckets
        for (final Map.Entry<DateTime, List<Bucket>> entry : expiredBucketMap.entrySet()) {
            // Remove all buckets at the expiration date
            for (final Bucket bucket : _bucketsByExpiration.remove(entry.getKey())) {
                if (bucket.isOpen()) {
                    LOGGER.error()
                            .setMessage("Dropped bucket still open")
                            .addData("periodCloser", this)
                            .addData("bucket", bucket)
                            .addData("now", now)
                            .log();
                }
                _bucketsByStart.remove(bucket.getStart());
            }
        }

        LOGGER.debug().setMessage("Rotated").addData("count", closedBucketCount).log();
    }

    /* package private */ DateTime getRotateAt(final DateTime now) {
        final Map.Entry<DateTime, List<Bucket>> firstEntry = _bucketsByExpiration.firstEntry();
        final DateTime periodFirstExpiration = firstEntry == null ? null : firstEntry.getKey();
        if (periodFirstExpiration != null && periodFirstExpiration.isAfter(now)) {
            return periodFirstExpiration;
        }
        return now.plus(_rotationCheck);
    }

    /* package private */ static Duration getPeriodTimeout(final Period period) {
        // TODO(vkoskela): Support separate configurable timeouts per period. [MAI-499]
        final Duration halfPeriodDuration = period.toStandardDuration().dividedBy(2);
        if (MAXIMUM_PERIOD_TIMEOUT.isShorterThan(halfPeriodDuration)) {
            return MAXIMUM_PERIOD_TIMEOUT;
        }
        return halfPeriodDuration;
    }

    /* package private */ static DateTime getStartTime(final DateTime dateTime, final Period period) {
        // This effectively uses Jan 1, 1970 at 00:00:00 as the anchor point
        // for non-standard bucket sizes (e.g. 18 min) that do not divide
        // equally into an hour or day. Such use cases are rather uncommon.
        final long periodMillis = period.toStandardDuration().getMillis();
        final long dateTimeMillis = dateTime.getMillis();
        return new DateTime(dateTimeMillis - (dateTimeMillis % periodMillis), DateTimeZone.UTC);
    }

    /* package private */ static DateTime max(final DateTime dateTime1, final DateTime dateTime2) {
        if (dateTime1.isAfter(dateTime2)) {
            return dateTime1;
        }
        return dateTime2;
    }

    private PeriodCloser(final Builder builder) {
        _period = builder._period;
        _bucketBuilder = builder._bucketBuilder;
    }

    private volatile boolean _isRunning = true;

    private final Period _period;
    private final Bucket.Builder _bucketBuilder;
    private final Duration _rotationCheck = Duration.millis(100);
    private final ConcurrentSkipListMap<DateTime, Bucket> _bucketsByStart = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<DateTime, List<Bucket>> _bucketsByExpiration = new ConcurrentSkipListMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodCloser.class);
    private static final Duration MAXIMUM_PERIOD_TIMEOUT = Duration.standardMinutes(10);

    /**
     * <code>Builder</code> implementation for <code>PeriodCloser</code>.
     */
    public static final class Builder extends OvalBuilder<PeriodCloser> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(PeriodCloser.class);
        }

        /**
         * Set the period. Cannot be null or empty.
         *
         * @param value The periods.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPeriod(final Period value) {
            _period = value;
            return this;
        }

        /**
         * Set the <code>Bucket</code> <code>Builder</code>. Cannot be null.
         *
         * @param value The bucket builder.
         * @return This <code>Builder</code> instance.
         */
        public Builder setBucketBuilder(final Bucket.Builder value) {
            _bucketBuilder = value;
            return this;
        }

        @NotNull
        private Period _period;
        @NotNull
        private Bucket.Builder _bucketBuilder;
    }
}
