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
package com.arpnetworking.configuration.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * <code>JsonNode</code> based configuration sourced from a file.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class JsonNodePaginatedUrlSource extends BaseJsonNodeSource implements JsonNodeSource {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<JsonNode> getValue(final String... keys) {
        return getValue(getJsonNode(), keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Url", _url)
                .add("DataKey", _dataKeys)
                .add("NextPageKey", _nextPageKeys)
                .add("MergingSource", _mergingSource)
                .toString();
    }

    /* package private */ Optional<JsonNode> getJsonNode() {
        return _mergingSource.getValue(_dataKeys);
    }

    private JsonNodePaginatedUrlSource(final Builder builder) {
        super(builder);
        _url = builder._url;
        _dataKeys = builder._dataKeys.toArray(new String[builder._dataKeys.size()]);
        _nextPageKeys = builder._nextPageKeys.toArray(new String[builder._nextPageKeys.size()]);

        final JsonNodeMergingSource.Builder mergingSourceBuilder = new JsonNodeMergingSource.Builder();
        final String baseUrl = _url.getProtocol() + "://" + _url.getHost() + ":" + _url.getPort() + "/";
        URL currentUrl = _url;
        while (currentUrl != null) {
            LOGGER.debug(String.format("Creating JsonNodeUrlSource; url=%s", currentUrl));

            // Create a URL source for the page
            final JsonNodeUrlSource urlSource = new JsonNodeUrlSource.Builder()
                    .setUrl(currentUrl)
                    .build();
            mergingSourceBuilder.addSource(urlSource);

            // Extract the link for the next page
            final Optional<JsonNode> nextPageNode = urlSource.getValue(_nextPageKeys);
            if (nextPageNode.isPresent() && !nextPageNode.get().isNull()) {
                try {
                    currentUrl = new URL(baseUrl + nextPageNode.get().asText());
                } catch (final MalformedURLException e) {
                    throw Throwables.propagate(e);
                }
            } else {
                currentUrl = null;
            }
        }

        _mergingSource = mergingSourceBuilder.build();
    }

    private final URL _url;
    private final String[] _dataKeys;
    private final String[] _nextPageKeys;
    private final JsonNodeMergingSource _mergingSource;

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNodePaginatedUrlSource.class);

    /**
     * Builder for <code>JsonNodeUrlSource</code>.
     */
    public static final class Builder extends BaseJsonNodeSource.Builder<Builder, JsonNodePaginatedUrlSource> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(JsonNodePaginatedUrlSource.class);
        }

        /**
         * Set the source <code>URL</code>. Required. The full URL to
         * the first page's results. The protocol, host and port will
         * be used from this url to complete the path-only url found
         * at each next page key.
         *
         * @param value The source <code>URL</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUrl(final URL value) {
            _url = value;
            return this;
        }

        /**
         * Set the keys to the data. Required. Cannot be null
         * or empty. The value at the end of this key chain should
         * be an array.
         *
         * @param value The keys from the root to the data.
         * @return This <code>Builder</code> instance.
         */
        public Builder setDataKeys(final List<String> value) {
            _dataKeys = value;
            return this;
        }

        /**
         * Set the keys to the next page url. Required. Cannot
         * be null or empty. The value at this key should be a path-only
         * URL (e.g. without protocol, host or port) to the next
         * page's results.
         *
         * @param value The keys from the root to the next page url.
         * @return This <code>Builder</code> instance.
         */
        public Builder setNextPageKeys(final List<String> value) {
            _nextPageKeys = value;
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
        private URL _url;
        @NotEmpty
        @NotNull
        private List<String> _dataKeys;
        @NotEmpty
        @NotNull
        private List<String> _nextPageKeys;
    }
}
