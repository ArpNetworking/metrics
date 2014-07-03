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

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * Publisher to send data to a Carbon server.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class CarbonSink extends VertxSink {

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
    protected void onConnect(final NetSocket socket) {
        // Nothing to be done.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Buffer serialize(final AggregatedData datum) {
        // TODO(vkoskela): Use AggregatedData host [MAI-109]
        return new Buffer(
                String.format(
                        "%s.%s.%s.%s.%s.%s %f %d%n",
                        _cluster,
                        _host,
                        datum.getService(),
                        datum.getMetric(),
                        datum.getPeriod().toString(),
                        datum.getStatistic().getName(),
                        Double.valueOf(datum.getValue()),
                        Long.valueOf(datum.getPeriodStart().toInstant().getMillis() / 1000)));
    }

    private CarbonSink(final Builder builder) {
        super(builder);
        _host = builder._host;
        _cluster = builder._cluster;
    }

    private final String _host;
    private final String _cluster;

    /**
     * Implementation of builder pattern for <code>CarbonSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends VertxSink.Builder<Builder> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(CarbonSink.class);
            setServerPort(Integer.valueOf(2003));
        }

        /**
         * The cluster identifier. Cannot be null or empty.
         * 
         * @param value The cluster identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * The host identified. Cannot be null or empty.
         * 
         * @param value The host identifier.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
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
    }
}
