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
package com.arpnetworking.configuration.triggers;

import com.arpnetworking.configuration.Trigger;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <code>Trigger</code> implementation based on the contents of a directory.
 * Configurable to either monitor all content or only specific content. The
 * <code>FileTrigger</code> is used to determine whether a file changed. This
 * is intended to monitor the files in a single directory and is not designed
 * to monitor a directory tree (e.g. it is not recursive).
 *
 * <b>Note:</b> If you are monitoring a directory in which files are created
 * and removed this implementation needs to be modified to purge file trigger
 * instances when a file is removed!
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DirectoryTrigger implements Trigger {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluateAndReset() {
        final boolean exists = _directory.exists() && _directory.isDirectory();

        // Track directory contents
        if (exists) {
            for (final File file : MoreObjects.firstNonNull(_directory.listFiles(), EMPTY_FILE_ARRAY)) {
                // Note: To save memory in cases where a directory contains
                // temporary files (e.g. create, destroy and never recreate)
                // we should remove any triggers for files that no longer exist
                // also noting that a deletion should trigger a reload!
                // TODO(vkoskela): Implement memory saving [MAI-444]
                if (!_evaluationFileTriggers.containsKey(file.getName()) && isFileMonitored(file)) {
                    LOGGER.debug(String.format("Directory trigger monitoring new file; file=%s", file));
                    _evaluationFileTriggers.put(
                            file.getName(),
                            new FileTrigger.Builder()
                                    .setFile(file)
                                    .build());
                }
            }
        } else {
            _evaluationFileTriggers.clear();
        }

        // Check files for changes
        boolean fileChanged = false;
        for (final FileTrigger fileTrigger : _evaluationFileTriggers.values()) {
            fileChanged = fileChanged || fileTrigger.evaluateAndReset();
        }

        // Handle directory creation/removal
        if (!_exists.isPresent()) {
            LOGGER.debug(String.format(
                    "Directory trigger initialized; directory=%s",
                    _directory));

            _exists = Optional.of(exists);
            return true;
        } else if (_exists.get() != exists) {
            LOGGER.debug(String.format(
                    "Directory created or removed; directory=%s",
                    _directory));

            _exists = Optional.of(exists);
            return true;
        }

        if (fileChanged) {
            LOGGER.debug(String.format(
                    "Directory contents changed; directory=%s",
                    _directory));
        }
        return fileChanged;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Directory", _directory)
                .add("Exists", _exists)
                .add("FileNames", _fileNames)
                .add("FileNamePatterns", _fileNamePatterns)
                .add("EvaluationFileTriggers", _evaluationFileTriggers)
                .toString();
    }

    private DirectoryTrigger(final Builder builder) {
        _directory = builder._directory;
        _fileNames = Sets.newHashSet(builder._fileNames);
        _fileNamePatterns = builder._fileNamePatterns;
    }

    private final File _directory;
    private final Set<String> _fileNames;
    private final List<Pattern> _fileNamePatterns;

    private Optional<Boolean> _exists = Optional.absent();
    private final Map<String, FileTrigger> _evaluationFileTriggers = Maps.newHashMap();

    private static final File[] EMPTY_FILE_ARRAY = new File[0];
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryTrigger.class);

    /**
     * Builder for <code>DirectoryTrigger</code>.
     */
    public static final class Builder extends OvalBuilder<DirectoryTrigger> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DirectoryTrigger.class);
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

        @NotNull
        private File _directory;
        @NotNull
        private List<String> _fileNames = Lists.newArrayList();
        @NotNull
        private List<Pattern> _fileNamePatterns = Lists.newArrayList();
    }
}
