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

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.metrics.com.arpnetworking.steno.Logger;
import com.arpnetworking.metrics.com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Publishes to a ReMet endpoint. This class is thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ReMetSink extends HttpPostSink {

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
    protected Collection<String> serialize(final PeriodicData periodicData) {
        // TODO(vkoskela): Send conditions to ReMet [MAI-451]
        final StringBuilder buffer = new StringBuilder();
        buffer.append(HEADER);
        int bufferLength = HEADER_BYTE_LENGTH;
        int chunkCount = 0;

        final List<String> serializedData = Lists.newArrayList();

        for (final AggregatedData datum : periodicData.getData()) {
            if (!datum.isSpecified()) {
                continue;
            }
            // TODO(vkoskela): Refactor into JSON serializer [MAI-88]
            // Question: We should consider carefully how to separate sinks and
            // data formats.
            final StringBuilder nextChunkBuilder = new StringBuilder()
                    .append("{\"value\":\"").append(datum.getValue().getValue())
                    .append("\",\"metric\":\"").append(datum.getFQDSN().getMetric())
                    .append("\",\"service\":\"").append(datum.getFQDSN().getService())
                    .append("\",\"host\":\"").append(datum.getHost())
                    .append("\",\"period\":\"").append(datum.getPeriod())
                    .append("\",\"periodStart\":\"").append(datum.getPeriodStart())
                    .append("\",\"statistic\":\"").append(datum.getFQDSN().getStatistic().getName())
                    .append("\"},");
            final String nextChunk = nextChunkBuilder.toString();
            final int nextChunkSize = nextChunk.getBytes(Charsets.UTF_8).length;
            if (bufferLength + nextChunkSize > _maxRequestSize) {
                if (chunkCount > 0) {
                    // TODO(vkoskela): Add chunk size metric. [MAI-?]

                    // Close the string builder and add the string to the serialized list
                    buffer.setCharAt(buffer.length() - 1, ']');
                    serializedData.add(buffer.toString());

                    // Truncate all but the beginning '[' to prepare the next entries
                    buffer.setLength(HEADER_BYTE_LENGTH);
                    bufferLength = HEADER_BYTE_LENGTH;
                    chunkCount = 0;
                } else {
                    LOGGER.warn(
                            String.format(
                                    "First chunk too big; bufferLength=%d nextChunkSize=%d maxRequestSize=%d",
                                    bufferLength,
                                    nextChunkSize,
                                    _maxRequestSize));
                }
            }

            buffer.append(nextChunk);
            bufferLength += nextChunkSize;
            ++chunkCount;
        }
        if (bufferLength > 1) {
            buffer.setCharAt(buffer.length() - 1, ']');
            serializedData.add(buffer.toString());
        }
        return serializedData;
    }

    private ReMetSink(final Builder builder) {
        super(builder);
        _maxRequestSize = builder._maxRequestSize;
    }

    private final long _maxRequestSize;

    private static final String HEADER = "[";
    private static final int HEADER_BYTE_LENGTH = HEADER.getBytes(Charsets.UTF_8).length;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReMetSink.class);

    /**
     * Implementation of builder pattern for <code>ReMetSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends HttpPostSink.Builder<Builder, ReMetSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(ReMetSink.class);
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
        public Builder setMaxRequestSize(final Long value) {
            _maxRequestSize = value;
            return this;
        }

        @NotNull
        private Long _maxRequestSize = 100 * 1024L;
    }
}
