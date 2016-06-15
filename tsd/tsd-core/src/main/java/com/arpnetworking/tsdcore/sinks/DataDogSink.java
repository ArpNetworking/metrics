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

import akka.http.javadsl.model.HttpMethods;
import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.format.ISOPeriodFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Publishes aggregations to Data Dog. This class is thread safe.
 *
 * API Documentation:
 * http://docs.datadoghq.com/api/
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DataDogSink extends HttpPostSink {

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
                .put("apiKey", _apiKey)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<byte[]> serialize(final PeriodicData periodicData) {
        final String period = periodicData.getPeriod().toString(ISOPeriodFormat.standard());
        final long timestamp = (periodicData.getStart().getMillis() + periodicData.getPeriod().toStandardDuration().getMillis()) / 1000;

        final List<Datum> dataDogData = Lists.newArrayList();
        for (final AggregatedData datum : periodicData.getData()) {
            if (!datum.isSpecified()) {
                continue;
            }

            dataDogData.add(new Datum(
                    period + "_" + datum.getFQDSN().getMetric() + "_" + datum.getFQDSN().getStatistic().getName(),
                    timestamp,
                    (float) datum.getValue().getValue(),
                    periodicData.getDimensions().get("host"),
                    createTags(periodicData, datum)));
        }

        final String dataDogDataAsJson;
        try {
            dataDogDataAsJson = OBJECT_MAPPER.writeValueAsString(
                    Collections.singletonMap("series", dataDogData));
        } catch (final JsonProcessingException e) {
            LOGGER.error()
                    .setMessage("Serialization error")
                    .addData("periodicData", periodicData)
                    .setThrowable(e)
                    .log();
            return Collections.emptyList();
        }
        return Collections.singletonList(dataDogDataAsJson.getBytes(Charsets.UTF_8));
    }

    private static List<String> createTags(final PeriodicData periodicData, final AggregatedData datum) {
        final List<String> tags = Lists.newArrayList();
        // TODO(vkoskela): The publication of cluster vs host metrics needs to be formalized. [AINT-678]
        if (periodicData.getDimensions().get("host").matches("^.*-cluster\\.[^\\.]+$")) {
            tags.add("scope:" + "cluster");
        } else {
            tags.add("scope:" + "host");
            tags.add("host:" + periodicData.getDimensions().get("host"));
        }
        tags.add("service:" + datum.getFQDSN().getService());
        tags.add("cluster:" + datum.getFQDSN().getCluster());
        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Request createRequest(final AsyncHttpClient client, final byte[] serializedData) {
        return new RequestBuilder()
                .setUrl(getUri().toString())
                .setBody(serializedData)
                .setMethod(HttpMethods.POST.value())
                .setHeader("Content-Type", "application/json")
                .addQueryParam("api_key", _apiKey)
                .build();
    }

    private DataDogSink(final Builder builder) {
        super(builder);
        _apiKey = builder._apiKey;
    }

    private final String _apiKey;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDogSink.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private static final class Datum {

        private Datum(
                final String metric,
                final long secondsSinceEpoch,
                final float value,
                final String host,
                final List<String> tags) {
            _metric = metric;
            _host = host;
            _tags = tags;
            _points = Collections.singletonList(Arrays.asList(secondsSinceEpoch, value));
        }

        public String getMetric() {
            return _metric;
        }

        public String getHost() {
            return _host;
        }

        public List<String> getTags() {
            return _tags;
        }

        public List<Object> getPoints() {
            return _points;
        }

        private final String _metric;
        private final String _host;
        private final List<String> _tags;
        private final List<Object> _points;
    }

    /**
     * Implementation of builder pattern for <code>DataDogSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends HttpPostSink.Builder<Builder, DataDogSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DataDogSink.class);
        }

        /**
         * The API key. Required. Cannot be null or empty.
         *
         * @param value API key.
         * @return This <code>Builder</code> instance.
         */
        public Builder setApiKey(final String value) {
            _apiKey = value;
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
        @NotEmpty
        private String _apiKey;
    }
}
