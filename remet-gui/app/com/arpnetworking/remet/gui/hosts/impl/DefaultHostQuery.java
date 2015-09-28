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

import com.arpnetworking.logback.annotations.Loggable;
import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.HostQuery;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.google.common.base.MoreObjects;

import java.util.Optional;

/**
 * Default implementation of <code>HostQuery</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@Loggable
public final class DefaultHostQuery implements HostQuery {

    /**
     * Public constructor.
     *
     * @param repository The <code>HostRepository</code>
     */
    public DefaultHostQuery(final HostRepository repository) {
        _repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery partialHostname(final Optional<String> partialHostname) {
        _partialHostname = partialHostname;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery metricsSoftwareState(final Optional<MetricsSoftwareState> metricsSoftwareState) {
        _metricsSoftwareState = metricsSoftwareState;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery cluster(final Optional<String> cluster) {
        _cluster = cluster;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery limit(final int limit) {
        _limit = limit;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery offset(final Optional<Integer> offset) {
        _offset = offset;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery sortBy(final Optional<Field> sortBy) {
        _sortBy = sortBy;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<Host> execute() {
        return _repository.query(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getPartialHostname() {
        return _partialHostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MetricsSoftwareState> getMetricsSoftwareState() {
        return _metricsSoftwareState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getCluster() {
        return _cluster;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLimit() {
        return _limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getOffset() {
        return _offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Field> getSortBy() {
        return _sortBy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("class", this.getClass())
                .add("Repository", _repository)
                .add("PartialHostname", _partialHostname)
                .add("MetricsSoftwareState", _metricsSoftwareState)
                .add("Limit", _limit)
                .add("Offset", _offset)
                .add("SortBy", _sortBy)
                .toString();
    }


    private final HostRepository _repository;
    private Optional<String> _partialHostname = Optional.empty();
    private Optional<MetricsSoftwareState> _metricsSoftwareState = Optional.empty();
    private Optional<String> _cluster = Optional.empty();
    private int _limit = DEFAULT_LIMIT;
    private Optional<Integer> _offset = Optional.empty();
    private Optional<Field> _sortBy = Optional.empty();

    private static final int DEFAULT_LIMIT = 1000;
}
