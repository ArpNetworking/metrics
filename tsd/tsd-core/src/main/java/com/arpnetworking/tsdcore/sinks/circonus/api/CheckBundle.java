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

import com.arpnetworking.logback.annotations.Loggable;
import com.arpnetworking.utility.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Represents a check bundle in Circonus.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @see <a href="https://login.circonus.com/resources/api/calls/check_bundle">API Docs</a>
 */
@Loggable
public final class CheckBundle {
    @JsonAnyGetter
    public Map<String, Object> getOther() {
        return _other;
    }

    @JsonIgnore
    public String getCid() {
        return _cid;
    }

    public List<Map<String, String>> getMetrics() {
        return _metrics;
    }

    public Map<String, String> getConfig() {
        return _config;
    }

    public String getStatus() {
        return _status;
    }

    private CheckBundle(final Builder builder) {
        _brokers = Lists.newArrayList(builder._brokers);
        _tags = Lists.newArrayList(builder._tags);
        _target = builder._target;
        _displayName = builder._displayName;
        _status = builder._status;

        if (builder._config != null) {
            _config = builder._config;
        } else {
            _config = Maps.newHashMap();
            _config.put("asynch_metrics", "true");
            _config.put("secret", "secret");
        }

        if (builder._period != null) {
            _period = builder._period;
        } else {
            _period = 60;
        }

        _cid = builder._cid;

        _metricLimit = builder._metricLimit;

        if (builder._metrics != null) {
            _metrics = builder._metrics;
        } else {
            final Map<String, String> dummyMetric = Maps.newHashMap();
            dummyMetric.put("name", "dummy");
            dummyMetric.put("status", "active");
            dummyMetric.put("type", "numeric");

            _metrics = Lists.newArrayList();
            _metrics.add(dummyMetric);
        }
        _other = builder._other;
    }

    @JsonProperty("target")
    private final String _target;
    @JsonProperty("tags")
    private final List<String> _tags;
    @JsonProperty("config")
    private final Map<String, String> _config;
    @JsonProperty("metric_limit")
    private final int _metricLimit;
    @JsonProperty("display_name")
    private final String _displayName;
    @JsonProperty("period")
    private final int _period;
    @JsonProperty("metrics")
    private final List<Map<String, String>> _metrics;
    @JsonProperty("type")
    private final String _type = "httptrap";
    @JsonProperty("brokers")
    private final List<String> _brokers;
    @JsonIgnore
    private final Map<String, Object> _other;
    @JsonProperty("_cid")
    private final String _cid;
    @JsonProperty("status")
    private final String _status;

    /**
     * Implementation of the builder pattern for {@link CheckBundle}.
     */
    public static class Builder extends OvalBuilder<CheckBundle> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(CheckBundle.class);
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
        @JsonProperty("tags")
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
        @JsonProperty("target")
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
        @JsonProperty("display_name")
        public Builder setDisplayName(final String value) {
            _displayName = value;
            return this;
        }

        /**
         * Sets the metric limit.
         *
         * @param value The metric limit.
         * @return This builder.
         */
        @JsonProperty("metric_limit")
        public Builder setMetricLimit(final Integer value) {
            _metricLimit = value;
            return this;
        }

        /**
         * Sets the period.
         *
         * @param value The period in seconds.
         * @return This builder.
         */
        @JsonProperty("period")
        public Builder setPeriod(final Integer value) {
            _period = value;
            return this;
        }

        /**
         * Sets the config block.
         *
         * @param value Config block for the check.
         * @return This builder.
         */
        @JsonProperty("config")
        public Builder setConfig(final Map<String, String> value) {
            _config = value;
            return this;
        }

        /**
         * Sets the metrics block.
         *
         * @param value Metrics block for the check.
         * @return This builder.
         */
        @JsonProperty("metrics")
        public Builder setMetrics(final List<Map<String, String>> value) {
            _metrics = value;
            return this;
        }

        /**
         * Sets the CID.
         *
         * @param value The CID.
         * @return This builder.
         */
        @JsonProperty("_cid")
        public Builder setCid(final String value) {
            _cid = value;
            return this;
        }

        /**
         * Sets the status.
         *
         * @param value The status.
         * @return This builder.
         */
        @JsonProperty("status")
        public Builder setStatus(final String value) {
            _status = value;
            return this;
        }

        /**
         * Setter for other properties that get serialized to/from json.
         *
         * @param value The values
         * @return This builder.
         */
        @JsonAnySetter
        public Builder setOther(final Map<String, Object> value) {
            _other = value;
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
        private Integer _metricLimit = -1;
        private Map<String, String> _config;
        private Integer _period;
        private List<Map<String, String>> _metrics;
        private Map<String, Object> _other = Maps.newHashMap();
        private String _cid;
        private String _status;
    }
}
