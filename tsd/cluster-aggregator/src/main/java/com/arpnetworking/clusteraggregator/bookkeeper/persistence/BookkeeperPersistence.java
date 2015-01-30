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

package com.arpnetworking.clusteraggregator.bookkeeper.persistence;

import com.arpnetworking.clusteraggregator.models.BookkeeperData;
import com.arpnetworking.tsdcore.model.AggregatedData;
import scala.concurrent.Future;

/**
 * Contains methods to store bookkeeping information in a persistent store.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface BookkeeperPersistence {
    /**
     * Stores metric metadata into the persistent store.
     *
     * @param data The metric to store.
     */
    void insertMetric(final AggregatedData data);

    /**
     * Gets the reporting relevant statistics about metrics metadata from the store.
     *
     * @return The bookkeeping data for the metrics cluster.
     */
    Future<BookkeeperData> getBookkeeperData();
}
