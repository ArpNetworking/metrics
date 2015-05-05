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
import java.util.UUID;

/**
 * Interface for repository of alerts.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface AlertRepository {

    /**
     * Open the <code>AlertRepository</code>.
     */
    void open();

    /**
     * Close the <code>AlertRepository</code>.
     */
    void close();

    /**
     * Get the <code>Alert</code> by identifier.
     *
     * @param identifier The <code>Alert</code> identifier.
     * @return The matching <code>Alert</code> if found or <code>Optional.empty()</code>.
     */
    Optional<Alert> get(final UUID identifier);

    /**
     * Create a query against the alerts repository.
     *
     * @return Instance of <code>AlertQuery</code>.
     */
    AlertQuery createQuery();

    /**
     * Query alerts.
     *
     * @param query Instance of <code>AlertQuery</code>.
     * @return The <code>Collection</code> of all alerts.
     */
    QueryResult<Alert> query(AlertQuery query);

    /**
     * Retrieve the total number of alerts in the repository.
     *
     * @return The total number of alerts.
     */
    long getAlertCount();
}
