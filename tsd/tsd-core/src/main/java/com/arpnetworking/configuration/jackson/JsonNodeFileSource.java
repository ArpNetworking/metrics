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
package com.arpnetworking.configuration.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * <code>JsonNode</code> based configuration sourced from a file.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class JsonNodeFileSource extends BaseJsonNodeSource implements JsonNodeSource {

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
        return MoreObjects.toStringHelper(JsonNodeMergingSource.class)
                .add("File", _file)
                .add("JsonNode", _jsonNode)
                .toString();
    }

    /* package private */ Optional<JsonNode> getJsonNode() {
        return _jsonNode;
    }

    private JsonNodeFileSource(final Builder builder) {
        super(builder);
        _file = builder._file;

        JsonNode jsonNode = null;
        if (_file.canRead()) {
            try {
                jsonNode = _objectMapper.readTree(_file);
            } catch (final IOException e) {
                Throwables.propagate(e);
            }
        } else if (builder._file.exists()) {
            LOGGER.warn(String.format("Cannot read file; file=%s", _file));
        } else {
            LOGGER.debug(String.format("File does not exist; file=%s", _file));
        }
        _jsonNode = Optional.fromNullable(jsonNode);
    }

    private final File _file;
    private final Optional<JsonNode> _jsonNode;

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNodeFileSource.class);

    /**
     * Builder for <code>JsonNodeFileSource</code>.
     */
    public static final class Builder extends BaseJsonNodeSource.Builder<Builder, JsonNodeFileSource> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(JsonNodeFileSource.class);
        }

        /**
         * Set the source <code>File</code>.
         *
         * @param value The source <code>File</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setFile(final File value) {
            _file = value;
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
        private File _file;
    }
}
