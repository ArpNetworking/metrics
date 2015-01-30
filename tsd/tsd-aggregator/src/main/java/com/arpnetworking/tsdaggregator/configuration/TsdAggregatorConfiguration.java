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
package com.arpnetworking.tsdaggregator.configuration;

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.tsdcore.limiter.MetricsLimiter;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.MoreObjects;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Representation of TsdAggregator configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TsdAggregatorConfiguration {

    /**
     * Create an <code>ObjectMapper</code> for TsdAggregator configuration.
     *
     * @return An <code>ObjectMapper</code> for TsdAggregator configuration.
     */
    public static ObjectMapper createObjectMapper() {
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();

        final SimpleModule module = new SimpleModule("TsdAggregator");
        BuilderDeserializer.addTo(module, TsdAggregatorConfiguration.class);

        final Set<Class<? extends MetricsLimiter>> limiterClasses = INTERFACE_DATABASE.findClassesWithInterface(MetricsLimiter.class);
        for (final Class<? extends MetricsLimiter> limiterClass : limiterClasses) {
            BuilderDeserializer.addTo(module, limiterClass);
        }

        objectMapper.registerModules(module);

        return objectMapper;
    }

    public File getLogDirectory() {
        return _logDirectory;
    }

    public File getPipelinesDirectory() {
        return _pipelinesDirectory;
    }

    public String getHttpHost() {
        return _httpHost;
    }

    public int getHttpPort() {
        return _httpPort;
    }

    public Map<String, MetricsLimiter> getLimiters() {
        return Collections.unmodifiableMap(_limiters);
    }

    public Map<String, ?> getAkkaConfiguration() {
        return Collections.unmodifiableMap(_akkaConfiguration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("LogDirectory", _logDirectory)
                .add("PipelinesDirectory", _pipelinesDirectory)
                .add("HttpHost", _httpHost)
                .add("HttpPort", _httpPort)
                .add("Limiters", _limiters)
                .add("AkkaConfiguration", _akkaConfiguration)
                .toString();
    }

    private TsdAggregatorConfiguration(final Builder builder) {
        _logDirectory = builder._logDirectory;
        _pipelinesDirectory = builder._pipelinesDirectory;
        _httpHost = builder._httpHost;
        _httpPort = builder._httpPort.intValue();
        _limiters = builder._limiters;
        _akkaConfiguration = builder._akkaConfiguration;
    }

    private final File _logDirectory;
    private final File _pipelinesDirectory;
    private final String _httpHost;
    private final int _httpPort;
    private final Map<String, MetricsLimiter> _limiters;
    private final Map<String, ?> _akkaConfiguration;

    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();

    /**
     * Implementation of builder pattern for <code>TsdAggregatorConfiguration</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends OvalBuilder<TsdAggregatorConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(TsdAggregatorConfiguration.class);
        }

        /**
         * The log directory. Cannot be null.
         *
         * @param value The log directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setLogDirectory(final File value) {
            _logDirectory = value;
            return this;
        }

        /**
         * The pipelines directory. Cannot be null.
         *
         * @param value The pipelines directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPipelinesDirectory(final File value) {
            _pipelinesDirectory = value;
            return this;
        }

        /**
         * The http host address to bind to. Cannot be null or empty.
         *
         * @param value The host address to bind to.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHttpHost(final String value) {
            _httpHost = value;
            return this;
        }

        /**
         * The http port to listen on. Cannot be null, must be between 1 and
         * 65535 (inclusive).
         *
         * @param value The port to listen on.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHttpPort(final Integer value) {
            _httpPort = value;
            return this;
        }

        /**
         * The named limiters. Optional. Defaults to an empty map.
         *
         * @param value The port to listen on.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setLimiters(final Map<String, MetricsLimiter> value) {
            _limiters = value;
            return this;
        }

        /**
         * Akka configuration. Cannot be null. By convention Akka configuration
         * begins with a map containing a single key "akka" and a value of a
         * nested map. For more information please see:
         *
         * http://doc.akka.io/docs/akka/snapshot/general/configuration.html
         *
         * NOTE: No validation is performed on the Akka configuration itself.
         *
         * @param value The Akka configuration.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAkkaConfiguration(final Map<String, ?> value) {
            _akkaConfiguration = value;
            return this;
        }

        @NotNull
        private File _logDirectory;
        @NotNull
        private File _pipelinesDirectory;
        @NotNull
        @NotEmpty
        private String _httpHost;
        @NotNull
        @Range(min = 1, max = 65535)
        private Integer _httpPort;
        @NotNull
        private Map<String, MetricsLimiter> _limiters = Collections.emptyMap();
        @NotNull
        private Map<String, ?> _akkaConfiguration;
    }
}
