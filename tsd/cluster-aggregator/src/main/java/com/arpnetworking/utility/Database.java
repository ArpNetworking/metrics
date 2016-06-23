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
package com.arpnetworking.utility;

import com.arpnetworking.clusteraggregator.configuration.DatabaseConfiguration;
import com.arpnetworking.logback.annotations.Loggable;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.util.Set;
import javax.persistence.Entity;
import javax.sql.DataSource;

/**
 * Database instance abstraction across database technologies: HikariCP, Flyway and EBean.
 *
 * NOTE: The current configuration process of mapping to classes and then instantiating components from "safe"
 * configuration is a complete falacy. Only by attempting to apply (e.g. restart) the system with the new configuration
 * can you determine if it is truly safe. Once rewritten that way configuration classes can be eliminated in many cases
 * and the raw Configuration object mapped directly to the consumer of that configuration. For example, in this class we
 * could read a subkey for Hikari thus decoupling our configuration from Hikari's. The same can be done for Flyway by
 * automatically converting a subkey into a Properties instance and giving it to Flyway.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@Loggable
public class Database implements Launchable {

    /**
     * Public constructor.
     *
     * @param name The name of the database instance.
     * @param configuration Configuration for the database instance.
     */
    public Database(final String name, final DatabaseConfiguration configuration) {
        _name = name;
        _configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch() {
        _dataSource = createDataSource();
        _flyway = createFlyway();
        _ebeanServer = createEbeanServer();

        if (_flyway.isPresent()) {
            _flyway.get().migrate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        _ebeanServer.shutdown(false, false);
        _dataSource.close();
    }

    public String getName() {
        return _name;
    }

    public DataSource getDataSource() {
        return _dataSource;
    }

    public EbeanServer getEbeanServer() {
        return _ebeanServer;
    }

    public Optional<Flyway> getFlyway() {
        return _flyway;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Name", _name)
                .add("DataSource", _dataSource)
                .add("EbeanServer", _ebeanServer)
                .add("Flyway", _flyway)
                .toString();
    }

    private HikariDataSource createDataSource() {
        final HikariConfig hikariConfiguration = new HikariConfig();
        hikariConfiguration.setPoolName(_name);
        hikariConfiguration.setJdbcUrl(_configuration.getJdbcUrl());
        hikariConfiguration.setDriverClassName(_configuration.getDriverName());
        hikariConfiguration.setUsername(_configuration.getUsername());
        hikariConfiguration.setPassword(_configuration.getPassword());
        hikariConfiguration.setMaximumPoolSize(_configuration.getMaximumPoolSize());
        hikariConfiguration.setMinimumIdle(_configuration.getMinimumIdle());
        hikariConfiguration.setIdleTimeout(_configuration.getIdleTimeout());
        hikariConfiguration.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(hikariConfiguration);
    }

    private Optional<Flyway> createFlyway() {
        Flyway flyway = null;
        if (!_configuration.getMigrationLocations().isEmpty()) {
            flyway = new Flyway();
            flyway.setLocations(_configuration.getMigrationLocations().toArray(new String[_configuration.getMigrationLocations().size()]));
            flyway.setSchemas(_configuration.getMigrationSchemas().toArray(new String[_configuration.getMigrationSchemas().size()]));
            flyway.setDataSource(_dataSource);
        }
        return Optional.fromNullable(flyway);
    }

    private EbeanServer createEbeanServer() {
        final ServerConfig ebeanConfiguration = new ServerConfig();
        ebeanConfiguration.setDefaultServer("default".equalsIgnoreCase(_name));
        ebeanConfiguration.setDdlGenerate(false);
        ebeanConfiguration.setDdlRun(false);
        ebeanConfiguration.setName(_name);
        ebeanConfiguration.setDataSource(_dataSource);
        final Set<Class<?>> entityClasses = ReflectionsDatabase.newInstance().findClassesWithAnnotation(Entity.class);
        for (final String modelPackage : _configuration.getModelPackages()) {
            final String safePackage = modelPackage.endsWith(".*") ? modelPackage.substring(0, modelPackage.length() - 2) : modelPackage;
            ebeanConfiguration.addPackage(modelPackage);
            entityClasses.stream().filter(c -> c.getPackage().getName().startsWith(safePackage)).forEach(ebeanConfiguration::addClass);
        }

        return EbeanServerFactory.create(ebeanConfiguration);
    }

    private final String _name;
    private final DatabaseConfiguration _configuration;
    private HikariDataSource _dataSource;
    private EbeanServer _ebeanServer;
    private Optional<Flyway> _flyway;
}
