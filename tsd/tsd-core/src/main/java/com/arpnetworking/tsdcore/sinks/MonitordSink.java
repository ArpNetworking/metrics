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

import com.arpnetworking.tsdcore.limiter.DefaultMetricsLimiter;
import com.arpnetworking.tsdcore.limiter.MetricsLimiter;
import com.arpnetworking.tsdcore.limiter.MetricsLimiterStateManager;
import com.arpnetworking.tsdcore.limiter.NoLimitMetricsLimiter;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Publishes aggregations to Monitord. This class is thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class MonitordSink extends HttpPostSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("Host", _host)
                .add("Cluster", _cluster)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> serialize(final List<AggregatedData> data) {
        // Transform the data list to a multimap by metric name
        final ImmutableListMultimap<String, AggregatedData> indexedData = Multimaps.index(
                data,
                new Function<AggregatedData, String>() {
                    @Override
                    public String apply(final AggregatedData input) {
                        // NOTE: It is assumed as part of serialization that
                        // that period is part of the unique metric name.
                        return input.getService() + "_"
                                + input.getPeriod().toString(ISOPeriodFormat.standard()) + "_"
                                + input.getMetric();
                    }
                });

        // Filter the multimap
        final DateTime now = DateTime.now();
        final Multimap<String, AggregatedData> filteredData = Multimaps.filterValues(
                indexedData,
                // CHECKSTYLE.OFF: AnonInnerLength - Rewrite with MAI-91 and MAI-92.
                new Predicate<AggregatedData>() {
                    @Override
                    public boolean apply(final AggregatedData input) {
                        // Skip periods less than 1 minute
                        // TODO(vkoskela): This should be configurable [MAI-91]
                        if (input.getPeriod().toStandardDuration().isShorterThan(org.joda.time.Duration.standardMinutes(1))) {
                            return false;
                        }

                        // Skip the entry if the period start is too long ago,
                        // specifically not within 2x periods of the start
                        // period.
                        // TODO(vkoskela): This should be configurable [MAI-92]
                        if (now.isAfter(input.getPeriodStart().plus(input.getPeriod().multipliedBy(2)))) {
                            LOGGER.warn(getName() + ": Skipping publication of stale data; periodStart=" + input.getPeriodStart());
                            return false;
                        }

                        // Skip the entry if limited
                        if (!_metricsLimiter.offer(input, now)) {
                            LOGGER.warn(getName() + ": Skipping publication of limited data; aggregatedData=" + input);
                            return false;
                        }

                        // Otherwise accept the data
                        return true;
                    }
                });
        // CHECKSTYLE.ON: AnonInnerLength

        // Serialize
        final List<String> serializedData = Lists.newArrayListWithCapacity(filteredData.size());
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String name : filteredData.keySet()) {
            final Collection<AggregatedData> namedData = filteredData.get(name);
            if (!namedData.isEmpty()) {
                final Period period = Iterables.getFirst(namedData, null).getPeriod();
                // TODO(vkoskela): Get host from AggregatedData [MAI-103]
                stringBuilder.append("run_every=").append(period.toStandardSeconds().getSeconds())
                        .append("&path=").append(_cluster).append("/").append(_host)
                        .append("&monitor=").append(name)
                        .append("&status=0")
                        .append("&output=").append(name)
                        .append("%7C");

                for (final AggregatedData datum : namedData) {
                    stringBuilder.append(datum.getStatistic().getName())
                            .append("%3D").append(datum.getValue())
                            .append("%3B");
                }

                stringBuilder.setLength(stringBuilder.length() - 3);
                serializedData.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }

        return serializedData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpUriRequest createRequest(final String serializedData) {
        final StringEntity requestEntity = new StringEntity(serializedData, ContentType.APPLICATION_FORM_URLENCODED);
        final HttpPost request = new HttpPost(getUri());
        request.setEntity(requestEntity);
        return request;
    }

    private MetricsLimiter createMetricsLimiter() {
        MetricsLimiter metricsLimiter = new NoLimitMetricsLimiter();
        if (_maximumStatistics.isPresent() && _stateDirectory.isPresent()) {
            final MetricsLimiterStateManager.Builder limiterStateManagerBuilder = MetricsLimiterStateManager.builder()
                    .withStateFile(_stateDirectory.get().toPath().resolve("tsd-aggregator-state"));

            metricsLimiter = DefaultMetricsLimiter.builder()
                    .withMaxAggregations(_maximumStatistics.get().longValue())
                    .withStateManagerBuilder(limiterStateManagerBuilder)
                    .build();
        }
        return metricsLimiter;
    }

    private MonitordSink(final Builder builder) {
        super(builder);
        _cluster = builder._cluster;
        _host = builder._host;

        // TODO(vkoskela): This is a huge hack that needs to be fixed [MAI-208]
        _maximumStatistics = Optional.fromNullable(builder._maximumStatistics);
        _stateDirectory = Optional.fromNullable(builder._stateDirectory);
        _metricsLimiter = createMetricsLimiter();
    }

    private final String _cluster;
    private final String _host;
    private final Optional<Long> _maximumStatistics;
    private final Optional<File> _stateDirectory;
    private final MetricsLimiter _metricsLimiter;

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitordSink.class);

    /**
     * Implementation of builder pattern for <code>MonitordSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends HttpPostSink.Builder<Builder> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(MonitordSink.class);
        }

        /**
         * The cluster identifier. Cannot be null or empty.
         *
         * @param value The cluster identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return self();
        }

        /**
         * The host identified. Cannot be null or empty.
         *
         * @param value The host identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
            return self();
        }

        /**
         * The maximum statistics. Minimum 0. Optional; default is 0 which 
         * disables the metrics limiter.
         *
         * @param value The host identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaximumStatistics(final Long value) {
            _maximumStatistics = value;
            return self();
        }

        /**
         * The state directory. Cannot be null.
         *
         * @param value The state directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateDirectory(final File value) {
            _stateDirectory = value;
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
        @NotEmpty
        private String _cluster;
        @NotNull
        @NotEmpty
        private String _host;
        @Min(value = 0)
        private Long _maximumStatistics;
        private File _stateDirectory;
    }
}
