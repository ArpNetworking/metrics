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
package com.arpnetworking.remet.gui.hosts;

import com.arpnetworking.remet.gui.QueryResult;

/**
 * Interface for repository of hosts available to ReMet. The repository is
 * designed around the host name as the primary key.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 * @author Ting Tu (tingtu at groupon dot com)
 */
public interface HostRepository extends AutoCloseable {

    /**
     * Open the <code>HostRepository</code>.
     */
    void open();

    /**
     * Close the <code>HostRepository</code>.
     */
    void close();

    /**
     * Add a new host or update an existing host in the repository.
     *
     * @param host The host to add to the repository.
     */
    void addOrUpdateHost(Host host);

    /**
     * Remove the host by hostname from the repository.
     *
     * @param hostname The hostname of the host to remove.
     */
    void deleteHost(String hostname);

    /**
     * Create a query against the hosts repository.
     *
     * @return Instance of <code>HostQuery</code>.
     */
    HostQuery createQuery();

    /**
     * Query the hosts repository.
     *
     * @param query Instance of <code>HostQuery</code>.
     * @return Instance of <code>HostQueryResult</code>.
     */
    QueryResult<Host> query(final HostQuery query);

    /**
     * Retrieve the total number of hosts in the repository.
     *
     * @return The total number of hosts.
     */
    long getHostCount();

    /**
     * Retrieve the number of hosts with metrics software in the specified
     * state.
     *
     * @param metricsSoftwareState The state to filter on.
     * @return The number of hosts in the specified state.
     */
    long getHostCount(MetricsSoftwareState metricsSoftwareState);
}
