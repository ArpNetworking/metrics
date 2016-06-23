/**
 * Copyright 2014 Groupon.com
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

package com.arpnetworking.clusteraggregator.models;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.google.common.base.MoreObjects;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;

import java.io.Serializable;

/**
 * Message class containing the information retrieved from the {@link com.arpnetworking.clusteraggregator.aggregation.Bookkeeper}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class BookkeeperData implements Serializable {
    public long getClusters() {
        return _clusters;
    }

    public long getMetrics() {
        return _metrics;
    }

    public long getServices() {
        return _services;
    }

    public long getStatistics() {
        return _statistics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Clusters", _clusters)
                .add("Metrics", _metrics)
                .add("Services", _services)
                .add("Statistics", _statistics)
                .toString();
    }

    private BookkeeperData(final Builder builder) {
        _clusters = builder._clusters;
        _metrics = builder._metrics;
        _services = builder._services;
        _statistics = builder._statistics;
    }

    private final long _clusters;
    private final long _metrics;
    private final long _services;
    private final long _statistics;
    private static final long serialVersionUID = 1L;

    /**
     * Builder for a {@link BookkeeperData}.
     */
    public static final class Builder extends OvalBuilder<BookkeeperData> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(BookkeeperData.class);
        }

        /**
         * Set the statistics. Required. Cannot be null.
         *
         * @param value The total statistics.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStatistics(final Long value) {
            _statistics = value;
            return this;
        }

        /**
         * Set the metrics. Required. Cannot be null.
         *
         * @param value The total metrics.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetrics(final Long value) {
            _metrics = value;
            return this;
        }

        /**
         * Set the services. Required. Cannot be null.
         *
         * @param value The total services.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setServices(final Long value) {
            _services = value;
            return this;
        }

        /**
         * Set the clusters. Required. Cannot be null.
         *
         * @param value The total clusters.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setClusters(final Long value) {
            _clusters = value;
            return this;
        }

        @NotNull
        @Min(0)
        private Long _statistics;
        @NotNull
        @Min(0)
        private Long _metrics;
        @NotNull
        @Min(0)
        private Long _services;
        @NotNull
        @Min(0)
        private Long _clusters;
    }
}
