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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.tsdcore.limiter.MetricsLimiter;
import com.arpnetworking.tsdcore.limiter.NoLimitMetricsLimiter;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Applies a <code>MetricsLimiter</code> to limit the number of
 * <code>AggregatedData</code> instances recorded by the underlying
 * <code>Sink</code> instance.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class LimitingSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final DateTime now = DateTime.now();
        final List<AggregatedData> filteredData = Lists.newArrayListWithExpectedSize(data.size());
        final List<Condition> filteredConditions = Lists.newArrayListWithExpectedSize(conditions.size());
        final Multimap<FQDSN, Condition> conditionsByFQDSN = Multimaps.index(conditions, Condition::getFQDSN);
        final Set<FQDSN> filteredFQDSNs = Sets.newHashSet();
        long limited = 0;
        for (final AggregatedData datum : data) {
            if (_metricsLimiter.offer(datum, now)) {
                filteredData.add(datum);
                filteredFQDSNs.add(datum.getFQDSN());
            } else {
                LOGGER.warn(String.format(
                        "%s: Skipping publication of limited data; aggregatedData=%s",
                        getName(),
                        datum));
                ++limited;
            }
        }
        for (final FQDSN fqdsn : filteredFQDSNs) {
            filteredConditions.addAll(conditionsByFQDSN.get(fqdsn));
        }
        _limited.getAndAdd(limited);
        _sink.recordAggregateData(filteredData, filteredConditions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        _sink.close();
        try {
            _executor.shutdown();
            _executor.awaitTermination(EXECUTOR_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw Throwables.propagate(e);
        }
        flushMetrics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("MetricsLimiter", _metricsLimiter)
                .add("Sink", _sink)
                .add("Limited", _limited.get())
                .toString();
    }

    private void flushMetrics() {
        final Metrics newMetrics = createMetrics();
        final Metrics oldMetrics = _metrics.getAndSet(newMetrics);

        // Record statistics and close
        oldMetrics.incrementCounter(_limitedName, _limited.getAndSet(0));
        oldMetrics.close();
    }

    private Metrics createMetrics() {
        final Metrics metrics = _metricsFactory.create();
        metrics.resetCounter(_limitedName);
        return metrics;
    }

    private LimitingSink(final Builder builder) {
        super(builder);
        _sink = builder._sink;

        final MetricsLimiter metricsLimiter;
        if (builder._metricsLimiter != null) {
            LOGGER.debug(String.format(
                    "Using injected metrics limiter; limiter=%s",
                    builder._metricsLimiter));
            metricsLimiter = builder._metricsLimiter;
        } else if (builder._injector != null && builder._metricsLimiterName != null) {
            LOGGER.debug(String.format(
                    "Using named metrics limiter; limiter=%s",
                    builder._metricsLimiterName));
            metricsLimiter = builder._injector.getInstance(Key.get(MetricsLimiter.class, Names.named(builder._metricsLimiterName)));
        } else {
            LOGGER.debug("Using no limit metrics limiter");
            metricsLimiter = new NoLimitMetricsLimiter();
        }
        _metricsLimiter = metricsLimiter;

        _metricsFactory = builder._metricsFactory;
        _limitedName = "Sinks/LimitingSink/" + getMetricSafeName() + "/Limited";
        _metrics.set(createMetrics());

        // Write the metrics periodically
        _executor.scheduleAtFixedRate(
                new MetricsLogger(),
                builder._intervalInMilliseconds,
                builder._intervalInMilliseconds,
                TimeUnit.MILLISECONDS);
    }

    private final Sink _sink;
    private final MetricsLimiter _metricsLimiter;
    private final MetricsFactory _metricsFactory;
    private final AtomicReference<Metrics> _metrics = new AtomicReference<>();
    private final AtomicLong _limited = new AtomicLong(0);
    private final String _limitedName;
    private final ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitingSink.class);
    private static final int EXECUTOR_TIMEOUT_IN_SECONDS = 30;

    private final class MetricsLogger implements Runnable {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            flushMetrics();
        }
    }

    /**
     * Implementation of builder pattern for <code>LimitingSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, LimitingSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(LimitingSink.class);
        }

        /**
         * The aggregated data sink to limit. Cannot be null.
         *
         * @param value The aggregated data sink to limit.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * The interval in milliseconds between statistic flushes. Cannot be null;
         * minimum 1. Default is 1.
         *
         * @param value The interval in seconds between flushes.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setIntervalInMilliseconds(final Long value) {
            _intervalInMilliseconds = value;
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
         * The <code>MetricsLimiter</code>. Optional. The default is to use
         * a <code>NoLimitMetricsLimiter</code> instance. This takes place
         * over looking up a <code>MetricsLimiter</code> instance by injection.
         *
         * @param value The state directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetricsLimiter(final MetricsLimiter value) {
            _metricsLimiter = value;
            return this;
        }

        /**
         * The metrics limiter name. Optional. The default is to use
         * a <code>NoLimitMetricsLimiter</code> instance. This will be overridden
         * by a specified <code>MetricsLimiter</code> instance.
         *
         * @param value The state directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetricsLimiterName(final String value) {
            _metricsLimiterName = value;
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
        private Sink _sink;
        private MetricsLimiter _metricsLimiter;
        @NotEmpty
        private String _metricsLimiterName;
        @JacksonInject
        private Injector _injector;
        @NotNull
        @Min(value = 1)
        private Long _intervalInMilliseconds = 500L;
        @JacksonInject
        @NotNull
        private MetricsFactory _metricsFactory;
    }
}
