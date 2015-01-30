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

package com.arpnetworking.tsdcore.sinks.circonus.api;

import com.arpnetworking.utility.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Represents a request to create a check bundle.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @see <a href="https://login.circonus.com/resources/api/calls/check_bundle">API Docs</a>
 */
public final class CheckBundleRequest {
    private CheckBundleRequest(final Builder builder) {
        _brokers = Lists.newArrayList(builder._brokers);
        _tags = Lists.newArrayList(builder._tags);
        _target = builder._target;
        _displayName = builder._displayName;

        _config = Maps.newHashMap();
        _config.put("asynch_metrics", "false");
        _config.put("secret", "secret");

        final Map<String, String> dummyMetric = Maps.newHashMap();
        dummyMetric.put("name", "dummy");
        dummyMetric.put("status", "active");
        dummyMetric.put("type", "numeric");

        _metrics = Lists.newArrayList();
        _metrics.add(dummyMetric);
    }

    @JsonProperty("target")
    private final String _target;
    @JsonProperty("tags")
    private final List<String> _tags;
    @JsonProperty("config")
    private final Map<String, String> _config;
    @JsonProperty("metric_limit")
    private final int _metricLimit = -1;
    @JsonProperty("display_name")
    private final String _displayName;
    @JsonProperty("period")
    private final int _period = 60;
    @JsonProperty("metrics")
    private final List<Map<String, String>> _metrics;
    @JsonProperty("type")
    private final String _type = "httptrap";
    @JsonProperty("brokers")
    private final List<String> _brokers;

    /**
     * Implementation of the builder pattern for {@link com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundleRequest}.
     */
    public static class Builder extends OvalBuilder<CheckBundleRequest> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(CheckBundleRequest.class);
        }

        /**
         * Sets the {@link java.util.List} of brokers.
         *
         * @param value List of brokers to write to register the check on.
         * @return This builder.
         */
        public Builder setBrokers(final List<String> value) {
            _brokers = value;
            return this;
        }

        /**
         * Adds a broker to the list of brokers.
         *
         * @param value Broker to add.
         * @return This builder.
         */
        public Builder addBroker(final String value) {
            if (_brokers == null) {
                _brokers = Lists.newArrayList();
            }
            _brokers.add(value);
            return this;
        }

        /**
         * Sets the {@link java.util.List} of tags.
         *
         * @param value List of tags to associate with the check.
         * @return This builder.
         */
        public Builder setTags(final List<String> value) {
            _tags = value;
            return this;
        }

        /**
         * Adds a tag to the list of tags.
         *
         * @param value Tag to add.
         * @return This builder.
         */
        public Builder addTag(final String value) {
            if (_tags == null) {
                _tags = Lists.newArrayList();
            }
            _tags.add(value);
            return this;
        }

        /**
         * Sets the name of the target.
         *
         * @param value Name of the target.
         * @return This builder.
         */
        public Builder setTarget(final String value) {
            _target = value;
            return this;
        }

        /**
         * Sets the display name.
         *
         * @param value Display name for the check.
         * @return This builder.
         */
        public Builder setDisplayName(final String value) {
            _displayName = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private List<String> _brokers;
        @NotNull
        private List<String> _tags;
        @NotNull
        @NotEmpty
        private String _target;
        @NotNull
        @NotEmpty
        private String _displayName;
    }
}
