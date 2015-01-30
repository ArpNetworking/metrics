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

package com.arpnetworking.metrics.generator.client;

import com.arpnetworking.logback.StenoEncoder;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
// CHECKSTYLE.OFF: RegexpSingleline - We are replacing shaded classes, so we need these
import com.arpnetworking.metrics.ch.qos.logback.classic.LoggerContext;
import com.arpnetworking.metrics.ch.qos.logback.classic.spi.ILoggingEvent;
import com.arpnetworking.metrics.ch.qos.logback.core.FileAppender;
// CHECKSTYLE.ON: RegexpSingleline
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a sink to allow modification of the timestamps.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class GeneratorSink implements Sink {
    /**
     * Public constructor.
     *
     * @param outputPath The file to write to.
     * @param initialTime The time to use in the replacement.
     */
    public GeneratorSink(final Path outputPath, final DateTime initialTime) {
        _time = initialTime;
        final Path file = outputPath.toAbsolutePath().normalize();
        _wrapped = new TsdQueryLogSink.Builder()
                .setPath(file.getParent().toString())
                .setName(Files.getNameWithoutExtension(file.toString()))
                .setExtension(Files.getFileExtension(file.toString()))
                .build();
        replaceFileAppender(_wrapped, outputPath);
    }

    public void setTime(final DateTime time) {
        _time = time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void record(
            final Map<String, String> annotations,
            final Map<String, List<Quantity>> timerSamples,
            final Map<String, List<Quantity>> counterSamples,
            final Map<String, List<Quantity>> gaugeSamples) {
        final HashMap<String, String> modified = Maps.newHashMap(annotations);
        modified.put("initTimestamp", _time.withZone(DateTimeZone.UTC).toString());
        modified.put("finalTimestamp", _time.withZone(DateTimeZone.UTC).toString());
        _wrapped.record(modified, timerSamples, counterSamples, gaugeSamples);
    }

    /**
     * Flushes unwritten data to disk.
     */
    public void flush() {
        // Do not call _appender.stop() and then _appender.start(), there is a bug
        // where that doesn't flush the file.
        _appender.getEncoder().stop();
        _appender.getEncoder().start();
    }

    private void replaceFileAppender(final Sink queryLogSink, final Path outputPath) {
        try {
            final Field queryLoggerField = queryLogSink.getClass().getDeclaredField("_queryLogger");
            queryLoggerField.setAccessible(true);
            final com.arpnetworking.metrics.ch.qos.logback.classic.Logger queryLogger =
                    (com.arpnetworking.metrics.ch.qos.logback.classic.Logger) queryLoggerField.get(queryLogSink);

            final Field contextField = queryLogSink.getClass().getDeclaredField("_loggerContext");
            contextField.setAccessible(true);
            final LoggerContext loggerContext = (LoggerContext) contextField.get(queryLogSink);

            final StenoEncoder encoder = new StenoEncoder();
            encoder.setContext(loggerContext);
            encoder.setImmediateFlush(false);


            final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setAppend(false);
            fileAppender.setFile(outputPath.toAbsolutePath().toString());
            fileAppender.setName("hijacked-query-log");
            fileAppender.setContext(loggerContext);
            fileAppender.setEncoder(encoder);

            encoder.start();
            fileAppender.start();

            _appender = fileAppender;

            queryLogger.detachAppender("query-log-async");
            queryLogger.addAppender(fileAppender);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            Throwables.propagate(e);
        }
    }

    private FileAppender<ILoggingEvent> _appender;
    private DateTime _time;
    private final Sink _wrapped;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorSink.class);
}
