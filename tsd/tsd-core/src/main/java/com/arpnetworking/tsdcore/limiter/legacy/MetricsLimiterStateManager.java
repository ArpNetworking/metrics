/**
 * Copyright 2014 Groupn.com
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
package com.arpnetworking.tsdcore.limiter.legacy;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.limiter.legacy.LegacyMetricsLimiter.Mark;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joda.time.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the state of the metrics limiter.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
public final class MetricsLimiterStateManager implements Runnable {

    /**
     * Request a write.
     */
    public void requestWrite() {
        _writeRequests.release(1);
    }

    /**
     * Flush marks to state file.
     */
    public void writeState() {

        // Write the current mark times to a temp file
        final Path newFilePath = _stateFile.resolveSibling(_stateFile.getFileName() + ".tmp");
        try {
            com.google.common.io.Files.asCharSink(newFilePath.toFile(), Charsets.UTF_8).writeLines(
                    Iterables.transform(
                            _marks.entrySet(),
                            aggregationMarkTime -> String.format(
                                    "%d %d %s",
                                    aggregationMarkTime.getValue().getTime(),
                                    aggregationMarkTime.getValue().getCount(),
                                    aggregationMarkTime.getKey()))
                    );
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    String.format(
                        "Failed to flush state file; could not write temp file; stateFile=%s, tempFile=%s",
                        _stateFile,
                        newFilePath),
                    e);
        }

        // Make the temp file the current file
        try {
            Files.move(newFilePath, _stateFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Failed to flush state file; could not move temp file; stateFile=%s, tempFile=%s",
                            _stateFile,
                            newFilePath),
                    e);
        }

        LOGGER.info()
                .setMessage("State file flushed")
                .addData("file", _stateFile)
                .log();
    }

    /**
     * Read marks from the state file.
     *
     * @return <code>Map</code> of the metric to its <code>Mark</code>.
     */
    public Map<String, Mark> readState() {

        // If there's no file, return the empty map
        if (!Files.exists(_stateFile)) {
            return Collections.emptyMap();
        }

        final Map<String, Mark> marks = Maps.newHashMap();
        try {
            for (final String line : Files.readAllLines(_stateFile, Charsets.UTF_8)) {
                final Matcher match = STATE_FILE_LINE_PATTERN.matcher(line);
                if (!match.lookingAt()) {
                    LOGGER.warn()
                            .setMessage("Ignoring unparsable line in state file")
                            .addData("file", _stateFile)
                            .addData("line", line)
                            .log();
                    continue;
                }

                final String metric = match.group(3);
                final Mark mark;
                try {
                    final int count = Integer.parseInt(match.group(2));
                    final long time = Long.parseLong(match.group(1));
                    mark = new Mark(count, time);
                } catch (final NumberFormatException e) {
                    LOGGER.warn()
                            .setMessage("Parsing error on line in state file")
                            .addData("file", _stateFile)
                            .addData("line", line)
                            .setThrowable(e)
                            .log();
                    continue;
                }

                marks.put(metric, mark);
            }
            return marks;
        } catch (final IOException e) {
            LOGGER.error()
                    .setMessage("Could not read state file")
                    .addData("file", _stateFile)
                    .setThrowable(e)
                    .log();
            return Collections.emptyMap();
        }
    }

    /**
     * Start the auto writer.
     */
    public void startAutoWriter() {
        synchronized (_autoWriterMutex) {
            if (_autoWriterThread == null) {
                LOGGER.info()
                        .setMessage("AutoWriter starting")
                        .addData("file", _stateFile)
                        .log();
                _stop = false;
                _autoWriterThread = new Thread(this, "MetricsLimiterAutoWriter");
                Runtime.getRuntime().addShutdownHook(_autoWriterShutdown);
                _autoWriterThread.start();
            } else {
                LOGGER.warn()
                        .setMessage("AutoWriter already started")
                        .addData("file", _stateFile)
                        .log();
            }
        }
    }

    // We typically remove the shutdown hook while testing, but when the for-real
    // shutdown happens, the hook can't be removed.
    void stopAutoWriter(final boolean removeShutdownHook) {
        synchronized (_autoWriterMutex) {
            if (_autoWriterThread != null) {
                LOGGER.info()
                        .setMessage("AutoWriter stopping")
                        .addData("file", _stateFile)
                        .log();
                _stop = true;
                _autoWriterThread.interrupt();
                // Wait up to 500ms for thread to die
                try {
                    _autoWriterThread.join(500);
                } catch (final InterruptedException e) {
                    LOGGER.warn()
                            .setMessage("AutoWriter failed to terminate")
                            .addData("file", _stateFile)
                            .setThrowable(e)
                            .log();
                }
                if (removeShutdownHook) {
                    Runtime.getRuntime().removeShutdownHook(_autoWriterShutdown);
                }
                _autoWriterThread = null;
            } else {
                LOGGER.warn()
                        .setMessage("AutoWriter not running")
                        .addData("file", _stateFile)
                        .log();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug()
                .setMessage("AutoWriter running")
                .addData("file", _stateFile)
                .log();
        try {
            while (true) {
                try {
                    if (_stop) {
                        return;
                    }

                    waitForRequestOrTimeout();

                    // Either we timed-out or somebody incremented the semaphore, either way, we want to write the
                    // file. We want to write the file one last time, so we don't check the stop flag until after
                    // the write
                    writeState();
                    if (_stop) {
                        return;
                    }
                    // And we don't want to run more frequently than every 500ms
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    if (_stop) {
                        return;
                    }
                }
            }
        } finally {
            // Write the file one last time
            writeState();
            LOGGER.debug()
                    .setMessage("AutoWriter stopped")
                    .addData("file", _stateFile)
                    .log();
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("stateFile", _stateFile)
                .put("flushInterval", _stateFileFlushInterval)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED")
    private void waitForRequestOrTimeout() throws InterruptedException {
        _writeRequests.tryAcquire(_stateFileFlushInterval.getMillis(), TimeUnit.MILLISECONDS);
        _writeRequests.drainPermits();
    }

    boolean isAlive() {
        return _autoWriterThread != null && _autoWriterThread.isAlive();
    }

    private MetricsLimiterStateManager(final Builder builder) {
        this._stateFile = builder._stateFile;
        this._stateFileFlushInterval = builder._stateFileFlushInterval;
        this._marks = builder._marks;

        // Create the parent directories if necessary
        final Path dir = _stateFile.getParent();
        try {
            Files.createDirectories(dir);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    String.format(
                        "State file path could not be created; stateFile=%s",
                        _stateFile),
                    e);
        }

        // Using tmp defeats the purpose
        if (_stateFile.startsWith("/tmp")) {
            LOGGER.warn(
                    "Storing the aggregator state file in /tmp is not recommended because"
                            + "on many platforms /tmp is not persisted across reboots");
        }
    }

    private final Semaphore _writeRequests = new Semaphore(0);
    private final Object _autoWriterMutex = new Object();
    private volatile boolean _stop = false;
    private Thread _autoWriterThread;
    private final Thread _autoWriterShutdown = new Thread("MetricsLimiterShutdown") {
        @Override
        public void run() {
            stopAutoWriter(false);
        }
    };

    private final Path _stateFile;
    private final Duration _stateFileFlushInterval;
    private final ConcurrentMap<String, Mark> _marks;

    private static final Path DEFAULT_STATE_FILE = Paths.get("/var/db/tsd-aggregator/tsd-aggregator-state");
    private static final Duration DEFAULT_STATE_FILE_FLUSH_INTERVAL = Duration.standardMinutes(5);
    private static final Pattern STATE_FILE_LINE_PATTERN = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(.+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsLimiterStateManager.class);

    /**
     * Builder for <code>MetricsStateLimiterManager</code>.
     *
     * @author Joe Frisbie (jfrisbie at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<MetricsLimiterStateManager> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(MetricsLimiterStateManager.class);
        }

        /**
         * Build instance of <code>MetricsLimiterStateManager</code>.
         *
         * @param marks <code>Map</code> of metric to <code>Mark</code>.
         * @return Instance of <code>MetricsLimiterStateManager</code>.
         */
        public MetricsLimiterStateManager build(final ConcurrentMap<String, Mark> marks) {
            this._marks = marks;
            return build();
        }

        /**
         * Set the state file.
         *
         * @param stateFile The state file.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFile(final Path stateFile) {
            this._stateFile = stateFile;
            return this;
        }

        /**
         * Set the state file flush interval. Optional.
         *
         * @param stateFileFlushInterval The state file flush interval.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateFileFlushInterval(final Duration stateFileFlushInterval) {
            this._stateFileFlushInterval = stateFileFlushInterval;
            return this;
        }

        private Path _stateFile = DEFAULT_STATE_FILE;
        private Duration _stateFileFlushInterval = DEFAULT_STATE_FILE_FLUSH_INTERVAL;
        private ConcurrentMap<String, Mark> _marks;
    }
}
