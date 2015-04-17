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
package com.arpnetworking.metrics.impl;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import com.arpnetworking.logback.StenoEncoder;
import com.arpnetworking.logback.StenoMarker;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Unit;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of <code>Sink</code> for the query log. For an example of its
 * use please refer to the documentation for <code>TsdMetricsFactory</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdQueryLogSink implements Sink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void record(
            final Map<String, String> annotations,
            final Map<String, List<Quantity>> timerSamples,
            final Map<String, List<Quantity>> counterSamples,
            final Map<String, List<Quantity>> gaugeSamples) {

        try {
            //final String jsonString = _objectMapper.writeValueAsString(
            final JsonNode jsonNode = _objectMapper.valueToTree(
                    new Entry(
                            annotations,
                            timerSamples,
                            counterSamples,
                            gaugeSamples));

            if (!(jsonNode instanceof ObjectNode)) {
                throw new IOException("Unexpected serialization into non-object node");
            }
            final ObjectNode objectNode = (ObjectNode) jsonNode;

            final List<String> keys = new ArrayList<>(5);
            final List<String> values = new ArrayList<>(5);
            for (final Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields(); iterator.hasNext();) {
                final Map.Entry<String, JsonNode> field = iterator.next();
                keys.add(field.getKey());
                values.add(_objectMapper.writeValueAsString(field.getValue()));
            }

            // TODO(vkoskela): Simplify with JSON_OBJECT_MARKER [MAI-250]
            _queryLogger.info(
                    StenoMarker.ARRAY_JSON_MARKER,
                    "aint.metrics",
                    keys.toArray(new String[keys.size()]),
                    values.toArray(new String[values.size()]));

        } catch (final IOException e) {
            // This is in place of an exception; see class Javadoc
            _logger.warn("Exception serializing and writing metrics", e);
        }
    }

    // NOTE: Package private for testing.
    Logger getQueryLogger() {
        return _queryLogger;
    }

    private TimeBasedRollingPolicy<ILoggingEvent> createRollingPolicy(
            final String extension,
            final String fileNameWithoutExtension,
            final int maxHistory) {
        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(_loggerContext);
        rollingPolicy.setMaxHistory(maxHistory);
        rollingPolicy.setCleanHistoryOnStart(true);
        rollingPolicy.setFileNamePattern(fileNameWithoutExtension + DATE_EXTENSION + extension + GZIP_EXTENSION);
        return rollingPolicy;
    }

    private Encoder<ILoggingEvent> createEncoder(final boolean immediateFlush) {
        final StenoEncoder encoder = new StenoEncoder();
        encoder.setContext(_loggerContext);
        encoder.setImmediateFlush(immediateFlush);
        return encoder;
    }

    private FileAppender<ILoggingEvent> createRollingAppender(
            final String fileName,
            final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy,
            final Encoder<ILoggingEvent> encoder) {
        final RollingFileAppender<ILoggingEvent> rollingAppender = new RollingFileAppender<>();
        rollingAppender.setContext(_loggerContext);
        rollingAppender.setName("query-log");
        rollingAppender.setFile(fileName);
        rollingAppender.setAppend(true);
        rollingAppender.setRollingPolicy(rollingPolicy);
        rollingAppender.setEncoder(encoder);
        return rollingAppender;
    }

    private Appender<ILoggingEvent> createAsyncAppender(final Appender<ILoggingEvent> appender) {
        final AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(_loggerContext);
        asyncAppender.setDiscardingThreshold(0);
        asyncAppender.setName("query-log-async");
        asyncAppender.setQueueSize(500);
        asyncAppender.addAppender(appender);
        return asyncAppender;
    }

    /**
     * Protected constructor.
     * 
     * @param builder Instance of <code>Builder</code>.
     */
    protected TsdQueryLogSink(final Builder builder) {
        this(builder, OBJECT_MAPPER, LOGGER);
    }

    // NOTE: Package private for testing
    /* package private */TsdQueryLogSink(final Builder builder, final ObjectMapper objectMapper, final org.slf4j.Logger logger) {
        _loggerContext = new LoggerContext();

        final String path = builder._path;
        final String extension = builder._extension;
        final boolean immediateFlush = builder._immediateFlush.booleanValue();
        final int maxHistory = builder._maxHistory.intValue();

        final StringBuilder fileNameBuilder = new StringBuilder(path);
        if (!path.isEmpty() && !path.endsWith(File.separator)) {
            fileNameBuilder.append(File.separator);
        }
        fileNameBuilder.append(builder._name);
        final String fileNameWithoutExtension = fileNameBuilder.toString();
        fileNameBuilder.append(extension);
        final String fileName = fileNameBuilder.toString();

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = createRollingPolicy(
                extension,
                fileNameWithoutExtension,
                maxHistory);
        final Encoder<ILoggingEvent> encoder = createEncoder(immediateFlush);
        final FileAppender<ILoggingEvent> rollingAppender = createRollingAppender(fileName, rollingPolicy, encoder);
        final Appender<ILoggingEvent> asyncAppender = createAsyncAppender(rollingAppender);

        rollingPolicy.setParent(rollingAppender);
        rollingPolicy.start();
        encoder.start();
        rollingAppender.start();
        asyncAppender.start();

        final Logger rootLogger = _loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(asyncAppender);

        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(_loggerContext));

        _queryLogger = _loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        _objectMapper = objectMapper;
        _logger = logger;
    }

    private final LoggerContext _loggerContext;
    private final Logger _queryLogger;
    private final ObjectMapper _objectMapper;
    private final org.slf4j.Logger _logger;

    private static final String DATE_EXTENSION = ".%d{yyyy-MM-dd-HH}";
    private static final String GZIP_EXTENSION = ".gz";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TsdQueryLogSink.class);

    static {
        final SimpleModule simpleModule = new SimpleModule("TsdMetrics");
        simpleModule.addSerializer(Entry.class, EntrySerializer.newInstance());
        simpleModule.addSerializer(Quantity.class, QuantitySerializer.newInstance());
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        OBJECT_MAPPER.registerModule(simpleModule);
    }

    private static final class Entry {

        public Map<String, String> getAnnotations() {
            return _annotations;
        }

        public Map<String, List<Quantity>> getTimerSamples() {
            return _timerSamples;
        }

        public Map<String, List<Quantity>> getCounterSamples() {
            return _counterSamples;
        }

        public Map<String, List<Quantity>> getGaugeSamples() {
            return _gaugeSamples;
        }

        public Entry(
                final Map<String, String> annotations,
                final Map<String, List<Quantity>> timerSamples,
                final Map<String, List<Quantity>> counterSamples,
                final Map<String, List<Quantity>> gaugeSamples) {
            _annotations = annotations;
            _timerSamples = timerSamples;
            _counterSamples = counterSamples;
            _gaugeSamples = gaugeSamples;
        }

        private final Map<String, String> _annotations;
        private final Map<String, List<Quantity>> _timerSamples;
        private final Map<String, List<Quantity>> _counterSamples;
        private final Map<String, List<Quantity>> _gaugeSamples;
    }

    private static final class EntrySerializer extends JsonSerializer<Entry> {

        public static JsonSerializer<Entry> newInstance() {
            return new EntrySerializer();
        }

        @Override
        public void serialize(
                final Entry entry,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {

            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("version", "2e");
            jsonGenerator.writeObjectField("annotations", entry.getAnnotations());
            if (!entry.getCounterSamples().isEmpty()) {
                jsonGenerator.writeObjectFieldStart("counters");
                serializeSamples(entry.getCounterSamples(), jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            if (!entry.getGaugeSamples().isEmpty()) {
                jsonGenerator.writeObjectFieldStart("gauges");
                serializeSamples(entry.getGaugeSamples(), jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            if (!entry.getTimerSamples().isEmpty()) {
                jsonGenerator.writeObjectFieldStart("timers");
                serializeSamples(entry.getTimerSamples(), jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }

        private void serializeSamples(
                final Map<String, ? extends Collection<? extends Quantity>> samples,
                final JsonGenerator jsonGenerator)
                throws IOException {
            for (final Map.Entry<String, ? extends Collection<? extends Quantity>> entry : samples.entrySet()) {
                jsonGenerator.writeObjectFieldStart(entry.getKey());
                jsonGenerator.writeObjectField("values", entry.getValue());
                jsonGenerator.writeEndObject();
            }
        }

        private EntrySerializer() {}
    }

    private static final class QuantitySerializer extends JsonSerializer<Quantity> {

        public static JsonSerializer<Quantity> newInstance() {
            return new QuantitySerializer();
        }

        @Override
        public void serialize(
                final Quantity valueWithUnit,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("value", valueWithUnit.getValue());
            final Unit unit = valueWithUnit.getUnit();
            if (unit != null) {
                jsonGenerator.writeStringField("unit", unit.getSerializedName());
            }
            jsonGenerator.writeEndObject();
        }
    }

    // NOTE: Package private for testing
    /* package private */static final class ShutdownHookThread extends Thread {

        public ShutdownHookThread(final LoggerContext context) {
            _context = context;
        }

        @Override
        public void run() {
            _context.stop();
        }

        private final LoggerContext _context;
    }

    /**
     * Builder for <code>TsdQueryLogSink</code>.
     * 
     * This class is thread safe.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder {

        /**
         * Create an instance of <code>Sink</code>.
         * 
         * @return Instance of <code>Sink</code>.
         */
        public Sink build() {
            if (_path == null) {
                throw new IllegalArgumentException("Path cannot be null");
            }
            if (_name == null || _name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            if (_extension == null) {
                throw new IllegalArgumentException("Extension cannot be null");
            }
            if (_immediateFlush == null) {
                throw new IllegalArgumentException("ImmediateFlush cannot be null");
            }
            if (_maxHistory == null) {
                throw new IllegalArgumentException("MaxHistory cannot be null");
            }
            if (_maxHistory.intValue() < 0) {
                throw new IllegalArgumentException("MaxHistory cannot be negative");
            }
            return new TsdQueryLogSink(this);
        }

        /**
         * Set the path. Optional; default is empty string which defaults to a
         * the current working directory of the application.
         * 
         * @param value The value for path.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPath(final String value) {
            _path = value;
            return this;
        }

        /**
         * Set the file name without extension. Optional; default is "query".
         * The file name without extension cannot be empty.
         * 
         * @param value The value for name.
         * @return This <code>Builder</code> instance.
         */
        public Builder setName(final String value) {
            _name = value;
            return this;
        }

        /**
         * Set the file extension. Optional; default is ".log".
         * 
         * @param value The value for extension.
         * @return This <code>Builder</code> instance.
         */
        public Builder setExtension(final String value) {
            _extension = value;
            return this;
        }

        /**
         * Set whether entries are flushed immediately. Entries are still 
         * written asynchronously. Optional; default is true.
         * 
         * @param value Whether to flush immediately.
         * @return This <code>Builder</code> instance.
         */
        public Builder setImmediateFlush(final Boolean value) {
            _immediateFlush = value;
            return this;
        }

        /**
         * Set the maximum number of historical (e.g. number of rotated files
         * to retain). Files are rotated hourly, so this is equivalent to the
         * number of hours of logs to retain. Optional; default is 24.
         *
         * @param value Maximum number of historical (e.g. rotated) files to retain.
         * @return This <code>Builder</code> instance.
         */
        public Builder setMaxHistory(final Integer value) {
            _maxHistory = value;
            return this;
        }

        private String _path = DEFAULT_PATH;
        private String _name = DEFAULT_NAME;
        private String _extension = DEFAULT_EXTENSION;
        private Boolean _immediateFlush = DEFAULT_IMMEDIATE_FLUSH;
        private Integer _maxHistory = DEFAULT_MAX_HISTORY;

        private static final String DEFAULT_PATH = "";
        private static final String DEFAULT_NAME = "query";
        private static final String DEFAULT_EXTENSION = ".log";
        private static final Boolean DEFAULT_IMMEDIATE_FLUSH = true;
        private static final Integer DEFAULT_MAX_HISTORY = 24;
    }
}
