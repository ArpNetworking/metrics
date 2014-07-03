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

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of <code>MetricsFactory</code> for creating
 * <code>Metrics</code> instances for publication of time series data (TSD).
 *
 * For more information about the semantics of this class and its methods
 * please refer to the <code>MetricsFactory</code> interface documentation. To
 * create an instance of this class use the nested <code>Builder</code> class:
 *
 * {@code
 * final MetricsFactory metricsFactory = new MetricsFactory.Builder()
 *     .setSinks(Collections.singletonList(
 *         new TsdQueryLogSink.Builder().build()));
 *     .build();
 * }
 *
 * The above will write metrics to the current working directory in query.log.
 * It is strongly recommended that at least a path be set:
 *
 * {@code
 * final MetricsFactory metricsFactory = new MetricsFactory.Builder()
 *     .setSinks(Collections.singletonList(
 *         new TsdQueryLogSink.Builder()
 *             .setPath("/usr/local/var/my-app/logs")
 *             .build()));
 *     .build();
 * }
 *
 * The above will write metrics to /usr/local/var/my-app/logs in query.log.
 * Additionally, you can customize the base file name and extension for your
 * application. However, if you are using TSDAggregator remember to configure
 * it to match:
 *
 * {@code
 * final MetricsFactory metricsFactory = new MetricsFactory.Builder()
 *     .setSinks(Collections.singletonList(
 *         new TsdQueryLogSink.Builder()
 *             .setPath("/usr/local/var/my-app/logs")
 *             .setName("tsd")
 *             .setExtension(".txt")
 *             .build()));
 *     .build();
 * }
 *
 * The above will write metrics to /usr/local/var/my-app/logs in tsd.txt. The
 * extension is configured separately as the files are rolled over every hour
 * inserting a date-time between the name and extension like:
 *
 * query-log.YYYY-MM-DD-HH.log
 *
 * This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdMetricsFactory implements MetricsFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics create() {
        return new TsdMetrics(_sinks);
    }

    /**
     * Protected constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    protected TsdMetricsFactory(final Builder builder) {
        _sinks = Collections.unmodifiableList(new ArrayList<Sink>(builder._sinks));
    }

    private final List<Sink> _sinks;

    /**
     * Builder for <code>TsdMetricsFactory</code>.
     *
     * This class is thread safe.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder {

        /**
         * Create an instance of <code>MetricsFactory</code>.
         *
         * @return Instance of <code>MetricsFactory</code>.
         */
        public MetricsFactory build() {
            if (_sinks == null) {
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
                _sinks = Collections.singletonList(
                        new TsdQueryLogSink.Builder()
                                .setPath(_path)
                                .setName(_name)
                                .setExtension(_extension)
                                .setImmediateFlush(_immediateFlush)
                                .build());
            }
            return new TsdMetricsFactory(this);
        }

        /**
         * Set the sinks to publish to.
         *
         * @param value The sinks to publish to.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSinks(final List<Sink> value) {
            _sinks = value;
            return this;
        }

        /**
         * Set the path. Optional; default is empty string which defaults to a
         * the current working directory of the application.
         *
         * @param value The value for path.
         * @return This <code>Builder</code> instance.
         * @deprecated Use setSinks with TsdQueryLogSink instead.
         */
        @Deprecated
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
         * @deprecated Use setSinks with TsdQueryLogSink instead.
         */
        @Deprecated
        public Builder setName(final String value) {
            _name = value;
            return this;
        }

        /**
         * Set the file extension. Optional; default is ".log".
         *
         * @param value The value for extension.
         * @return This <code>Builder</code> instance.
         * @deprecated Use setSinks with TsdQueryLogSink instead.
         */
        @Deprecated
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
         * @deprecated Use setSinks with TsdQueryLogSink instead.
         */
        @Deprecated
        public Builder setImmediateFlush(final Boolean value) {
            _immediateFlush = value;
            return this;
        }

        private List<Sink> _sinks;

        private String _path = DEFAULT_PATH;
        private String _name = DEFAULT_NAME;
        private String _extension = DEFAULT_EXTENSION;
        private Boolean _immediateFlush = DEFAULT_IMMEDIATE_FLUSH;

        private static final String DEFAULT_PATH = "";
        private static final String DEFAULT_NAME = "query";
        private static final String DEFAULT_EXTENSION = ".log";
        private static final Boolean DEFAULT_IMMEDIATE_FLUSH = Boolean.TRUE;
    }
}
