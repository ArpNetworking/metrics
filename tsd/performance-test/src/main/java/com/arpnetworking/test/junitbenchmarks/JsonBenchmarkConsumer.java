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

package com.arpnetworking.test.junitbenchmarks;

import com.carrotsearch.junitbenchmarks.AutocloseConsumer;
import com.carrotsearch.junitbenchmarks.GCSnapshot;
import com.carrotsearch.junitbenchmarks.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Writes a JSON file with benchmarking results.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class JsonBenchmarkConsumer extends AutocloseConsumer implements Closeable {
    /**
     * Public constructor.
     *
     * @param path Path of the file to write.
     */
    public JsonBenchmarkConsumer(final Path path) {
        _path = path;
        addAutoclose(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(final Result result) throws IOException {
        if (_closed) {
            throw new IllegalStateException("consumer is already closed");
        }
        _results.add(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (!_closed) {
            try {
                if (!Files.exists(_path.toAbsolutePath().getParent())) {
                    Files.createDirectories(_path.toAbsolutePath().getParent());
                }
                LOGGER.info(String.format("Closing; file=%s", _path));
                MAPPER.writeValue(_path.toFile(), this);
            } catch (final IOException e) {
                LOGGER.error("Could not write json performance file", e);
            }
            _closed = true;
        }
    }

    public List<Result> getResults() {
        return Collections.unmodifiableList(_results);
    }

    private boolean _closed = false;

    private final List<Result> _results = Lists.newArrayList();
    private final Path _path;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonBenchmarkConsumer.class);

    static {
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(GCSnapshot.class, new GCSnapshotSerializer());
        MAPPER.registerModule(simpleModule);
    }
}
