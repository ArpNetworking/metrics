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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Publishes to a ReMet endpoint. This class is thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ReMetSink extends HttpPostSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> serialize(final List<AggregatedData> data) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (final AggregatedData datum : data) {
            // TODO(vkoskela): Refactor into JSON serializer [MAI-88]
            // Question: We should consider carefully how to separate sinks and
            // data formats.
            stringBuilder.append("{\"value\":\"").append(datum.getValue())
                    .append("\",\"metric\":\"").append(datum.getMetric())
                    .append("\",\"service\":\"").append(datum.getService())
                    .append("\",\"host\":\"").append(datum.getHost())
                    .append("\",\"period\":\"").append(datum.getPeriod())
                    .append("\",\"periodStart\":\"").append(datum.getPeriodStart())
                    .append("\",\"statistic\":\"").append(datum.getStatistic().getName())
                    .append("\"},");
        }
        stringBuilder.setCharAt(stringBuilder.length() - 1, ']');
        return Collections.singletonList(stringBuilder.toString());
    }

    private ReMetSink(final Builder builder) {
        super(builder);
    }

    /**
     * Implementation of builder pattern for <code>ReMetSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends HttpPostSink.Builder<Builder> {

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
    }
}
