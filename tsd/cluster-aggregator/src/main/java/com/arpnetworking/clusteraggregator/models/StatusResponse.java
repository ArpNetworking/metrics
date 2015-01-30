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

package com.arpnetworking.clusteraggregator.models;

import akka.actor.Address;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import com.arpnetworking.utility.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.util.Collections;
import java.util.Map;

/**
 * Response model for the status http endpoint.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class StatusResponse {
    public String getClusterLeader() {
        return _clusterLeader != null ? _clusterLeader.toString() : null;
    }

    public String getLocalAddress() {
        return _localAddress.toString();
    }

    public BookkeeperData getMetrics() {
        return _metrics;
    }

    @JsonProperty("isLeader")
    public boolean isLeader() {
        return _localAddress.equals(_clusterLeader);
    }

    public Iterable<Member> getMembers() {
        return _members;
    }

    public Map<Period, PeriodMetrics> getLocalMetrics() {
        return _localMetrics;
    }

    private StatusResponse(final Builder builder) {
        if (builder._clusterState == null) {
            _clusterLeader = null;
            _members = Collections.emptyList();
        } else {
            _clusterLeader = builder._clusterState.getLeader();
            _members = builder._clusterState.getMembers();
        }

        _localAddress = builder._localAddress;
        _metrics = builder._bookkeeperData;
        _localMetrics = builder._localMetrics;
    }

    private final Address _localAddress;
    private final Address _clusterLeader;
    private final BookkeeperData _metrics;
    private final Iterable<Member> _members;
    private final Map<Period, PeriodMetrics> _localMetrics;

    /**
     * Builder for a {@link StatusResponse}.
     */
    public static class Builder extends OvalBuilder<StatusResponse> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(StatusResponse.class);
        }

        /**
         * Sets the cluster state.
         *
         * @param value The cluster state.
         * @return This builder.
         */
        public Builder setClusterState(final ClusterEvent.CurrentClusterState value) {
            _clusterState = value;
            return this;
        }

        /**
         * Sets the bookkeeper data.
         *
         * @param value The bookkeeper data.
         * @return This builder.
         */
        public Builder setClusterMetrics(final BookkeeperData value) {
            _bookkeeperData = value;
            return this;
        }

        /**
         * Sets the local address of this cluster node.
         *
         * @param value The address of the local node.
         * @return This builder.
         */
        public Builder setLocalAddress(final Address value) {
            _localAddress = value;
            return this;
        }

        /**
         * Sets the local metrics for this cluster node.
         *
         * @param value The local metrics.
         * @return This builder.
         */
        public Builder setLocalMetrics(final Map<Period, PeriodMetrics> value) {
            _localMetrics = value;
            return this;
        }

        private ClusterEvent.CurrentClusterState _clusterState;
        private BookkeeperData _bookkeeperData;
        @NotNull
        private Address _localAddress;
        private Map<Period, PeriodMetrics> _localMetrics;
    }
}
