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
package com.arpnetworking.tsdcore.tailer;

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.utility.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of <code>PositionStore</code> which stores the read
 * position in a file on local disk. This class is thread-safe per file
 * identifier.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class FilePositionStore implements PositionStore {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> getPosition(final String identifier) {
        final Descriptor descriptor = _state.get(identifier);
        if (descriptor == null) {
            return Optional.absent();
        }
        return Optional.of(descriptor.getPosition());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPosition(final String identifier, final long position) {
        final Descriptor descriptor = _state.putIfAbsent(
                identifier,
                new Descriptor.Builder()
                        .setPosition(Long.valueOf(position))
                        .build());

        final DateTime now = DateTime.now();
        boolean requiresFlush = now.minus(_flushInterval).isAfter(_lastFlush);
        if (descriptor != null) {
            descriptor.update(position, now);
            requiresFlush = requiresFlush || descriptor.getDelta() > _flushThreshold;
        }
        if (requiresFlush) {
            flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        flush();
    }

    private void flush() {
        // Age out old state
        final DateTime now = DateTime.now();
        final DateTime oldest = now.minus(_retention);
        final long sizeBefore = _state.size();
        Maps.filterEntries(_state, new Predicate<Map.Entry<String, Descriptor>>() {
            @Override
            public boolean apply(final Map.Entry<String, Descriptor> entry) {
                return oldest.isBefore(entry.getValue().getLastUpdated());
            }
        });
        final long sizeAfter = _state.size();
        if (sizeBefore != sizeAfter) {
            LOGGER.debug(String.format(
                    "Removed old entries from file position store; sizeBefore=%d, sizeAfter=%d",
                    Long.valueOf(sizeBefore),
                    Long.valueOf(sizeAfter)));
        }

        // Persist the state to disk
        try {
            OBJECT_MAPPER.writeValue(_file, _state);

            LOGGER.debug(String.format(
                    "Persisted file position state to disk; size=%d, file=%s",
                    Long.valueOf(_state.size()),
                    _file));
        } catch (final IOException ioe) {
            Throwables.propagate(ioe);
        } finally {
            _lastFlush = now;
        }
    }

    private FilePositionStore(final Builder builder) {
        _file = builder._file;
        _flushInterval = builder._flushInterval;
        _flushThreshold = builder._flushThreshold.longValue();
        _retention = builder._retention;

        ConcurrentMap<String, Descriptor> state = Maps.newConcurrentMap();
        try {
            state = OBJECT_MAPPER.readValue(_file, STATE_MAP_TYPE_REFERENCE);
        } catch (final IOException e) {
            LOGGER.warn(String.format("Unable to load state; file=%s", _file), e);
        }
        _state = state;
    }

    private final File _file;
    private final Duration _flushInterval;
    private final long _flushThreshold;
    private final Duration _retention;
    private final ConcurrentMap<String, Descriptor> _state;

    private DateTime _lastFlush = DateTime.now();

    private static final TypeReference<ConcurrentMap<String, Descriptor>> STATE_MAP_TYPE_REFERENCE =
            new TypeReference<ConcurrentMap<String, Descriptor>>(){};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(FilePositionStore.class);

    static {
        final SimpleModule module = new SimpleModule("FilePositionStore");
        module.addDeserializer(Descriptor.class, BuilderDeserializer.of(Descriptor.Builder.class));
        OBJECT_MAPPER.registerModules(module);
        OBJECT_MAPPER.registerModule(new JodaModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final class Descriptor {

        public void update(final long position, final DateTime updatedAt) {
            _delta += position - _position;
            _lastUpdated = updatedAt;
            _position = position;
        }

        public void flush() {
            _delta = 0;
        }

        public long getPosition() {
            return _position;
        }

        public DateTime getLastUpdated() {
            return _lastUpdated;
        }

        @JsonIgnore
        public long getDelta() {
            return _delta;
        }

        private Descriptor(final Builder builder) {
            _position = builder._position.longValue();
            _lastUpdated = builder._lastUpdated;
            _delta = 0;
        }

        private long _position;
        private DateTime _lastUpdated;
        private long _delta;

        public static class Builder extends OvalBuilder<Descriptor> {

            public Builder() {
                super(Descriptor.class);
            }

            public Builder setPosition(final Long value) {
                _position = value;
                return this;
            }

            public Builder setLastUpdated(final DateTime value) {
                _lastUpdated = value;
                return this;
            }

            @NotNull
            private Long _position;
            @NotNull
            private DateTime _lastUpdated = DateTime.now();
        }
    }

    /**
     * Implementation of builder pattern for <code>FilePositionStore</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends OvalBuilder<FilePositionStore> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(FilePositionStore.class);
        }

        /**
         * Sets the file to store position in. Cannot be null or empty.
         *
         * @param value The file to store position in.
         * @return This instance of {@link Builder}
         */
        public Builder setFile(final File value) {
            _file = value;
            return this;
        }

        /**
         * Sets the interval between flushes to the position store. Optional.
         * Default is one minute.
         *
         * @param value The interval between flushes to the position store.
         * @return This instance of {@link Builder}
         */
        public Builder setFlushInterval(final Duration value) {
            _flushInterval = value;
            return this;
        }

        /**
         * Sets the minimum position delta threshold to initiate a flush of the
         * position store. Optional. Default is 1Mb (1024 * 1024 bytes).
         *
         * @param value The minimum position delta threshold.
         * @return This instance of {@link Builder}
         */
        public Builder setFlushThreshold(final Long value) {
            _flushThreshold = value;
            return this;
        }

        /**
         * Sets the duration of an entry in the position store. Optional.
         * Default is one day.
         *
         * @param value The retention of an entry in the position store.
         * @return This instance of {@link Builder}
         */
        public Builder setRetention(final Duration value) {
            _retention = value;
            return this;
        }

        @NotNull
        private File _file;
        @NotNull
        private Duration _flushInterval = Duration.standardSeconds(10);
        @NotNull
        @Min(0)
        private Long _flushThreshold = 10485760L; // 2^20 * 10 = (10 Mebibyte)
        @NotNull
        private Duration _retention = Duration.standardDays(1);
    }
}
