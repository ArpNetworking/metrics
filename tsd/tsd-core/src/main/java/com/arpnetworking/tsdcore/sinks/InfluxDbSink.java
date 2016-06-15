/**
 * Copyright 2016 Groupon.com
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

import akka.http.javadsl.model.HttpMethods;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import org.joda.time.format.ISOPeriodFormat;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Publishes to a InfluxDB endpoint. This class is thread safe.
 *
 * @author Daniel Guerrero (dguerreromartin at groupon dot com)
 */
public final class InfluxDbSink extends HttpPostSink {


    /**
     * {@inheritDoc}
     */
    @Override
    protected Request createRequest(final AsyncHttpClient client, final byte[] serializedData) {
        return new RequestBuilder()
                .setUrl(getUri().toString())
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .setBody(serializedData)
                .setMethod(HttpMethods.POST.value())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<byte[]> serialize(final PeriodicData periodicData) {
        final String period = periodicData.getPeriod()
            .toString(ISOPeriodFormat.standard());

        final Map<String, MetricFormat> metrics = Maps.newHashMap();

        for (final AggregatedData data : periodicData.getData()) {
            final String metricName = buildMetricName(period, data.getFQDSN());
            MetricFormat formattedData = metrics.get(metricName);

            if (formattedData == null) {
                formattedData = new MetricFormat(
                        metricName,
                        periodicData.getStart().getMillis(),
                        periodicData.getDimensions()
                )
                        .addTag("service", data.getFQDSN().getService())
                        .addTag("cluster", data.getFQDSN().getCluster());

                metrics.put(metricName, formattedData);
            }

            formattedData.addMetric(
                    data.getFQDSN().getStatistic().getName(),
                    data.getValue().getValue()
            );
            //TODO(dguerreromartin): include Conditional
        }

        final StringJoiner dataList = new StringJoiner("\n");
        for (MetricFormat metric : metrics.values()) {
            dataList.add(metric.buildMetricString());
        }

        return Lists.newArrayList(dataList.toString().getBytes(StandardCharsets.UTF_8));
    }


    private String buildMetricName(final String period, final FQDSN fqdsn) {
        return new StringBuilder()
                .append(period).append(".")
                .append(fqdsn.getMetric())
                .toString();
    }

    /**
     * Implementation of output format for <code>InfluxDB</code> metrics.
     * The format follow the pattern (https://docs.influxdata.com/influxdb/v0.10/write_protocols/write_syntax/):
     *      measurement[,tag_key1=tag_value1...] field_key=field_value[,field_key2=field_value2] [timestamp]
     *
     * The spaces, comma and = will be escaped from the measurement,tags and fields
     *
     * @author Daniel Guerrero (dguerreromartin at groupon dot com)
     */
    private static class MetricFormat {

        public MetricFormat addTag(final String tagName, final String value) {
            this._tags.put(encode(tagName), encode(value));
            return this;
        }

        public MetricFormat addMetric(final String statisticName, final Double value) {
            this._values.put(encode(statisticName), value);
            return this;
        }

        public String buildMetricString() {
            final StringJoiner metricName = new StringJoiner(",");
            metricName.add(_metric);

            for (Map.Entry entryTag : this._tags.entrySet()) {
                metricName.add(String.format("%s=%s", entryTag.getKey(), entryTag.getValue()));
            }

            final StringJoiner valuesJoiner = new StringJoiner(",");

            for (Map.Entry entryValue : this._values.entrySet()) {
                valuesJoiner.add(String.format("%s=%s", entryValue.getKey(), entryValue.getValue()));
            }

            return String.format("%s %s %d", metricName.toString(), valuesJoiner.toString(), _timestamp);
        }

        MetricFormat(final String metric, final long timestamp, final Map<String, String> tags) {
            this._metric = encode(metric);
            this._timestamp = timestamp;
            for (final Map.Entry<String, String> tag : tags.entrySet()) {
                this._tags.put(encode(tag.getKey()), encode(tag.getValue()));
            }
        }

        private String encode(final String name) {
            return name
                    .replace(",", "\\,")
                    .replace(" ", "\\ ")
                    .replace("=", "_");
        }

        private final String _metric;
        private final long _timestamp;
        private final Map<String, Double> _values = Maps.newHashMap();
        private final Map<String, String> _tags = Maps.newHashMap();

    }

    /**
     * Private constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    private InfluxDbSink(final Builder builder) {
        super(builder);
    }

    /**
     * Implementation of builder pattern for <code>InfluxDbSink</code>.
     *
     * @author Daniel Guerrero (dguerreromartin at groupon dot com)
     */
    public static final class Builder extends HttpPostSink.Builder<Builder, InfluxDbSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(InfluxDbSink.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }
    }

}
