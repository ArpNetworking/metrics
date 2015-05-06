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

import com.arpnetworking.tsdcore.model.Quantity;
import org.joda.time.Period;

import java.util.Map;
import java.util.UUID;

/**
 * Interface describing an alert.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Alert {

    /**
     * The unique identifier of the alert.
     *
     * @return The unique identifier of the alert.
     */
    UUID getId();

    /**
     * The context of the alert. Either a host or cluster.
     *
     * @return The context of the alert.
     */
    Context getContext();

    /**
     * The name of the alert.
     *
     * @return The name of the alert.
     */
    String getName();

    /**
     * The name of the cluster for statistic identifier of condition left-hand side.
     *
     * @return The name of the cluster for statistic identifier of condition left-hand side.
     */
    String getCluster();

    /**
     * The name of the service for statistic identifier of condition left-hand side.
     *
     * @return The name of the service for statistic identifier of condition left-hand side.
     */
    String getService();

    /**
     * The name of the metric for statistic identifier of condition left-hand side.
     *
     * @return The name of the metric for statistic identifier of condition left-hand side.
     */
    String getMetric();

    /**
     * The name of the statistic for statistic identifier of condition left-hand side.
     *
     * @return The name of the statistic for statistic identifier of condition left-hand side.
     */
    String getStatistic();

    /**
     * The period to evaluate the condition in.
     *
     * @return The period to evaluate the condition in.
     */
    Period getPeriod();

    /**
     * The condition operator.
     *
     * @return The condition operator.
     */
    Operator getOperator();

    /**
     * The value of condition right-hand side.
     *
     * @return The value of condition right-hand side.
     */
    Quantity getValue();

    /**
     * Endpoint specific extensions.
     *
     * @return Endpoint specific extensions.
     */
    Map<String, Object> getExtensions();
}
