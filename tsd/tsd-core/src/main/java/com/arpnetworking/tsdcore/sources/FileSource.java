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
package com.arpnetworking.tsdcore.sources;

import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.parsers.exceptions.ParsingException;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Produce instances of <code>T</code>from a file. Supports rotating files
 * using <code>Tailer</code> from Apache Commons IO.
 *
 * @param <T> The data type to parse from the <code>Source</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class FileSource<T> extends BaseSource {

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        _tailerExecutor.execute(_tailer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        _tailer.stop();
        _tailerExecutor.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("File", _file)
                .add("Parser", _parser)
                .toString();
    }

    @SuppressWarnings("unused")
    private FileSource(final Builder<T> builder) {
        this(builder, LOGGER);
    }

    // NOTE: Package private for testing
    /*package private*/FileSource(final Builder<T> builder, final Logger logger) {
        super(builder);
        _logger = logger;
        _file = new File(builder._filePath);
        _parser = builder._parser;
        _tailer = new Tailer(_file, new LogTailerListener(), builder._interval.longValue(), false);
        _tailerExecutor = Executors.newSingleThreadExecutor();
    }

    private final File _file;
    private final Parser<T> _parser;
    private final Tailer _tailer;
    private final ExecutorService _tailerExecutor;
    private final Logger _logger;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSource.class);
    private static final Period FILE_NOT_FOUND_WARNING_INTERVAL = Period.minutes(1);

    private class LogTailerListener implements TailerListener {

        @Override
        public void init(final Tailer tailer) {
            _logger.debug(String.format("Tailer initialized; source=%s", FileSource.this));
        }

        @Override
        public void fileNotFound() {
            final DateTime now = DateTime.now();
            if (!_lastFileNotFoundWarning.isPresent()
                    || _lastFileNotFoundWarning.get().isBefore(now.minus(FILE_NOT_FOUND_WARNING_INTERVAL))) {
                _logger.warn(String.format("Tailer file not found; source=%s", FileSource.this));
                _lastFileNotFoundWarning = Optional.of(now);
            }
        }

        @Override
        public void fileRotated() {
            _logger.info(String.format("Tailer file rotate; source=%s", FileSource.this));
        }

        @Override
        public void handle(final String line) {
            _logger.trace(String.format("Tailer reading line; source=%s, line=%s", FileSource.this, line));
            T record;
            try {
                record = _parser.parse(line.getBytes(Charsets.UTF_8));
            } catch (final ParsingException e) {
                _logger.error("Failed to parse data", e);
                return;
            }
            FileSource.this.notify(FileSource.this, record);
        }

        @Override
        public void handle(final Exception e) {
            _logger.error(String.format("Tailer exception; source=%s", FileSource.this), e);
        }

        private Optional<DateTime> _lastFileNotFoundWarning = Optional.absent();
    }

    /**
     * Implementation of builder pattern for <code>FileSource</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder<T> extends BaseSource.Builder<Builder<T>> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(FileSource.class);
        }

        /**
         * Sets file path. Cannot be null or empty.
         * 
         * @param value The file path.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setFilePath(final String value) {
            _filePath = value;
            return this;
        }

        /**
         * Sets file read interval in milliseconds. Cannot be null, minimum 1.
         * Default is 500 milliseconds.
         * 
         * @param value The file read interval in milliseconds.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setInterval(final Long value) {
            _interval = value;
            return this;
        }

        /**
         * Sets <code>Parser</code>. Cannot be null.
         * 
         * @param value The <code>Parser</code>.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setParser(final Parser<T> value) {
            _parser = value;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder<T> self() {
            return this;
        }

        @NotNull
        @NotEmpty
        private String _filePath;
        @NotNull
        @Min(value = 1)
        private Long _interval = Long.valueOf(500);
        @NotNull
        private Parser<T> _parser;
    }
}
