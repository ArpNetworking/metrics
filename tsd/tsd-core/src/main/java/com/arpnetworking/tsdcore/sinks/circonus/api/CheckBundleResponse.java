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
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Represents the response from a check bundle get or create.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @see <a href="https://login.circonus.com/resources/api/calls/check_bundle">API Docs</a>
 */
public final class CheckBundleResponse {
    public String getCid() {
        return _cid;
    }

    public String getDisplayName() {
        return _displayName;
    }

    public URI getUrl() {
        return _url;
    }

    private CheckBundleResponse(final Builder builder) {
        _cid = builder._cid;
        try {
            _url = new URI(builder._config.get("submission_url"));
        } catch (final URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        _displayName = builder._displayName;
    }

    private final String _cid;
    private final URI _url;
    private final String _displayName;

    /**
     * Implementation of the builder pattern for {@link com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundleResponse}.
     */
    public static class Builder extends OvalBuilder<CheckBundleResponse> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(CheckBundleResponse.class);
        }

        /**
         * Sets the display name.
         *
         * @param value The display name.
         * @return This builder.
         */
        @JsonProperty("display_name")
        public Builder setDisplayName(final String value) {
            _displayName = value;
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
         * Sets the config map.
         *
         * @param value The config map.
         * @return This builder.
         */
        @JsonProperty("config")
        public Builder setConfig(final Map<String, String> value) {
            _config = value;
            return this;
        }

        @NotNull
        private Map<String, String> _config;
        @NotNull
        @NotEmpty
        private String _cid;
        @NotNull
        @NotEmpty
        private String _displayName;
    }
}
