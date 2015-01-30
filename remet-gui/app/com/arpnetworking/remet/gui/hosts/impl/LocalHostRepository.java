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
import com.arpnetworking.remet.gui.hosts.HostQuery;
import com.arpnetworking.remet.gui.hosts.HostQueryResult;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import play.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of <code>HostRepository</code> using a <code>Map</code>. This
 * is <b>not</b> intended for production usage.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class LocalHostRepository implements HostRepository {

    /**
     * Public constructor.
     */
    @Inject
    public LocalHostRepository() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        assertIsOpen(false);
        Logger.debug("Opening host repository");
        _isOpen.set(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        assertIsOpen();
        Logger.debug("Closing host repository");
        _isOpen.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOrUpdateHost(final Host host) {
        assertIsOpen();
        Logger.debug(String.format("Adding or updating host; host=%s", host));
        _temporaryStorage.put(host.getHostName(), host.getMetricsSoftwareState());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHost(final String hostName) {
        assertIsOpen();
        Logger.debug(String.format("Deleting host; hostName=%s", hostName));
        _temporaryStorage.remove(hostName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery createQuery() {
        assertIsOpen();
        Logger.debug("Preparing query");
        return new DefaultHostQuery(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQueryResult query(final HostQuery query) {
        assertIsOpen();
        Logger.debug(String.format("Querying; query=%s", query));

        // Find all matching hosts
        final List<Host> hosts = Lists.newLinkedList();
        for (final Map.Entry<String, MetricsSoftwareState> entry : _temporaryStorage.entrySet()) {
            boolean matches = true;
            if (query.getHostName().isPresent()) {
                final String queryName = query.getHostName().get().toLowerCase();
                final String hostName = entry.getKey().toLowerCase();
                if (!hostName.equals(queryName) && !hostName.startsWith(queryName) && !hostName.contains(queryName)) {
                    matches = false;
                }
            }
            if (query.getMetricsSoftwareState().isPresent()) {
                final MetricsSoftwareState metricsSoftwareState = entry.getValue();
                if (!query.getMetricsSoftwareState().get().equals(metricsSoftwareState)) {
                    matches = false;
                }
            }
            if (matches) {
                hosts.add(new DefaultHost.Builder()
                        .setHostName(entry.getKey())
                        .setMetricsSoftwareState(entry.getValue())
                        .build());
            }
        }

        // Apply sorting
        Collections.sort(hosts, new HostComparator(query));

        // Apply pagination
        final long total = hosts.size();
        if (query.getOffset().isPresent()) {
            for (long i = 0; i < query.getOffset().get().longValue() && !hosts.isEmpty(); ++i) {
                hosts.remove(0);
            }
        }
        if (query.getLimit().isPresent()) {
            while (hosts.size() > query.getLimit().get().longValue() && !hosts.isEmpty()) {
                hosts.remove(hosts.size() - 1);
            }
        }

        return new DefaultHostQueryResult(hosts, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHostCount() {
        assertIsOpen();
        Logger.debug("Getting host count");
        return _temporaryStorage.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHostCount(final MetricsSoftwareState metricsSoftwareState) {
        assertIsOpen();
        Logger.debug(String.format("Getting host count in state; metricsSoftwareState=%s", metricsSoftwareState));
        long count = 0;
        for (final MetricsSoftwareState state : _temporaryStorage.values()) {
            if (!metricsSoftwareState.equals(state)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("TemporaryStorage", _temporaryStorage)
                .toString();
    }

    private void assertIsOpen() {
        assertIsOpen(true);
    }

    private void assertIsOpen(final boolean expectedState) {
        if (_isOpen.get() != expectedState) {
            throw new IllegalStateException(String.format("Host repository is %s", expectedState ? "open" : "closed"));
        }
    }

    private final AtomicBoolean _isOpen = new AtomicBoolean(false);
    private final Map<String, MetricsSoftwareState> _temporaryStorage = Maps.newConcurrentMap();

    private static class HostComparator implements Comparator<Host> {

        public HostComparator(final HostQuery query) {
            _query = query;
        }

        @Override
        public int compare(final Host h1, final Host h2) {
            if (_query.getSortBy().isPresent()) {
                if (HostQuery.Field.HOST_NAME.equals(_query.getSortBy().get())) {
                    return String.CASE_INSENSITIVE_ORDER.compare(h1.getHostName(), h2.getHostName());
                } else if (HostQuery.Field.METRICS_SOFTWARE_STATE.equals(_query.getSortBy().get())) {
                    return h1.getMetricsSoftwareState().compareTo(h2.getMetricsSoftwareState());
                } else {
                    Logger.warn(String.format("Unsupported sort by field; field=%s", _query.getSortBy().get()));
                }
            } else {
                double s1 = 0.0;
                double s2 = 0.0;
                if (_query.getHostName().isPresent()) {
                    // All comparisons are case-insentive
                    final String q = _query.getHostName().get().toLowerCase();
                    final String n1 = h1.getHostName().toLowerCase();
                    final String n2 = h2.getHostName().toLowerCase();

                    // Compute score
                    if (n1.equals(q)) {
                        s1 = 1.0;
                    } else if (n1.startsWith(q)) {
                        s1 = (1000000 - _query.getHostName().get().length()) / 1000000.0;
                    } else if (n1.contains(q)) {
                        s1 = (1000 - _query.getHostName().get().length()) / 1000000.0;
                    }
                    if (n2.equals(q)) {
                        s2 = 1.0;
                    } else if (n2.startsWith(q)) {
                        s2 = (1000000 - _query.getHostName().get().length()) / 1000000.0;
                    } else if (n2.contains(q)) {
                        s2 = (1000 - _query.getHostName().get().length()) / 1000000.0;
                    }
                }

                return Double.valueOf(s1).compareTo(Double.valueOf(s2));
            }
            return 0;
        }

        private final HostQuery _query;
    }
}
