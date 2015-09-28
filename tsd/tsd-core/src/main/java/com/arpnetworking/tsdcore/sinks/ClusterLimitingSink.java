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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.limiter.DefaultMetricsLimiter;
import com.arpnetworking.tsdcore.limiter.MetricsLimiter;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Applies a <code>MetricsLimiter</code> to limit the number of
 * <code>AggregatedData</code> instances to each cluster. This sink effectively
 * splits <code>AggregatedData</code> by cluster. At this time this is not a
 * problem because all pipelines only process metrics for a single cluster at
 * a time.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ClusterLimitingSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final PeriodicData periodicData) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", periodicData.getData().size())
                .addData("conditionsSize", periodicData.getConditions().size())
                .log();

        final Multimap<String, AggregatedData> dataByCluster = Multimaps.index(
                periodicData.getData(),
                input -> {
                        return input.getFQDSN().getCluster();
                });
        final Multimap<String, Condition> conditionsByCluster = Multimaps.index(
                periodicData.getConditions(),
                condition -> {
                        return condition.getFQDSN().getCluster();
                });
        for (final String cluster : dataByCluster.keySet()) {
            // Obtain or create this cluster's limiting sink
            Sink clusterLimitingSink = _clusterMetricsLimitingSinks.get(cluster);
            if (clusterLimitingSink == null) {
                final Sink newClusterLimitingSink = _sinkFactory.create(
                        cluster,
                        _stateFileDirectory.toPath().resolve(
                                getMetricSafeName() + "_" + cluster + ".state").toFile());

                clusterLimitingSink = _clusterMetricsLimitingSinks.putIfAbsent(cluster, newClusterLimitingSink);
                if (clusterLimitingSink == null) {
                    LOGGER.debug()
                            .setMessage("Registered new limiting sink for cluster")
                            .addData("sink", getName())
                            .addData("cluster", cluster)
                            .addData("sink", newClusterLimitingSink)
                            .log();
                    clusterLimitingSink = newClusterLimitingSink;
                }
            }

            // Limit cluster's metrics and conditions
            clusterLimitingSink.recordAggregateData(
                    PeriodicData.Builder.clone(periodicData, new PeriodicData.Builder())
                            .setData(ImmutableList.copyOf(dataByCluster.get(cluster)))
                            .setConditions(ImmutableList.copyOf(conditionsByCluster.get(cluster)))
                            .build());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        for (final Sink sink : _clusterMetricsLimitingSinks.values()) {
            sink.close();
        }
        for (final MetricsLimiter limiter : _clusterMetricsLimiters) {
            limiter.shutdown();
        }
        _sink.close();
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("super", super.toLogValue())
                .put("clusterMetricsLimitingSinks", _clusterMetricsLimitingSinks)
                .build();
    }

    private ClusterLimitingSink(final Builder builder) {
        super(builder);
        _sink = builder._sink;
        _maximumAggregations = builder._maximumAggregations;
        _clusterMaximumAggregations = Maps.newHashMap(builder._clusterMaximumAggregations);
        _stateFileDirectory = builder._stateFileDirectory;
        _stateFlushInterval = builder._stateFlushInterval;
        _ageOutThreshold = builder._ageOutThreshold;
        _clusterMetricsLimitingSinks = Maps.newConcurrentMap();
        _clusterMetricsLimiters = Sets.newConcurrentHashSet();
        _intervalInMilliseconds = builder._intervalInMilliseconds;
        _metricsFactory = builder._metricsFactory;
        _sinkFactory = MoreObjects.firstNonNull(builder._sinkFactory, new DefaultLimitingSinkFactory());
    }

    private final Sink _sink;
    private final long _maximumAggregations;
    private final Map<String, Long> _clusterMaximumAggregations;
    private final File _stateFileDirectory;
    private final Period _stateFlushInterval;
    private final Period _ageOutThreshold;
    private final long _intervalInMilliseconds;
    private final MetricsFactory _metricsFactory;
    private final LimitingSinkFactory _sinkFactory;
    private final ConcurrentMap<String, Sink> _clusterMetricsLimitingSinks;
    private final Collection<MetricsLimiter> _clusterMetricsLimiters;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterLimitingSink.class);

    /* package private */ interface LimitingSinkFactory {
        Sink create(final String cluster, final File stateFile);
    }

    private final class DefaultLimitingSinkFactory implements LimitingSinkFactory {

        @Override
        public Sink create(final String cluster, final File stateFile) {
            final long maximum = _clusterMaximumAggregations.getOrDefault(cluster, _maximumAggregations);
            final MetricsLimiter limiter = new DefaultMetricsLimiter.Builder()
                    .setAgeOutThreshold(_ageOutThreshold)
                    .setMaxAggregations(maximum)
                    .setStateFile(stateFile)
                    .setStateFlushInterval(_stateFlushInterval)
                    .build();
            limiter.launch();
            _clusterMetricsLimiters.add(limiter);
            return new LimitingSink.Builder()
                    .setName("LimitingSink-" + cluster)
                    .setIntervalInMilliseconds(_intervalInMilliseconds)
                    .setMetricsFactory(_metricsFactory)
                    .setSink(_sink)
                    .setMetricsLimiter(limiter)
                    .build();
        }
    }

    /**
     * Implementation of builder pattern for <code>LimitingSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, ClusterLimitingSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(ClusterLimitingSink.class);
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
         * Set the state file directory. Required. Cannot be null or empty.
         *
         * @param value The state file directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFileDirectory(final File value) {
            _stateFileDirectory = value;
            return this;
        }

        /**
         * Set the maximum aggregations. Required. Cannot be null; must be
         * greater than or equal to zero.
         *
         * @param value The maximum aggregations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaxAggregations(final Long value) {
            _maximumAggregations = value;
            return this;
        }

        /**
         * Set the maximum aggregations per cluster. This overrides the maximum
         * aggregations. Optional. The default is an empty map. Cannot be null.
         *
         * @param value The maximum aggregations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setClusterMaxAggregations(final Map<String, Long> value) {
            _clusterMaximumAggregations = value;
            return this;
        }

        /**
         * Set the state flush interval. Optional. The default is five minutes.
         *
         * @param value The state flush interval.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFlushInterval(final Period value) {
            _stateFlushInterval = value;
            return this;
        }

        /**
         * Set the age out threshold. Optional. The default is seven days.
         *
         * @param value The age out threshold.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAgeOutThreshold(final Period value) {
            _ageOutThreshold = value;
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
         * Instance of <code>LimitingSinkFactory</code>. Cannot be null. This field
         * is package private and should not be set by clients.
         *
         * @param value Instance of <code>LimitingSinkFactory</code>.
         * @return This instance of <code>Builder</code>.
         */
        /* package private */ Builder setSinkFactory(final LimitingSinkFactory value) {
            _sinkFactory = value;
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
        @NotNull
        @Min(0)
        private Long _maximumAggregations;
        @NotNull
        private Map<String, Long> _clusterMaximumAggregations = Collections.emptyMap();
        @NotNull
        @NotEmpty
        private File _stateFileDirectory;
        @NotNull
        private Period _stateFlushInterval = Period.minutes(5);
        @NotNull
        private Period _ageOutThreshold = Period.days(7);
        @NotNull
        @Min(value = 1)
        private Long _intervalInMilliseconds = 500L;
        @JacksonInject
        @NotNull
        private MetricsFactory _metricsFactory;
        private LimitingSinkFactory _sinkFactory;
    }
}
