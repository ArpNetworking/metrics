/**
 * Copyright 2014 Brandon Arp
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
package com.arpnetworking.tsdcore.statistics;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for a statistic calculator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface Statistic extends Serializable {

    /**
     * Compute the statistic from the list of values. By default the list of
     * values is not guaranteed to be in any particular order; however,
     * <code>Statistic</code> subclasses may implement the marker interface
     * <code>OrderedStatistic</code> to ensure they are provided with values
     * that are sorted from smallest to largest.
     *
     * TODO(vkoskela): Return primitive double instead [MAI-132]
     *
     * @param values List of input values.
     * @return Computed statistic value.
     */
    double calculate(List<Quantity> values);

    /**
     * Accessor for the name of the statistic.
     *
     * @return The name of the statistic.
     */
    String getName();

    /**
     * Compute the cluster-level statistic from a list of aggregations.
     *
     * @param aggregations Aggregations to combine.
     * @return Computed statistic value.
     */
    Quantity calculateAggregations(List<AggregatedData> aggregations);
}
