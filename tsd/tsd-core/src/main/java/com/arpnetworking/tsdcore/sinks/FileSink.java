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
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publisher to write aggregated data to a file. This class is thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class FileSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", data.size())
                .addData("conditionsSize", conditions.size())
                .addData("fileName", _fileName)
                .log();

        if (!data.isEmpty()) {
            final StringBuilder stringBuilder = new StringBuilder();
            for (final AggregatedData datum : data) {
                // TODO(vkoskela): Refactor into JSON serializer [MAI-88]
                final String unitName;
                if (datum.getValue().getUnit().isPresent()) {
                    unitName = "\"" + datum.getValue().getUnit().get().name() + "\"";
                } else {
                    unitName = "null";
                }
                stringBuilder.append("{\"value\":\"").append(String.format("%f", datum.getValue().getValue()))
                        .append("\",\"unit\":").append(unitName)
                        .append(",\"metric\":\"").append(datum.getFQDSN().getMetric())
                        .append("\",\"service\":\"").append(datum.getFQDSN().getService())
                        .append("\",\"host\":\"").append(datum.getHost())
                        .append("\",\"period\":\"").append(datum.getPeriod())
                        .append("\",\"periodStart\":\"").append(datum.getPeriodStart())
                        .append("\",\"statistic\":\"").append(datum.getFQDSN().getStatistic().getName())
                        .append("\"}\n");
                try {
                    _writer.append(stringBuilder.toString());
                } catch (final IOException e) {
                    LOGGER.error()
                            .setMessage("Error writing output to file")
                            .addData("sink", getName())
                            .addData("fileName", _fileName)
                            .setThrowable(e)
                            .log();
                }
                stringBuilder.setLength(0);
                _recordsWritten.incrementAndGet();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        LOGGER.info()
                .setMessage("Closing sink")
                .addData("sink", getName())
                .addData("fileName", _fileName)
                .addData("recordsWritten", _recordsWritten)
                .log();

        try {
            // Calling close will flush the writer automatically
            _writer.close();
        } catch (final IOException e) {
            LOGGER.error()
                    .setMessage("Error closing output file")
                    .addData("sink", getName())
                    .addData("fileName", _fileName)
                    .setThrowable(e)
                    .log();
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "super", super.toLogValue(),
                "FileName", _fileName,
                "RecordsWritten", _recordsWritten);
    }

    private FileSink(final Builder builder) {
        super(builder);
        _fileName = builder._fileName;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(_fileName, true);
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
        _writer = new OutputStreamWriter(fileOutputStream, Charsets.UTF_8);
    }

    private final String _fileName;
    private final Writer _writer;
    private final AtomicLong _recordsWritten = new AtomicLong(0);

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSink.class);

    /**
     * Implementation of builder pattern for <code>FileSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, FileSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(FileSink.class);
        }

        /**
         * The file path and name to write the aggregated data to. Cannot be
         * null or empty.
         *
         * @param value The file path and name to write the aggregated data to.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setFileName(final String value) {
            _fileName = value;
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
        private String _fileName = "";
    }
}
