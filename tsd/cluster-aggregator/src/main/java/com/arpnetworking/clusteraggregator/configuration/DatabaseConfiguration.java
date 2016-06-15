/**
 * Copyright 2015 Groupon.com
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
import com.arpnetworking.logback.annotations.Loggable;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the database configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@Loggable
public final class DatabaseConfiguration {

    public String getJdbcUrl() {
        return _jdbcUrl;
    }

    public String getDriverName() {
        return _driverName;
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    public ImmutableList<String> getMigrationLocations() {
        return _migrationLocations;
    }

    public ImmutableList<String> getMigrationSchemas() {
        return _migrationSchemas;
    }

    public int getMaximumPoolSize() {
        return _maximumPoolSize;
    }

    public int getMinimumIdle() {
        return _minimumIdle;
    }

    public int getIdleTimeout() {
        return _idleTimeout;
    }

    public ImmutableList<String> getModelPackages() {
        return _modelPackages;
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

    private DatabaseConfiguration(final Builder builder) {
        _jdbcUrl = builder._jdbcUrl;
        _driverName = builder._driverName;
        _username = builder._username;
        _password = builder._password;
        _migrationLocations = ImmutableList.copyOf(builder._migrationLocations);
        _migrationSchemas = ImmutableList.copyOf(builder._migrationSchemas);
        _maximumPoolSize = builder._maximumPoolSize.intValue();
        _minimumIdle = builder._minimumIdle.intValue();
        _idleTimeout = builder._idleTimeout.intValue();
        _modelPackages = ImmutableList.copyOf(builder._modelPackages);
    }

    private final String _jdbcUrl;
    private final String _driverName;
    private final String _username;
    private final String _password;
    private final ImmutableList<String> _migrationLocations;
    private final ImmutableList<String> _migrationSchemas;
    private final int _maximumPoolSize;
    private final int _minimumIdle;
    private final int _idleTimeout;
    private final ImmutableList<String> _modelPackages;

    /**
     * Implementation of builder pattern for {@link DatabaseConfiguration}.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<DatabaseConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DatabaseConfiguration.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DatabaseConfiguration build() {
            if (_minimumIdle == null) {
                _minimumIdle = _maximumPoolSize;
            }
            return super.build();
        }

        /**
         * Database JDBC url. Required. Cannot be null or empty.
         *
         * @param value JDBC url.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setJdbcUrl(final String value) {
            _jdbcUrl = value;
            return this;
        }

        /**
         * Data driver name. Required. Cannot be null or empty.
         *
         * @param value Driver class name.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setDriverName(final String value) {
            _driverName = value;
            return this;
        }

        /**
         * Database username. Required. Cannot be null or empty.
         *
         * @param value Username.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setUsername(final String value) {
            _username = value;
            return this;
        }

        /**
         * Database password. Required. Cannot be null or empty.
         *
         * @param value Password.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPassword(final String value) {
            _password = value;
            return this;
        }

        /**
         * Migration location(s). Optional. Cannot be null. Default is an empty list. An empty list effectively disables
         * schema migrations.
         *
         * @param value Migration location(s).
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMigrationLocations(final List<String> value) {
            _migrationLocations = value;
            return this;
        }

        /**
         * Migration schema(s). Optional. Cannot be null. Default is an empty list. An empty list defaults to the
         * default schema in the connection.
         *
         *
         * @param value Migration schema(s).
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMigrationSchemas(final List<String> value) {
            _migrationSchemas = value;
            return this;
        }

        /**
         * Maximum pool size. Required. Cannot be null. Must be at least 1.
         *
         * @param value Maximum pool size.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaximumPoolSize(final Integer value) {
            _maximumPoolSize = value;
            return this;
        }

        /**
         * Minimum idle connections. Required. Cannot be null. Must be at least 0.
         *
         * @param value Minimum idle connections.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMinimumIdle(final Integer value) {
            _minimumIdle = value;
            return this;
        }

        /**
         * Idle timeout in milliseconds. Required. Cannot be null. Must be at least 1.
         *
         * @param value Idle timeout in milliseconds.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setIdleTimeout(final Integer value) {
            _idleTimeout = value;
            return this;
        }

        /**
         * Model package(s). Optional. Cannot be null. Default is an empty list.
         *
         * @param value Model package(s).
         * @return This instance of <code>Builder</code>.
         */
        public Builder setModelPackages(final List<String> value) {
            _modelPackages = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _jdbcUrl;
        @NotNull
        @NotEmpty
        private String _driverName;
        @NotNull
        @NotEmpty
        private String _username;
        @NotNull
        @NotEmpty
        private String _password;
        @NotNull
        private List<String> _migrationLocations = Collections.emptyList();
        @NotNull
        private List<String> _migrationSchemas = Collections.emptyList();
        @NotNull
        @Min(1)
        private Integer _maximumPoolSize;
        @NotNull
        @Min(0)
        private Integer _minimumIdle;
        @NotNull
        @Min(0)
        private Integer _idleTimeout;
        @NotNull
        private List<String> _modelPackages = Collections.emptyList();
    }
}
