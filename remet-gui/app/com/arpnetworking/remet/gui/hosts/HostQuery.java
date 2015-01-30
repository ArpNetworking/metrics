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

import com.google.common.base.Optional;

/**
 * Interface describing a query to the <code>HostRepository</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface HostQuery {

    /**
     * Set the host name to query for. Optional. Defaults to all hosts. If this field is not set it is strongly
     * encouraged that the <code>limit</code> field is set.
     *
     * @param partialHostName The partial or complete host name to match.
     * @return This instance of <code>HostQuery</code>.
     */
    HostQuery hostName(final Optional<String> partialHostName);

    /**
     * Set the metrics software state to query for. Optional. Defaults to any state.
     *
     * @param metricsSoftwareState The metrics software state to match.
     * @return This instance of <code>HostQuery</code>.
     */
    HostQuery metricsSoftwareState(final Optional<MetricsSoftwareState> metricsSoftwareState);

    /**
     * The maximum number of hosts to return. Optional. Default is not set (e.g. unbounded).
     *
     * @param limit The maximum number of hosts to return.
     * @return This instance of <code>HostQuery</code>.
     */
    HostQuery limit(final Optional<Integer> limit);

    /**
     * The offset into the result set. Optional. Default is not set.
     *
     * @param offset The offset into the result set.
     * @return This instance of <code>HostQuery</code>.
     */
    HostQuery offset(final Optional<Integer> offset);

    /**
     * Sort the results by the specified field. Optional. Default sorting is defined by the underlying repository
     * implementation but it is strongly recommended that the repository make some attempt to sort by score or relevance
     * given the inputs.
     *
     * @param field The <code>Field</code> to sort on.
     * @return This instance of <code>HostQuery</code>.
     */
    HostQuery sortBy(final Optional<Field> field);

    /**
     * Execute the query and return the results.
     *
     * @return The results of the query as an <code>HostQueryResult</code> instance.
     */
    HostQueryResult execute();

    /**
     * Accessor for the host name.
     *
     * @return The host name.
     */
    Optional<String> getHostName();

    /**
     * Accessor for the metrics software state.
     *
     * @return The metrics software state.
     */
    Optional<MetricsSoftwareState> getMetricsSoftwareState();

    /**
     * Accessor for the limit.
     *
     * @return The limit.
     */
    Optional<Integer> getLimit();

    /**
     * Accessor for the offset.
     *
     * @return The offset.
     */
    Optional<Integer> getOffset();

    /**
     * Accessor for the field to sort by.
     * @return The field to sort by.
     */
    Optional<Field> getSortBy();

    /**
     * The fields defined for a host.
     */
    public enum Field {
        /**
         * The hostname.
         */
        HOST_NAME,
        /**
         * The state of the metrics software.
         */
        METRICS_SOFTWARE_STATE;
    }
}
