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

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Unit;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregates and periodically logs metrics about the aggregated data being
 * record; effectively, this is metrics about metrics. It's primary purpose is
 * to provide a quick sanity check on installations by generating metrics that
 * the aggregator can then consume (and use to generate more metrics). This
 * class is thread safe.
 *
 * TODO(vkoskela): Remove synchronized blocks [MAI-110]
 *
 * Details: The synchronization can be removed if the metrics client can
 * be configured to throw ISE when attempting to write to a closed instance.
 * This would allow a retry on the new instance; starvation would theoretically
 * be possible but practically should never happen.
 *
 * (+) The implementation of _age as an AtomicLong currently relies on the
 * locking provided by the synchronized block to perform it's check and set.
 * This can be replaced with a separate lock or a thread-safe accumulator
 * implementation.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class PeriodicStatisticsSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        LOGGER.debug(String.format("%s: Writing aggregated data; size=%d", getName(), data.size()));

        final long now = System.currentTimeMillis();
        _aggregatedData.addAndGet(data.size());
        for (final AggregatedData datum : data) {
            final String fqsn = new StringBuilder()
                    .append(datum.getFQDSN().getCluster()).append(".")
                    .append(datum.getHost()).append(".")
                    .append(datum.getFQDSN().getService()).append(".")
                    .append(datum.getFQDSN().getMetric()).append(".")
                    .append(datum.getFQDSN().getStatistic()).append(".")
                    .append(datum.getPeriod())
                    .toString();

            final String metricName = new StringBuilder()
                    .append(datum.getFQDSN().getService()).append(".")
                    .append(datum.getFQDSN().getMetric())
                    .toString();

            _uniqueMetrics.get().add(metricName);

            _uniqueStatistics.get().add(fqsn);

            updateMax(_age, now - datum.getPeriodStart().plus(datum.getPeriod()).getMillis());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            _executor.shutdown();
            _executor.awaitTermination(EXECUTOR_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            Throwables.propagate(e);
        }
        flushMetrics(_metrics.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("AggregatedDataName", _aggregatedDataName)
                .add("UnqiueMetricsName", _uniqueMetricsName)
                .add("UniqueStatisticsName", _uniqueStatisticsName)
                .add("AgeName", _ageName)
                .add("AggregatedData", _aggregatedData)
                .toString();
    }

    private void flushMetrics(final Metrics metrics) {
        // Gather and reset state
        final Set<String> oldUniqueMetrics = _uniqueMetrics.getAndSet(
                createConcurrentSet(_uniqueMetrics.get()));
        final Set<String> oldUniqueStatistics = _uniqueStatistics.getAndSet(
                createConcurrentSet(_uniqueStatistics.get()));

        // Record statistics and close
        metrics.incrementCounter(_aggregatedDataName, _aggregatedData.getAndSet(0));
        metrics.incrementCounter(_uniqueMetricsName, oldUniqueMetrics.size());
        metrics.incrementCounter(_uniqueStatisticsName, oldUniqueStatistics.size());
        metrics.setGauge(_ageName, _age.getAndSet(0), Unit.fromTimeUnit(TimeUnit.MILLISECONDS));
        metrics.close();
    }

    private Metrics createMetrics() {
        final Metrics metrics = _metricsFactory.create();
        metrics.resetCounter(_aggregatedDataName);
        metrics.resetCounter(_uniqueMetricsName);
        metrics.resetCounter(_uniqueStatisticsName);
        return metrics;
    }

    private Set<String> createConcurrentSet(final Set<String> existingSet) {
        final int initialCapacity = (int) (existingSet.size() / 0.75);
        return Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>(initialCapacity));
    }

    private void updateMax(final AtomicLong maximum, final long sample) {
        // TODO(vkoskela): Replace with Java 8's LongAccumulator [MAI-328]
        while (true) {
            final long currentMaximum = maximum.longValue();
            if (currentMaximum >= sample) {
                break;
            }
            final boolean success = maximum.compareAndSet(currentMaximum, sample);
            if (success) {
                break;
            }
        }
    }

    // NOTE: Package private for testing
    /* package private */PeriodicStatisticsSink(final Builder builder, final ScheduledExecutorService executor) {
        super(builder);

        // Initialize the metrics factory and metrics instance
        _metricsFactory = builder._metricsFactory;
        _aggregatedDataName = "Sinks/PeriodicStatisticsSink/" + getMetricSafeName() + "/AggregatedData";
        _uniqueMetricsName = "Sinks/PeriodicStatisticsSink/" + getMetricSafeName() + "/UniqueMetrics";
        _uniqueStatisticsName = "Sinks/PeriodicStatisticsSink/" + getMetricSafeName() + "/UniqueStatistics";
        _ageName = "Sinks/PeriodicStatisticsSink/" + getMetricSafeName() + "/Age";
        _metrics.set(createMetrics());

        // Write the metrics periodically
        _executor = executor;
        _executor.scheduleAtFixedRate(
                new MetricsLogger(),
                builder._intervalInSeconds.longValue(),
                builder._intervalInSeconds.longValue(),
                TimeUnit.SECONDS);
    }


    private PeriodicStatisticsSink(final Builder builder) {
        this(builder, Executors.newSingleThreadScheduledExecutor());
    }

    private final MetricsFactory _metricsFactory;
    private final AtomicReference<Metrics> _metrics = new AtomicReference<>();

    private final AtomicLong _age = new AtomicLong(0);
    private final String _aggregatedDataName;
    private final String _uniqueMetricsName;
    private final String _uniqueStatisticsName;
    private final String _ageName;
    private final AtomicLong _aggregatedData = new AtomicLong(0);
    private final AtomicReference<Set<String>> _uniqueMetrics = new AtomicReference<>(
            Sets.newSetFromMap(Maps.<String, Boolean>newConcurrentMap()));
    private final AtomicReference<Set<String>> _uniqueStatistics = new AtomicReference<>(
            Sets.newSetFromMap(Maps.<String, Boolean>newConcurrentMap()));

    private final ScheduledExecutorService _executor;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicStatisticsSink.class);
    private static final int EXECUTOR_TIMEOUT_IN_SECONDS = 30;

    private final class MetricsLogger implements Runnable {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            final Metrics oldMetrics = _metrics.getAndSet(createMetrics());
            flushMetrics(oldMetrics);
        }
    }

    /**
     * Implementation of builder pattern for <code>PeriodicStatisticsSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, PeriodicStatisticsSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(PeriodicStatisticsSink.class);
        }

        /**
         * The interval in seconds between statistic flushes. Cannot be null;
         * minimum 1. Default is 1.
         *
         * @param value The interval in seconds between flushes.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setIntervalInSeconds(final Long value) {
            _intervalInSeconds = value;
            return this;
        }

        /**
         * Instance of <code>MetricsFactory</code>. Cannot be null. This field
         * may be injected automatically by Jackson/Guice if setup to do so.
         *
         * @param value Instance of <code>MetricsFactory</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetricsFactory(final MetricsFactory value) {
            _metricsFactory = value;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        @Min(value = 1)
        private Long _intervalInSeconds = Long.valueOf(1);
        @JacksonInject
        @NotNull
        private MetricsFactory _metricsFactory;
    }
}
