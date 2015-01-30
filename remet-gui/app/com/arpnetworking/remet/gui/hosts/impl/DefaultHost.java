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
    public String getHostName() {
        return _hostName;
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
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof DefaultHost)) {
            return false;
        }

        final DefaultHost otherHost = (DefaultHost) other;
        return Objects.equal(_hostName, otherHost._hostName)
                && Objects.equal(_metricsSoftwareState, otherHost._metricsSoftwareState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(_hostName, _metricsSoftwareState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("HostName", _hostName)
                .add("MetricsSoftwareState", _metricsSoftwareState)
                .toString();
    }

    private DefaultHost(final Builder builder) {
        _hostName = builder._hostName;
        _metricsSoftwareState = builder._metricsSoftwareState;
    }

    private final String _hostName;
    private final MetricsSoftwareState _metricsSoftwareState;

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
        public Builder setHostName(final String value) {
            _hostName = value;
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
        private String _hostName;
        @NotNull
        private MetricsSoftwareState _metricsSoftwareState;
    }
}
