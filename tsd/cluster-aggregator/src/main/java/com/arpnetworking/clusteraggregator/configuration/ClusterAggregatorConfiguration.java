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

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import org.joda.time.Period;

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

    public String getMonitoringCluster() {
        return _monitoringCluster;
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

    public Period getMaxConnectionTimeout() {
        return _maxConnectionTimeout;
    }

    public Period getMinConnectionTimeout() {
        return _minConnectionTimeout;
    }

    public Period getJvmMetricsCollectionInterval() {
        return _jvmMetricsCollectionInterval;
    }

    public Map<String, ?> getAkkaConfiguration() {
        return Collections.unmodifiableMap(_akkaConfiguration);
    }

    public File getHostPipelineConfiguration() {
        return _hostPipelineConfiguration;
    }

    public File getClusterPipelineConfiguration() {
        return _clusterPipelineConfiguration;
    }

    public RebalanceConfiguration getRebalanceConfiguration() {
        return _rebalanceConfiguration;
    }

    public Map<String, DatabaseConfiguration> getDatabaseConfigurations() {
        return _databaseConfigurations;
    }

    public int getAggregationPort() {
        return _aggregationPort;
    }

    public String getAggregationHost() {
        return _aggregationHost;
    }

    public String getClusterHostSuffix() {
        return _clusterHostSuffix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .toString();
    }

    private ClusterAggregatorConfiguration(final Builder builder) {
        _monitoringCluster = builder._monitoringCluster;
        _httpHost = builder._httpHost;
        _httpPort = builder._httpPort;
        _aggregationHost = builder._aggregationHost;
        _aggregationPort = builder._aggregationPort;
        _logDirectory = builder._logDirectory;
        _akkaConfiguration = Maps.newHashMap(builder._akkaConfiguration);
        _hostPipelineConfiguration = builder._hostPipelineConfiguration;
        _clusterPipelineConfiguration = builder._clusterPipelineConfiguration;
        _minConnectionTimeout = builder._minConnectionTimeout;
        _maxConnectionTimeout = builder._maxConnectionTimeout;
        _jvmMetricsCollectionInterval = builder._jvmMetricsCollectionInterval;
        _rebalanceConfiguration = builder._rebalanceConfiguration;
        _clusterHostSuffix = builder._clusterHostSuffix;
        _databaseConfigurations = Maps.newHashMap(builder._databaseConfigurations);
    }

    private final String _monitoringCluster;
    private final File _logDirectory;
    private final String _httpHost;
    private final int _httpPort;
    private final String _aggregationHost;
    private final int _aggregationPort;
    private final Map<String, ?> _akkaConfiguration;
    private final File _clusterPipelineConfiguration;
    private final File _hostPipelineConfiguration;
    private final Period _minConnectionTimeout;
    private final Period _maxConnectionTimeout;
    private final Period _jvmMetricsCollectionInterval;
    private final RebalanceConfiguration _rebalanceConfiguration;
    private final String _clusterHostSuffix;
    private final Map<String, DatabaseConfiguration> _databaseConfigurations;

    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();

    /**
     * Implementation of builder pattern for {@link com.arpnetworking.clusteraggregator.configuration.ClusterAggregatorConfiguration}.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<ClusterAggregatorConfiguration> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(ClusterAggregatorConfiguration.class);
        }

        /**
         * The monitoring cluster. Cannot be null or empty.
         *
         * @param value The monitoring cluster.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMonitoringCluster(final String value) {
            _monitoringCluster = value;
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
         * The suffix to append to the cluster host when reporting metrics. Optional.
         * Cannot be null.  Default is the empty string.
         *
         * @param value The host suffix to append.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setClusterHostSuffix(final String value) {
            _clusterHostSuffix = value;
            return this;
        }

        /**
         * The aggregation server host address to bind to. Cannot be null or empty.
         *
         * @param value The host address to bind to.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAggregationHost(final String value) {
            _aggregationHost = value;
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
         * The http port to listen on. Cannot be null, must be between 1 and
         * 65535 (inclusive). Defaults to 7065.
         *
         * @param value The port to listen on.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAggregationPort(final Integer value) {
            _aggregationPort = value;
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
         * The minimum connection cycling time for a client.  Required.  Cannot be null.
         *
         * @param value The minimum time before cycling a connection.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMinConnectionTimeout(final Period value) {
            _minConnectionTimeout = value;
            return this;
        }

        /**
         * The maximum connection cycling time for a client.  Required.  Cannot be null.
         *
         * @param value The maximum time before cycling a connection.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaxConnectionTimeout(final Period value) {
            _maxConnectionTimeout = value;
            return this;
        }

        /**
         * Period for collecting JVM metrics.
         *
         * @param value A <code>Period</code> value.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setJvmMetricsCollectionInterval(final Period value) {
            _jvmMetricsCollectionInterval = value;
            return this;
        }

        /**
         * The cluster pipeline configuration file. Cannot be null.
         *
         * @param value The cluster pipeline configuration file.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setClusterPipelineConfiguration(final File value) {
            _clusterPipelineConfiguration = value;
            return this;
        }

        /**
         * The host pipeline configuration file. Cannot be null.
         *
         * @param value The host pipeline configuration file.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHostPipelineConfiguration(final File value) {
            _hostPipelineConfiguration = value;
            return this;
        }

        /**
         * Configuration for the shard rebalance settings.
         *
         * @param value The rebalacing configuration.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setRebalanceConfiguration(final RebalanceConfiguration value) {
            _rebalanceConfiguration = value;
            return this;
        }

        /**
         * Configuration for the databases.
         *
         * @param value The database configurations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setDatabaseConfigurations(final Map<String, DatabaseConfiguration> value) {
            _databaseConfigurations = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _monitoringCluster;
        @NotNull
        @NotEmpty
        private String _httpHost = "0.0.0.0";
        @NotNull
        @Range(min = 1, max = 65535)
        private Integer _httpPort = 7066;
        @NotNull
        @NotEmpty
        private String _aggregationHost = "0.0.0.0";
        @NotNull
        @Range(min = 1, max = 65535)
        private Integer _aggregationPort = 7065;
        @NotNull
        private File _logDirectory;
        @NotNull
        private File _clusterPipelineConfiguration;
        @NotNull
        private File _hostPipelineConfiguration;
        @NotNull
        private Map<String, ?> _akkaConfiguration;
        @NotNull
        private Period _maxConnectionTimeout;
        @NotNull
        private Period _minConnectionTimeout;
        @NotNull
        private Period _jvmMetricsCollectionInterval;
        @NotNull
        private RebalanceConfiguration _rebalanceConfiguration;
        @NotNull
        private String _clusterHostSuffix = "";
        private Map<String, DatabaseConfiguration> _databaseConfigurations;
    }
}
