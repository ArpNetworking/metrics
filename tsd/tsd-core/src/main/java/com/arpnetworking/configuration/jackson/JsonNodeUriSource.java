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

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotNull;

import java.io.IOException;
import java.net.URI;

/**
 * <code>JsonNode</code> based configuration sourced from a file.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class JsonNodeUriSource extends BaseJsonNodeSource implements JsonNodeSource {

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
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("super", super.toLogValue())
                .put("uri", _uri)
                .put("jsonNode", _jsonNode)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return toLogValue().toString();
    }

    /* package private */ Optional<JsonNode> getJsonNode() {
        return _jsonNode;
    }

    private JsonNodeUriSource(final Builder builder) {
        super(builder);
        _uri = builder._uri;

        JsonNode jsonNode = null;
        try {
            jsonNode = _objectMapper.readTree(_uri.toURL());
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
        _jsonNode = Optional.fromNullable(jsonNode);
    }

    private final URI _uri;
    private final Optional<JsonNode> _jsonNode;

    /**
     * Builder for <code>JsonNodeUriSource</code>.
     */
    public static final class Builder extends BaseJsonNodeSource.Builder<Builder, JsonNodeUriSource> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(JsonNodeUriSource.class);
        }

        /**
         * Set the source <code>URI</code>.
         *
         * @param value The source <code>URI</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUri(final URI value) {
            _uri = value;
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
        private URI _uri;
    }
}
