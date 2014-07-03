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
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates and periodically logs metrics about the aggregated data being
 * record; effectively, this is metrics about metrics. It's primary purpose is
 * to provide a quick sanity check on installations by generating metrics that
 * the aggregator can then consume (and use to generate more metrics). This 
 * class is thread safe.
 * 
 * TODO(vkoskela): Remove synchronized blocks [MAI-110]
 * Details: The synchronization can be removed if the metrics client can
 * be configured to throw ISE when attempting to write to a closed instance.
 * This would allow a retry on the new instance; starvation would theoretically
 * be possible but practically should never happen.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class PeriodicStatisticsSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final List<AggregatedData> data) {
        LOGGER.debug(getName() + ": Writing aggregated data; size=" + data.size());

        synchronized (_metrics) {
            _metrics.incrementCounter(_counterName, data.size());
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
        } finally {
            synchronized (_metrics) {
                _statisticsWritten.incrementAndGet();
                _metrics.close();
            }
        }
        LOGGER.info(getName() + ": Closing sink; statisticsWritten=" + _statisticsWritten);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("CounterName", _counterName)
                .add("StatisticsWritten", _statisticsWritten)
                .toString();
    }

    private Metrics createMetrics() {
        final Metrics metrics = _metricsFactory.create();
        metrics.resetCounter(_counterName);
        return metrics;
    }

    private PeriodicStatisticsSink(final Builder builder) {
        super(builder);

        // Initialize the metrics factory and metrics instance
        _metricsFactory = builder._metricsFactory;
        _counterName = "Sinks/PeriodicStatisticsSink/" + getMetricSafeName() + "/AggregatedData";
        _metrics = createMetrics();

        // Write the metrics periodically
        _executor.scheduleAtFixedRate(
                new MetricsLogger(),
                builder._intervalInSeconds.longValue(),
                builder._intervalInSeconds.longValue(),
                TimeUnit.SECONDS);
    }

    private final MetricsFactory _metricsFactory;
    private volatile Metrics _metrics;
    private final String _counterName;
    private final AtomicLong _statisticsWritten = new AtomicLong(0);
    private final ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicStatisticsSink.class);
    private static final int EXECUTOR_TIMEOUT_IN_SECONDS = 30;

    private final class MetricsLogger implements Runnable {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                final Metrics newMetrics = createMetrics();
                synchronized (_metrics) {
                    final Metrics oldMetrics = _metrics;
                    _metrics = newMetrics;
                    oldMetrics.close();
                    _statisticsWritten.set(0);
                }
                // CHECKSTYLE.OFF: IllegalCatch - Intercept all exceptions
            } catch (final Throwable t) {
                // CHECKSTYLE.ON: IllegalCatch
                LOGGER.error(getName() + ": Failed to close metrics", t);
            }
        }
    }

    /**
     * Implementation of builder pattern for <code>PeriodicStatisticsSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder> {

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
