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
import com.google.common.base.Objects;

import net.sf.oval.constraint.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A publisher that wraps and buffers another. This class is not thread safe.
 * 
 * TODO(vkoskela): This class should be thread safe/optimized [MAI-99]
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class BufferedSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final List<AggregatedData> data) {
        LOGGER.debug(getName() + ": Buffering aggregated data; size=" + data.size());

        for (final AggregatedData datum : data) {
            if (_bufferedData.size() >= _bufferSize) {
                flush();
            }
            _bufferedData.add(datum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (!_bufferedData.isEmpty()) {
            flush();
        }
        LOGGER.info(getName() + ": Closing sink; bufferFlushes=" + _bufferFlushes + " wrappedSink=" + _sink);
        _sink.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("Sink", _sink)
                .add("BufferSize", _bufferSize)
                .add("BufferFlushes", _bufferFlushes)
                .toString();
    }

    private void flush() {
        LOGGER.debug(getName() + ": Flushing aggregated data buffer; size=" + _bufferedData.size());

        _sink.recordAggregateData(_bufferedData);
        _bufferedData.clear();
        _bufferFlushes.incrementAndGet();
    }

    private BufferedSink(final Builder builder) {
        super(builder);
        _bufferSize = builder._bufferSize.intValue();
        _sink = builder._sink;
        _bufferedData = new ArrayList<AggregatedData>(_bufferSize);
    }

    private final Sink _sink;
    private final int _bufferSize;
    private final List<AggregatedData> _bufferedData;
    private final AtomicLong _bufferFlushes = new AtomicLong(0);

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSink.class);

    /**
     * Implementation of builder pattern for <code>BufferedSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(BufferedSink.class);
        }

        /**
         * The maximum buffer size. Defaults to 15. Cannot be null.
         * 
         * @param value The maximum buffer size.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setBufferSize(final Integer value) {
            _bufferSize = value;
            return this;
        }

        /**
         * The aggregated data sink to buffer. Cannot be null.
         * 
         * @param value The aggregated data sink to buffer.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
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
        private Integer _bufferSize = Integer.valueOf(15);
        @NotNull
        private Sink _sink;
    }
}
