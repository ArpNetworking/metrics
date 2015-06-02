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
package com.arpnetworking.remet.gui.alerts;

import com.arpnetworking.remet.gui.QueryResult;

import java.util.Optional;

/**
 * Interface describing a query to the <code>AlertRepository</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface AlertQuery {

    /**
     * Set the text to query for. Optional. Defaults to no text.
     *
     * @param contains The text to match.
     * @return This instance of <code>ExpressionQuery</code>.
     */
    AlertQuery contains(final Optional<String> contains);

    /**
     * Set the context to query for. Optional. Defaults to all context types.
     *
     * @param context The context to match.
     * @return This instance of <code>AlertQuery</code>.
     */
    AlertQuery context(final Optional<Context> context);

    /**
     * Set the cluster to query for. Optional. Defaults to all clusters.
     *
     * @param cluster The cluster to match.
     * @return This instance of <code>AlertQuery</code>.
     */
    AlertQuery cluster(final Optional<String> cluster);

    /**
     * Set the service to query for. Optional. Defaults to all services.
     *
     * @param service The service to match.
     * @return This instance of <code>AlertQuery</code>.
     */
    AlertQuery service(final Optional<String> service);

    /**
     * The maximum number of alerts to return.  Optional. Default is 1000.
     *
     * @param limit The maximum number of alerts to return.
     * @return This instance of <code>HostQuery</code>.
     */
    AlertQuery limit(final int limit);

    /**
     * The offset into the result set. Optional. Default is not set.
     *
     * @param offset The offset into the result set.
     * @return This instance of <code>AlertQuery</code>.
     */
    AlertQuery offset(final Optional<Integer> offset);

    /**
     * Execute the query and return the results.
     *
     * @return The results of the query as an {@code QueryResult<Alert>} instance.
     */
    QueryResult<Alert> execute();

    /**
     * Accessor for the contains.
     *
     * @return The contains.
     */
    Optional<String> getContains();

    /**
     * Accessor for the context.
     *
     * @return The context.
     */
    Optional<Context> getContext();

    /**
     * Accessor for the cluster.
     *
     * @return The cluster.
     */
    Optional<String> getCluster();

    /**
     * Accessor for the service.
     *
     * @return The service.
     */
    Optional<String> getService();

    /**
     * Accessor for the limit.
     *
     * @return The limit.
     */
    int getLimit();

    /**
     * Accessor for the offset.
     *
     * @return The offset.
     */
    Optional<Integer> getOffset();
}
