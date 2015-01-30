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
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <code>JsonNode</code> based configuration sourced from a directory. This
 * is intended to monitor the files in a single directory and is not designed
 * to monitor a directory tree (e.g. it is not recursive).
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class JsonNodeDirectorySource extends BaseJsonNodeSource implements JsonNodeSource {

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
        return MoreObjects.toStringHelper(JsonNodeDirectorySource.class)
                .add("Directory", _directory)
                .add("FileNames", _fileNames)
                .add("FileNamePatterns", _fileNamePatterns)
                .add("JsonNode", _jsonNode)
                .toString();
    }

    /* package private */ Optional<JsonNode> getJsonNode() {
        return _jsonNode;
    }

    private void writeKeyValue(final String key, final String value, final StringBuilder jsonBuilder) {
        jsonBuilder.append("\"");
        jsonBuilder.append(key);
        jsonBuilder.append("\":");
        jsonBuilder.append(value);
    }

    private boolean isFileMonitored(final File file) {
        if (_fileNames.isEmpty() && _fileNamePatterns.isEmpty()) {
            return true;
        } else {
            if (_fileNames.contains(file.getName())) {
                return true;
            }
            for (final Pattern pattern : _fileNamePatterns) {
                if (pattern.matcher(file.getName()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonNodeDirectorySource(final Builder builder) {
        super(builder);
        _directory = builder._directory;
        _fileNames = Sets.newHashSet(builder._fileNames);
        _fileNamePatterns = builder._fileNamePatterns;
        _fileToKey = builder._fileToKey;

        // Build the unified configuration
        final StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        // Process all matching files
        JsonNode jsonNode = null;
        if (_directory.exists() && _directory.isDirectory() && _directory.canRead()) {
            for (final File file : MoreObjects.firstNonNull(_directory.listFiles(), EMPTY_FILE_ARRAY)) {
                if (isFileMonitored(file)) {
                    LOGGER.debug(String.format("Loading configuration file; file=%s", file));
                    try {
                        writeKeyValue(_fileToKey.apply(file), Files.toString(file, Charsets.UTF_8), jsonBuilder);
                        jsonBuilder.append(",");
                    } catch (final IOException ioe) {
                        throw Throwables.propagate(ioe);
                    }
                }
            }

            // Complete and parse the configuration
            if (jsonBuilder.length() > 1) {
                jsonBuilder.setCharAt(jsonBuilder.length() - 1, '}');
            } else {
                jsonBuilder.append("}");
            }
            try {
                jsonNode = _objectMapper.readTree(jsonBuilder.toString());
            } catch (final IOException e) {
                Throwables.propagate(e);
            }
        }
        _jsonNode = Optional.fromNullable(jsonNode);
    }

    private final File _directory;
    private final Set<String> _fileNames;
    private final List<Pattern> _fileNamePatterns;
    private Function<File, String> _fileToKey;
    private final Optional<JsonNode> _jsonNode;

    private static final File[] EMPTY_FILE_ARRAY = new File[0];
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNodeDirectorySource.class);

    /**
     * Builder for <code>JsonNodeDirectorySource</code>.
     */
    public static final class Builder extends BaseJsonNodeSource.Builder<Builder, JsonNodeDirectorySource> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(JsonNodeDirectorySource.class);
        }

        /**
         * Set the directory.
         *
         * @param value The directory.
         * @return This <code>Builder</code> instance.
         */
        public Builder setDirectory(final File value) {
            _directory = value;
            return this;
        }

        /**
         * Set the <code>Collection</code> of file names. Optional. Default is
         * an empty list (e.g. all files). Cannot be null.
         *
         * <b>Note:</b> Both the file names and file name patterns must be
         * empty (e.g. unset) in order to consider all files in the directory.
         *
         * @param value The <code>Collection</code> of file names.
         * @return This <code>Builder</code> instance.
         */
        public Builder setFileNames(final Collection<String> value) {
            _fileNames = Lists.newArrayList(value);
            return this;
        }

        /**
         * Add a file name.
         *
         * @param value The file name.
         * @return This <code>Builder</code> instance.
         */
        public Builder addFileName(final String value) {
            if (_fileNames == null) {
                _fileNames = Lists.newArrayList(value);
            } else {
                _fileNames.add(value);
            }
            return this;
        }

        /**
         * Set the <code>Collection</code> of file name patterns. Optional.
         * Default is an empty list (e.g. all files). Cannot be null.
         *
         * <b>Note:</b> Both the file names and file name patterns must be
         * empty (e.g. unset) in order to consider all files in the directory.
         *
         * @param value The <code>Collection</code> of file name patterns.
         * @return This <code>Builder</code> instance.
         */
        public Builder setFileNamePatterns(final Collection<Pattern> value) {
            _fileNamePatterns = Lists.newArrayList(value);
            return this;
        }

        /**
         * Add a file name pattern.
         *
         * @param value The file name pattern.
         * @return This <code>Builder</code> instance.
         */
        public Builder addFileNamePattern(final Pattern value) {
            if (_fileNamePatterns == null) {
                _fileNamePatterns = Lists.newArrayList(value);
            } else {
                _fileNamePatterns.add(value);
            }
            return this;
        }

        /**
         * Set the file to key function. Optional. The default uses the file
         * name removing the ".json" extension if one exists.
         *
         * @param value The file to key function.
         * @return This <code>Builder</code> instance.
         */
        public Builder setFileToKey(final Function<File, String> value) {
            _fileToKey = value;
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
        private File _directory;
        @NotNull
        private List<String> _fileNames = Lists.newArrayList();
        @NotNull
        private List<Pattern> _fileNamePatterns = Lists.newArrayList();
        @NotNull
        private Function<File, String> _fileToKey = DEFAULT_FILE_TO_KEY_FUNCTION;

        private static final Function<File, String> DEFAULT_FILE_TO_KEY_FUNCTION = new Function<File, String>() {

            @Override
            public String apply(final File file) {
                final String fileName = file != null ? file.getName() : "";
                if (fileName.endsWith(".json")) {
                    return fileName.substring(0, fileName.length() - 5);
                } else {
                    return fileName;
                }
            }
        };
    }
}
