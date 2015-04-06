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

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.Unit;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.sf.oval.constraint.NotNull;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> serialize(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final Multimap<String, AggregatedData> indexedData = prepareData(data);
        final Multimap<String, Condition> indexedConditions = prepareConditions(conditions);

        // Serialize
        final List<String> serializedData = Lists.newArrayListWithCapacity(indexedData.size());
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String key : indexedData.keySet()) {
            final Collection<AggregatedData> namedData = indexedData.get(key);
            if (!namedData.isEmpty()) {
                final AggregatedData first = Iterables.getFirst(namedData, null);
                final Period period = first.getPeriod();
                final String name = new StringBuilder()
                        .append(first.getFQDSN().getService())
                        .append("_")
                        .append(first.getPeriod().toString(ISOPeriodFormat.standard()))
                        .append("_")
                        .append(first.getFQDSN().getMetric())
                        .toString();

                int maxStatus = 0;
                final StringBuilder dataBuilder = new StringBuilder();
                for (final AggregatedData datum : namedData) {
                    dataBuilder.append(datum.getFQDSN().getStatistic().getName())
                            .append("%3D").append(datum.getValue().getValue())
                            .append("%3B");

                    final String conditionKey = datum.getFQDSN().getService() + "_"
                            + datum.getFQDSN().getMetric() + "_"
                            + datum.getFQDSN().getCluster() + "_"
                            + datum.getFQDSN().getStatistic();
                    for (final Condition condition : indexedConditions.get(conditionKey)) {
                        dataBuilder.append(datum.getFQDSN().getStatistic().getName())
                                .append("_").append(condition.getName())
                                .append("%3D").append(condition.getThreshold().getValue())
                                .append("%3B");

                        if (condition.isTriggered().isPresent() && condition.isTriggered().get()) {
                            // Collect the status of this metric
                            final Object severity = condition.getExtensions().get("severity");
                            int status = _unknownSeverityStatus;
                            if (severity != null && _severityToStatus.containsKey(severity)) {
                                status = _severityToStatus.get(severity);
                            }
                            maxStatus = Math.max(status, maxStatus);
                        }
                    }
                }

                stringBuilder.append("run_every=").append(period.toStandardSeconds().getSeconds())
                        .append("&path=").append(first.getFQDSN().getCluster()).append("/").append(first.getHost())
                        .append("&monitor=").append(name)
                        .append("&status=").append(maxStatus)
                        .append("&timestamp=").append((int) Unit.SECOND.convert(first.getPeriodStart().getMillis(), Unit.MILLISECOND))
                        .append("&output=").append(name)
                        .append("%7C")
                        .append(dataBuilder.toString());

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

    private Multimap<String, Condition> prepareConditions(final Collection<Condition> conditions) {
        // Transform conditions
        return Multimaps.index(
                conditions,
                new Function<Condition, String>() {
                    @Override
                    public String apply(final Condition input) {
                        // NOTE: It is assumed as part of serialization that
                        // that period is part of the unique metric name.
                        return input.getFQDSN().getService() + "_"
                                + input.getFQDSN().getMetric() + "_"
                                + input.getFQDSN().getCluster() + "_"
                                + input.getFQDSN().getStatistic();
                    }
                });
    }

    private Multimap<String, AggregatedData> prepareData(final Collection<AggregatedData> data) {
        // Transform the data list to a multimap by metric name
        // Ie, get all the statistics for a unique metric

        return Multimaps.index(
                data, input ->
                        input.getFQDSN().getService() + "_"
                                + input.getPeriod().toString(ISOPeriodFormat.standard()) + "_"
                                + input.getFQDSN().getMetric() + "_"
                                + input.getHost() + "_"
                                + input.getFQDSN().getCluster());
    }

    private MonitordSink(final Builder builder) {
        super(builder);
        _severityToStatus = Maps.newHashMap(builder._severityToStatus);
        _unknownSeverityStatus = builder._unknownSeverityStatus;
    }

    private final Map<String, Integer> _severityToStatus;
    private final int _unknownSeverityStatus;

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitordSink.class);

    /**
     * Implementation of builder pattern for <code>MonitordSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends HttpPostSink.Builder<Builder, MonitordSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(MonitordSink.class);
        }

        /**
         * Set severity to status map. Optional. Cannot be null. By default is
         * an <code>Map</code> containing the following:
         *
         * {@code
         * "warning" => 1
         * "critical" => 2
         * }
         *
         * @param value Map of severity to status.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSeverityToStatus(final Map<String, Integer> value) {
            _severityToStatus = value;
            return self();
        }

        /**
         * The status for unknown <code>Condition</code> severities; e.g. those
         * not found in the severity to status map. Optional. Cannot be null.
         * By default the status for a <code>Condition</code> is <code>2</code>.
         *
         * @param value Default status.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUnknownSeverityStatus(final Integer value) {
            _unknownSeverityStatus = value;
            return self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private Map<String, Integer> _severityToStatus = ImmutableMap.of(
                "warning", 1,
                "critical", 2);
        @NotNull
        private Integer _unknownSeverityStatus = 2;
    }
}
