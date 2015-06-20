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

import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A publisher that writes to <code>System.out</code>. This class is thread
 * safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ConsoleSink extends BaseSink {

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
                .log();

        if (!data.isEmpty()) {
            final StringBuilder builder = new StringBuilder();
            for (final AggregatedData datum : data) {
                final String unit;
                if (datum.getValue().getUnit().isPresent()) {
                    unit = datum.getValue().getUnit().get().toString();
                } else {
                    unit = "";
                }
                builder.append(datum.getHost())
                        .append("::")
                        .append(datum.getFQDSN().getService())
                        .append("::")
                        .append(datum.getFQDSN().getMetric())
                        .append(" ")
                        .append(datum.getPeriodStart())
                        .append(" [")
                        .append(datum.getPeriod())
                        .append("] ")
                        .append(datum.getFQDSN().getStatistic().getName())
                        .append(": ")
                        .append(String.format("%f", datum.getValue().getValue()))
                        .append(" ")
                        .append(unit);

                _printStream.println(builder.toString());
                builder.setLength(0);
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
                .addData("recordsWritten", _recordsWritten)
                .log();
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
                "RecordsWritten", _recordsWritten);
    }

    /* package private */ ConsoleSink(final Builder builder, final PrintStream printStream) {
        super(builder);
        _printStream = printStream;
    }

    private ConsoleSink(final Builder builder) {
        super(builder);
        _printStream = System.out;
    }

    private final PrintStream _printStream;
    private final AtomicLong _recordsWritten = new AtomicLong(0);

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSink.class);

    /**
     * Implementation of builder pattern for <code>ConsoleSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, ConsoleSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(ConsoleSink.class);
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
