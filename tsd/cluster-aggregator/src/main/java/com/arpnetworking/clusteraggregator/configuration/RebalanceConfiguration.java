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
package com.arpnetworking.clusteraggregator.configuration;

import com.arpnetworking.utility.OvalBuilder;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;

/**
 * Represents the shard rebalancing configuration.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class RebalanceConfiguration {
    public int getMaxParallel() {
        return _maxParallel;
    }

    public int getThreshold() {
        return _threshold;
    }

    private RebalanceConfiguration(final Builder builder) {
        _maxParallel = builder._maxParallel;
        _threshold = builder._threshold;
    }

    private final int _maxParallel;
    private final int _threshold;

    /**
     * Implementation of builder pattern for {@link RebalanceConfiguration}.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<RebalanceConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(RebalanceConfiguration.class);
        }

        /**
         * Maximum parallel shards to rebalance.
         * Required. Cannot be null. Must be greater than 0.
         *
         * @param value Maximum parallel shards to rebalance.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaxParallel(final Integer value) {
            _maxParallel = value;
            return this;
        }

        /**
         * Minimum difference in shards between hosts to trigger a rebalance.
         * Required. Cannot be null. Must be greater than 1.
         *
         * @param value Maximum parallel shards to rebalance.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setThreshold(final Integer value) {
            _threshold = value;
            return this;
        }

        @NotNull
        @Min(1)
        private Integer _maxParallel;
        @NotNull
        @Min(2)
        private Integer _threshold;
    }
}
