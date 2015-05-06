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
package com.arpnetworking.remet.gui.hosts.impl;

import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Description of a host for ReMet.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DefaultHost implements Host {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostname() {
        return _hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetricsSoftwareState getMetricsSoftwareState() {
        return _metricsSoftwareState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCluster() {
        return _cluster;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof DefaultHost)) {
            return false;
        }

        final DefaultHost otherHost = (DefaultHost) other;
        return Objects.equal(_hostname, otherHost._hostname)
                && Objects.equal(_metricsSoftwareState, otherHost._metricsSoftwareState)
                && Objects.equal(_cluster, otherHost._cluster);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(_hostname, _metricsSoftwareState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Hostname", _hostname)
                .add("MetricsSoftwareState", _metricsSoftwareState)
                .add("Cluster", _cluster)
                .toString();
    }

    private DefaultHost(final Builder builder) {
        _hostname = builder._hostname;
        _metricsSoftwareState = builder._metricsSoftwareState;
        _cluster = builder._cluster;
    }

    private final String _hostname;
    private final MetricsSoftwareState _metricsSoftwareState;
    private final String _cluster;

    /**
     * Implementation of builder pattern for <code>DefaultHost</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends OvalBuilder<Host> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DefaultHost.class);
        }

        /**
         * The hostname. Cannot be null or empty.
         *
         * @param value The hostname.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHostname(final String value) {
            _hostname = value;
            return this;
        }

        /**
         * The cluster. Cannot be null or empty.
         *
         * @param value The cluster.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * The state of the metrics software. Cannot be null.
         *
         * @param value The state of the metrics software.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetricsSoftwareState(final MetricsSoftwareState value) {
            _metricsSoftwareState = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _hostname;
        @NotNull
        private MetricsSoftwareState _metricsSoftwareState;
        @NotEmpty
        private String _cluster;
    }
}
