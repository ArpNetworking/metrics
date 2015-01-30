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
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Circonus api for a broker list response.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @see <a href="https://login.circonus.com/resources/api/calls/broker">API Docs</a>
 */
public class BrokerListResponse {
    /**
     * Public constructor.
     *
     * @param brokers List of brokers.
     */
    public BrokerListResponse(final List<Broker> brokers) {
        _brokers = Lists.newArrayList(brokers);
    }

    public List<Broker> getBrokers() {
        return Collections.unmodifiableList(_brokers);
    }

    private final List<Broker> _brokers;

    /**
     * Represents a broker object in the Circonus API.
     */
    public static final class Broker {
        private Broker(final Builder builder) {
            _cid = builder._cid;
            _name = builder._name;
            _tags = Lists.newArrayList(builder._tags);
            _type = builder._type;
            _details = Lists.newArrayList(builder._details);
        }

        public List<String> getTags() {
            return _tags;
        }

        public String getName() {
            return _name;
        }

        public String getType() {
            return _type;
        }

        public String getCid() {
            return _cid;
        }

        public List<BrokerDetails> getDetails() {
            return _details;
        }

        private final List<String> _tags;
        private final String _name;
        private final String _type;
        private final String _cid;
        private final List<BrokerDetails> _details;

        /**
         * Builder for a {@link com.arpnetworking.tsdcore.sinks.circonus.api.BrokerListResponse.Broker}.
         */
        public static class Builder extends OvalBuilder<Broker> {
            /**
             * Public constructor.
             */
            public Builder() {
                super(Broker.class);
            }

            /**
             * Sets the tags.
             *
             * @param value The tags.
             * @return This Builder.
             */
            public Builder setTags(final List<String> value) {
                _tags = Lists.newArrayList(value);
                return this;
            }

            /**
             * Adds a tag.
             *
             * @param value The tag.
             * @return This Builder.
             */
            public Builder addTag(final String value) {
                if (_tags == null) {
                    _tags = Lists.newArrayList();
                }
                _tags.add(value);
                return this;
            }

            /**
             * Sets the name.
             *
             * @param value The name of the broker.
             * @return This Builder.
             */
            public Builder setName(final String value) {
                _name = value;
                return this;
            }

            /**
             * Sets the cid.
             *
             * @param value The cid of the broker.
             * @return This Builder.
             */
            public Builder setCid(final String value) {
                _cid = value;
                return this;
            }

            /**
             * Sets the type.
             *
             * @param value The type of the broker.
             * @return This Builder.
             */
            public Builder setType(final String value) {
                _type = value;
                return this;
            }

            /**
             * Sets the broker details.
             *
             * @param value The broker details.
             * @return This Builder.
             */
            public Builder setBrokerDetails(final List<BrokerDetails> value) {
                _details = Lists.newArrayList(value);
                return this;
            }

            /**
             * Adds a broker details entry.
             *
             * @param value The broker details entry.
             * @return This Builder.
             */
            public Builder addBrokerDetails(final BrokerDetails value) {
                if (_details == null) {
                    _details = Lists.newArrayList();
                }
                _details.add(value);
                return this;
            }

            @NotNull
            @JsonProperty("_tags")
            private List<String> _tags;
            @NotNull
            @NotEmpty
            @JsonProperty("_name")
            private String _name;
            @NotNull
            @NotEmpty
            @JsonProperty("_type")
            private String _type;
            @NotNull
            @NotEmpty
            @JsonProperty("_cid")
            private String _cid;
            @NotNull
            @JsonProperty("_details")
            private List<BrokerDetails> _details;
        }
    }

    /**
     * Represents the details block of a broker object.
     */
    public static final class BrokerDetails {
        private BrokerDetails(final Builder builder) {
            _status = builder._status;
            _cn = builder._cn;
            _version = builder._version;
            _skew = builder._skew;
            _modules = Lists.newArrayList(builder._modules);
            _ipAddress = builder._ipAddress;
        }

        public String getStatus() {
            return _status;
        }

        public String getCn() {
            return _cn;
        }

        public String getVersion() {
            return _version;
        }

        public String getSkew() {
            return _skew;
        }

        public List<String> getModules() {
            return Collections.unmodifiableList(_modules);
        }

        public String getIpAddress() {
            return _ipAddress;
        }

        private final String _status;
        private final String _cn;
        private final String _version;
        private final String _skew;
        private final List<String> _modules;
        private final String _ipAddress;

        /**
         * Builder for a {@link com.arpnetworking.tsdcore.sinks.circonus.api.BrokerListResponse.BrokerDetails}.
         */
        public static class Builder extends OvalBuilder<BrokerDetails> {
            /**
             * Public constructor.
             */
            public Builder() {
                super(BrokerDetails.class);
            }

            /**
             * Sets the status.
             *
             * @param value The status.
             * @return This Builder.
             */
            public Builder setStatus(final String value) {
                _status = value;
                return this;
            }

            /**
             * Sets the version.
             *
             * @param value The version.
             * @return This Builder.
             */
            public Builder setVersion(final String value) {
                _version = value;
                return this;
            }

            /**
             * Sets the skew.
             *
             * @param value The skew.
             * @return This Builder.
             */
            public Builder setSkew(final String value) {
                _skew = value;
                return this;
            }

            /**
             * Sets the IP address.
             *
             * @param value The IP address.
             * @return This Builder.
             */
            public Builder setIpAddress(final String value) {
                _ipAddress = value;
                return this;
            }

            /**
             * Sets the modules.
             *
             * @param value The modules.
             * @return This Builder.
             */
            public Builder setModules(final List<String> value) {
                _modules = Lists.newArrayList(value);
                return this;
            }

            /**
             * Adds a module to the list.
             *
             * @param value The module to add.
             * @return This Builder.
             */
            public Builder addModule(final String value) {
                if (_modules == null) {
                    _modules = Lists.newArrayList();
                }
                _modules.add(value);
                return this;
            }

            @JsonProperty("status")
            private String _status;
            @JsonProperty("cn")
            private String _cn;
            @JsonProperty("version")
            private String _version;
            @JsonProperty("skew")
            private String _skew;
            @JsonProperty("modules")
            private List<String> _modules;
            @JsonProperty("ipaddress")
            private String _ipAddress;
        }
    }
}
