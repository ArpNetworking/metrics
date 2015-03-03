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
import com.google.common.base.MoreObjects;
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
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
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
        return new Buffer(
                String.format(
                        "%s.%s.%s.%s.%s.%s %f %d%n",
                        datum.getFQDSN().getCluster(),
                        datum.getHost(),
                        datum.getFQDSN().getService(),
                        datum.getFQDSN().getMetric(),
                        datum.getPeriod().toString(),
                        datum.getFQDSN().getStatistic().getName(),
                        datum.getValue().getValue(),
                        datum.getPeriodStart().toInstant().getMillis() / 1000));
    }

    private CarbonSink(final Builder builder) {
        super(builder);
    }

    /**
     * Implementation of builder pattern for <code>CarbonSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends VertxSink.Builder<Builder, CarbonSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(CarbonSink.class);
            setServerPort(2003);
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
