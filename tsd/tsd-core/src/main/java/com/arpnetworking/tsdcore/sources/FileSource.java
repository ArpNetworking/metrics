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
import com.arpnetworking.tsdcore.tailer.FilePositionStore;
import com.arpnetworking.tsdcore.tailer.InitialPosition;
import com.arpnetworking.tsdcore.tailer.NoPositionStore;
import com.arpnetworking.tsdcore.tailer.PositionStore;
import com.arpnetworking.tsdcore.tailer.StatefulTailer;
import com.arpnetworking.tsdcore.tailer.Tailer;
import com.arpnetworking.tsdcore.tailer.TailerListener;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("SourceFile", _sourceFile)
                .add("StateFile", _stateFile)
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
        _sourceFile = builder._sourceFile;
        _stateFile = builder._stateFile;
        _parser = builder._parser;
        final PositionStore positionStore;
        if (_stateFile == null) {
            positionStore = NO_POSITION_STORE;
        } else {
            positionStore = new FilePositionStore.Builder().setFile(_stateFile).build();
        }

        _tailer = new StatefulTailer.Builder()
                .setFile(_sourceFile)
                .setListener(new LogTailerListener())
                .setReadInterval(builder._interval)
                .setPositionStore(positionStore)
                .setInitialPosition(builder._initialPosition)
                .build();
        _tailerExecutor = Executors.newSingleThreadExecutor();
    }

    private final File _sourceFile;
    private final File _stateFile;
    private final Parser<T> _parser;
    private final Tailer _tailer;
    private final ExecutorService _tailerExecutor;
    private final Logger _logger;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSource.class);
    private static final Period FILE_NOT_FOUND_WARNING_INTERVAL = Period.minutes(1);
    private static final NoPositionStore NO_POSITION_STORE = new NoPositionStore();

    private class LogTailerListener implements TailerListener {

        @Override
        public void initialize(final Tailer tailer) {
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
        public void fileOpened() {
            _logger.info(String.format("Tailer file opened; source=%s", FileSource.this));
        }

        @Override
        public void handle(final byte[] line) {
            T record;
            try {
                record = _parser.parse(line);
            } catch (final ParsingException e) {
                _logger.error("Failed to parse data", e);
                return;
            }
            FileSource.this.notify(record);
        }

        @Override
        public void handle(final Throwable t) {
            _logger.error(String.format("Tailer exception; source=%s", FileSource.this), t);
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
         * Sets source file. Cannot be null.
         *
         * @param value The file path.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setSourceFile(final File value) {
            _sourceFile = value;
            return this;
        }

        /**
         * Sets file read interval in milliseconds. Cannot be null, minimum 1.
         * Default is 500 milliseconds.
         *
         * @param value The file read interval in milliseconds.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setInterval(final Duration value) {
            _interval = value;
            return this;
        }

        /**
         * Sets whether to tail the file from its end or from its start.
         * Default InitialPosition.START;
         *
         * @param value Initial position to tail from.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setInitialPosition(final InitialPosition value) {
            _initialPosition = value;
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
         * Sets state file. Optional. Default is null.
         * If null, uses a <code>NoPositionStore</code> in the underlying tailer.
         *
         * @param value The state file.
         * @return This instance of <code>Builder</code>.
         */
        public final Builder<T> setStateFile(final File value) {
            _stateFile = value;
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
        private File _sourceFile;
        @NotNull
        private Duration _interval = Duration.millis(500);
        @NotNull
        private Parser<T> _parser;
        private File _stateFile;
        @NotNull
        private InitialPosition _initialPosition = InitialPosition.START;
    }
}
