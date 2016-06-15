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

import java.util.Collections;
import java.util.UUID;

/**
 * Creates database instances for tests.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TestDatabaseFactory {
    /**
     * Creates a test database.
     *
     * @return a test database
     */
    public Database create() {
        final DatabaseConfiguration dbConfig = new DatabaseConfiguration.Builder()
                .setDriverName("org.h2.Driver")
                .setIdleTimeout(60000)
                .setJdbcUrl("jdbc:h2:mem:dbpartitiontests"
                        + UUID.randomUUID().toString()
                        + ";MODE=PostgreSQL;INIT=create schema if not exists clusteragg;")
                .setMaximumPoolSize(2)
                .setMigrationLocations(Collections.singletonList("db/migration/metrics_clusteragg/common"))
                .setMinimumIdle(0)
                .setModelPackages(Collections.singletonList("com.arpnetworking.clusteraggregator.models.ebean"))
                .setMigrationSchemas(Collections.singletonList("clusteragg"))
                .setUsername("sa")
                .setPassword("sa")
                .build();
        final Database database = new Database("default", dbConfig);
        database.launch();
        return database;
    }
}
