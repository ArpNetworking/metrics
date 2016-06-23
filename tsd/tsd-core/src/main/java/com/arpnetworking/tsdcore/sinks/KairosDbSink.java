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

import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import org.joda.time.format.ISOPeriodFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Publishes to a KairosDbSink endpoint. This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class KairosDbSink extends HttpPostSink {

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
                .put("maxRequestSize", _maxRequestSize)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<byte[]> serialize(final PeriodicData periodicData) {
        // Initialize serialization structures
        final List<byte[]> completeChunks = Lists.newArrayList();
        final ByteBuffer currentChunk = ByteBuffer.allocate(_maxRequestSize);
        final ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();

        // Extract and transform shared data
        final long timestamp = periodicData.getStart().plus(periodicData.getPeriod()).getMillis();
        final String serializedPeriod = periodicData.getPeriod().toString(ISOPeriodFormat.standard());
        final Optional<String> host = Optional.ofNullable(periodicData.getDimensions().get("host"));
        final Optional<String> scope = Optional.ofNullable(periodicData.getDimensions().get("scope"));
        final Optional<String> domain = Optional.ofNullable(periodicData.getDimensions().get("domain"));
        final Serializer serializer = new Serializer(timestamp, serializedPeriod, host, scope, domain);

        // Initialize the chunk buffer
        currentChunk.put(HEADER);

        // Add aggregated data
        for (final AggregatedData datum : periodicData.getData()) {
            if (!datum.isSpecified()) {
                LOGGER.trace()
                        .setMessage("Skipping unspecified datum")
                        .addData("datum", datum)
                        .log();
                continue;
            }

            serializer.serializeDatum(completeChunks, currentChunk, chunkStream, datum);
        }

        // Add conditions
        for (final Condition condition : periodicData.getConditions()) {
            serializer.serializeCondition(completeChunks, currentChunk, chunkStream, condition);
        }

        // Add the current chunk (if any) to the completed chunks
        if (currentChunk.position() > HEADER_BYTE_LENGTH) {
            currentChunk.put(currentChunk.position() - 1, FOOTER);
            completeChunks.add(Arrays.copyOf(currentChunk.array(), currentChunk.position()));
        }

        return completeChunks;
    }

    private void addChunk(
            final ByteArrayOutputStream chunkStream,
            final ByteBuffer currentChunk,
            final Collection<byte[]> completedChunks) {
        final byte[] nextChunk = chunkStream.toByteArray();
        final int nextChunkSize = nextChunk.length;
        if (currentChunk.position() + nextChunkSize > _maxRequestSize) {
            if (currentChunk.position() > HEADER_BYTE_LENGTH) {
                // TODO(vkoskela): Add chunk size metric. [MAI-?]

                // Copy the relevant part of the buffer
                currentChunk.put(currentChunk.position() - 1, FOOTER);
                completedChunks.add(Arrays.copyOf(currentChunk.array(), currentChunk.position()));

                // Truncate all but the beginning '[' to prepare the next entries
                currentChunk.clear();
                currentChunk.put(HEADER);
            } else {
                CHUNK_TOO_BIG_LOGGER.warn()
                        .setMessage("First chunk too big")
                        .addData("sink", getName())
                        .addData("bufferLength", currentChunk.position())
                        .addData("nextChunkSize", nextChunkSize)
                        .addData("maxRequestSIze", _maxRequestSize)
                        .log();
            }
        }

        currentChunk.put(nextChunk);
        currentChunk.put(SEPARATOR);
        chunkStream.reset();
    }

    private KairosDbSink(final Builder builder) {
        super(builder);
        _maxRequestSize = builder._maxRequestSize;
    }

    private final int _maxRequestSize;

    private static final byte HEADER = '[';
    private static final byte FOOTER = ']';
    private static final byte SEPARATOR = ',';
    private static final int HEADER_BYTE_LENGTH = 1;
    // TODO(vkoskela): Switch to ImmutableObjectMapper. [https://github.com/ArpNetworking/commons/issues/7]
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(KairosDbSink.class);
    private static final Logger SERIALIZATION_FAILURE_LOGGER = LoggerFactory.getRateLimitLogger(KairosDbSink.class, Duration.ofSeconds(30));
    private static final Logger CHUNK_TOO_BIG_LOGGER = LoggerFactory.getRateLimitLogger(KairosDbSink.class, Duration.ofSeconds(30));

    private class Serializer {

        Serializer(
                final long timestamp,
                final String serializedPeriod,
                final Optional<String> host,
                final Optional<String> scope,
                final Optional<String> domain) {
            _timestamp = timestamp;
            _serializedPeriod = serializedPeriod;
            _host = host;
            _scope = scope;
            _domain = domain;
        }

        public void serializeDatum(
                final List<byte[]> completeChunks,
                final ByteBuffer currentChunk,
                final ByteArrayOutputStream chunkStream,
                final AggregatedData datum) {
            final String name = _serializedPeriod
                    + "/" + datum.getFQDSN().getMetric()
                    + "/" + datum.getFQDSN().getStatistic().getName();
            try {
                final JsonGenerator chunkGenerator = OBJECT_MAPPER.getFactory().createGenerator(chunkStream, JsonEncoding.UTF8);

                chunkGenerator.writeStartObject();
                chunkGenerator.writeStringField("name", name);
                chunkGenerator.writeNumberField("timestamp", _timestamp);
                chunkGenerator.writeNumberField("value", datum.getValue().getValue());
                chunkGenerator.writeObjectFieldStart("tags");
                if (_host.isPresent()) {
                    chunkGenerator.writeStringField("host", _host.get());
                }
                if (_scope.isPresent()) {
                    chunkGenerator.writeStringField("scope", _scope.get());
                }
                if (_domain.isPresent()) {
                    chunkGenerator.writeStringField("domain", _domain.get());
                }
                chunkGenerator.writeStringField("service", datum.getFQDSN().getService());
                chunkGenerator.writeStringField("cluster", datum.getFQDSN().getCluster());
                chunkGenerator.writeEndObject();
                chunkGenerator.writeEndObject();

                chunkGenerator.close();

                addChunk(chunkStream, currentChunk, completeChunks);
            } catch (final IOException e) {
                SERIALIZATION_FAILURE_LOGGER.error()
                        .setMessage("Serialization failure")
                        .addData("datum", datum)
                        .setThrowable(e)
                        .log();
            }
        }

        public void serializeCondition(
                final List<byte[]> completeChunks,
                final ByteBuffer currentChunk,
                final ByteArrayOutputStream chunkStream,
                final Condition condition) {
            final String conditionName = _serializedPeriod
                    + "/" + condition.getFQDSN().getMetric()
                    + "/" + condition.getFQDSN().getStatistic().getName()
                    + "/" + condition.getName();
            final String conditionStatusName = conditionName
                    + "/status";
            try {
                // Value for condition threshold
                serializeConditionThreshold(completeChunks, currentChunk, chunkStream, condition, conditionName);

                if (condition.isTriggered().isPresent()) {
                    // Value for condition trigger (or status)
                    serializeConditionStatus(completeChunks, currentChunk, chunkStream, condition, conditionStatusName);

                }
            } catch (final IOException e) {
                SERIALIZATION_FAILURE_LOGGER.error()
                        .setMessage("Serialization failure")
                        .addData("condition", condition)
                        .setThrowable(e)
                        .log();
            }
        }

        private void serializeConditionStatus(
                final List<byte[]> completeChunks,
                final ByteBuffer currentChunk,
                final ByteArrayOutputStream chunkStream,
                final Condition condition,
                final String conditionStatusName)
                throws IOException {
            // 0 = Not triggered
            // 1 = Triggered
            final JsonGenerator chunkGenerator = OBJECT_MAPPER.getFactory().createGenerator(chunkStream, JsonEncoding.UTF8);

            chunkGenerator.writeStartObject();
            chunkGenerator.writeStringField("name", conditionStatusName);
            chunkGenerator.writeNumberField("timestamp", _timestamp);
            chunkGenerator.writeNumberField("value", condition.isTriggered().get() ? 1 : 0);
            chunkGenerator.writeObjectFieldStart("tags");
            if (_host.isPresent()) {
                chunkGenerator.writeStringField("host", _host.get());
            }
            if (_scope.isPresent()) {
                chunkGenerator.writeStringField("scope", _scope.get());
            }
            if (_domain.isPresent()) {
                chunkGenerator.writeStringField("domain", _domain.get());
            }
            chunkGenerator.writeStringField("service", condition.getFQDSN().getService());
            chunkGenerator.writeStringField("cluster", condition.getFQDSN().getCluster());
            chunkGenerator.writeEndObject();
            chunkGenerator.writeEndObject();

            chunkGenerator.close();

            addChunk(chunkStream, currentChunk, completeChunks);
        }

        private void serializeConditionThreshold(
                final List<byte[]> completeChunks,
                final ByteBuffer currentChunk,
                final ByteArrayOutputStream chunkStream,
                final Condition condition,
                final String conditionName)
                throws IOException {
            final JsonGenerator chunkGenerator = OBJECT_MAPPER.getFactory().createGenerator(chunkStream, JsonEncoding.UTF8);

            chunkGenerator.writeStartObject();
            chunkGenerator.writeStringField("name", conditionName);
            chunkGenerator.writeNumberField("timestamp", _timestamp);
            chunkGenerator.writeNumberField("value", condition.getThreshold().getValue());
            chunkGenerator.writeObjectFieldStart("tags");
            if (_host.isPresent()) {
                chunkGenerator.writeStringField("host", _host.get());
            }
            if (_scope.isPresent()) {
                chunkGenerator.writeStringField("scope", _scope.get());
            }
            if (_domain.isPresent()) {
                chunkGenerator.writeStringField("domain", _domain.get());
            }
            chunkGenerator.writeStringField("service", condition.getFQDSN().getService());
            chunkGenerator.writeStringField("cluster", condition.getFQDSN().getCluster());
            chunkGenerator.writeEndObject();
            chunkGenerator.writeEndObject();

            chunkGenerator.close();

            addChunk(chunkStream, currentChunk, completeChunks);
        }

        private final long _timestamp;
        private final String _serializedPeriod;
        private final Optional<String> _host;
        private final Optional<String> _scope;
        private final Optional<String> _domain;
    }

    /**
     * Implementation of builder pattern for <code>KairosDbSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends HttpPostSink.Builder<Builder, KairosDbSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(KairosDbSink.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Sets the maximum size of the request to publish.
         * Optional. Defaults to 100KiB.
         *
         * @param value the maximum request size.
         * @return This instance of {@link Builder}.
         */
        public Builder setMaxRequestSize(final Integer value) {
            _maxRequestSize = value;
            return this;
        }

        @NotNull
        @Min(value = 0)
        private Integer _maxRequestSize = 100 * 1024;
    }
}
