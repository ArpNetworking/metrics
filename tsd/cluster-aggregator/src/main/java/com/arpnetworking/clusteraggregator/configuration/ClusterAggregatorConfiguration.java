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

package com.arpnetworking.clusteraggregator.configuration;

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Representation of cluster aggregator configuration.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ClusterAggregatorConfiguration {
    /**
     * Create an {@link com.fasterxml.jackson.databind.ObjectMapper} for cluster aggregator configuration.
     *
     * @return An <code>ObjectMapper</code> for TsdAggregator configuration.
     */
    public static ObjectMapper createObjectMapper() {
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();

        final SimpleModule module = new SimpleModule("ClusterAggregator");
        BuilderDeserializer.addTo(module, ClusterAggregatorConfiguration.class);

        objectMapper.registerModules(module);

        return objectMapper;
    }

    public int getHttpPort() {
        return _httpPort;
    }

    public String getHttpHost() {
        return _httpHost;
    }

    public File getLogDirectory() {
        return _logDirectory;
    }

    public Map<String, ?> getAkkaConfiguration() {
        return Collections.unmodifiableMap(_akkaConfiguration);
    }

    public File getPipelineConfiguration() {
        return _pipelineConfiguration;
    }

    private ClusterAggregatorConfiguration(final Builder builder) {
        _httpHost = builder._httpHost;
        _httpPort = builder._httpPort;
        _logDirectory = builder._logDirectory;
        _akkaConfiguration = Maps.newHashMap(builder._akkaConfiguration);
        _pipelineConfiguration = builder._pipelineConfiguration;
    }

    private final File _logDirectory;
    private final String _httpHost;
    private final int _httpPort;
    private final Map<String, ?> _akkaConfiguration;
    private final File _pipelineConfiguration;

    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();

    /**
     * Implementation of builder pattern for {@link com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration}.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static class Builder extends OvalBuilder<ClusterAggregatorConfiguration> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(ClusterAggregatorConfiguration.class);
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
         * The pipeline configuration file. Cannot be null.
         *
         * @param value The pipeline configuration file.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPipelineConfiguration(final File value) {
            _pipelineConfiguration = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _httpHost;
        @NotNull
        @Range(min = 1, max = 65535)
        private Integer _httpPort;
        @NotNull
        private File _logDirectory;
        @NotNull
        private File _pipelineConfiguration;
        @NotNull
        private Map<String, ?> _akkaConfiguration;
    }
}
