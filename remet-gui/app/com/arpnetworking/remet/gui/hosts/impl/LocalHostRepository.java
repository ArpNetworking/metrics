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

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.HostQuery;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.arpnetworking.remet.gui.impl.DefaultQueryResult;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of <code>HostRepository</code> using a <code>Map</code>. This
 * is <b>not</b> intended for production usage.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class LocalHostRepository implements HostRepository {

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
        LOGGER.debug().setMessage("Opening host repository").log();
        _isOpen.set(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        assertIsOpen();
        LOGGER.debug().setMessage("Closing host repository").log();
        _isOpen.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOrUpdateHost(final Host host) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Adding or updating host")
                .addData("host", host)
                .log();
        _temporaryStorage.put(host.getHostname(), host);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHost(final String hostname) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Deleting host")
                .addData("hostname", hostname)
                .log();
        _temporaryStorage.remove(hostname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostQuery createQuery() {
        assertIsOpen();
        LOGGER.debug().setMessage("Preparing query").log();
        return new DefaultHostQuery(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<Host> query(final HostQuery query) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Querying")
                .addData("query", query)
                .log();

        // Find all matching hosts
        final List<Host> hosts = Lists.newLinkedList();
        for (final Map.Entry<String, Host> entry : _temporaryStorage.entrySet()) {
            boolean matches = true;
            if (query.getPartialHostname().isPresent()) {
                final String queryName = query.getPartialHostname().get().toLowerCase();
                final String hostName = entry.getKey().toLowerCase();
                if (!hostName.equals(queryName) && !hostName.startsWith(queryName) && !hostName.contains(queryName)) {
                    matches = false;
                }
            }
            if (query.getMetricsSoftwareState().isPresent()) {
                final MetricsSoftwareState metricsSoftwareState = entry.getValue().getMetricsSoftwareState();
                if (!query.getMetricsSoftwareState().get().equals(metricsSoftwareState)) {
                    matches = false;
                }
            }
            if (query.getCluster().isPresent()) {
                final String cluster = entry.getValue().getCluster();
                if (!query.getCluster().get().equals(cluster)) {
                    matches = false;
                }
            }
            if (matches) {
                hosts.add(entry.getValue());
            }
        }

        // Apply sorting
        Collections.sort(hosts, new HostComparator(query));

        // Apply pagination
        final long total = hosts.size();
        if (query.getOffset().isPresent()) {
            for (long i = 0; i < query.getOffset().get() && !hosts.isEmpty(); ++i) {
                hosts.remove(0);
            }
        }
        while (hosts.size() > query.getLimit() && !hosts.isEmpty()) {
            hosts.remove(hosts.size() - 1);
        }

        return new DefaultQueryResult<>(hosts, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHostCount() {
        assertIsOpen();
        LOGGER.debug().setMessage("Getting host count").log();
        return _temporaryStorage.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHostCount(final MetricsSoftwareState metricsSoftwareState) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Getting host count in state")
                .addData("state", metricsSoftwareState)
                .log();
        long count = 0;
        for (final Host host : _temporaryStorage.values()) {
            if (!metricsSoftwareState.equals(host.getMetricsSoftwareState())) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("isOpen", _isOpen)
                .put("temporaryStorage", _temporaryStorage)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private void assertIsOpen() {
        assertIsOpen(true);
    }

    private void assertIsOpen(final boolean expectedState) {
        if (_isOpen.get() != expectedState) {
            throw new IllegalStateException(String.format("Host repository is not %s", expectedState ? "open" : "closed"));
        }
    }

    private final AtomicBoolean _isOpen = new AtomicBoolean(false);
    private final Map<String, Host> _temporaryStorage = Maps.newConcurrentMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalHostRepository.class);

    @SuppressFBWarnings("SE_BAD_FIELD")
    private static class HostComparator implements Comparator<Host>, Serializable {

        public HostComparator(final HostQuery query) {
            _query = query;
        }

        @Override
        public int compare(final Host h1, final Host h2) {
            if (_query.getSortBy().isPresent()) {
                if (HostQuery.Field.HOSTNAME.equals(_query.getSortBy().get())) {
                    return String.CASE_INSENSITIVE_ORDER.compare(h1.getHostname(), h2.getHostname());
                } else if (HostQuery.Field.METRICS_SOFTWARE_STATE.equals(_query.getSortBy().get())) {
                    return h1.getMetricsSoftwareState().compareTo(h2.getMetricsSoftwareState());
                } else {
                    LOGGER.warn()
                            .setMessage("Unsupported sort by field")
                            .addData("field", _query.getSortBy().get())
                            .log();
                }
            } else {
                double s1 = 0.0;
                double s2 = 0.0;
                if (_query.getPartialHostname().isPresent()) {
                    // All comparisons are case-insentive
                    final String q = _query.getPartialHostname().get().toLowerCase(Locale.getDefault());
                    final String n1 = h1.getHostname().toLowerCase(Locale.getDefault());
                    final String n2 = h2.getHostname().toLowerCase(Locale.getDefault());

                    // Compute score
                    if (n1.equals(q)) {
                        s1 = 1.0;
                    } else if (n1.startsWith(q)) {
                        s1 = (1000000 - _query.getPartialHostname().get().length()) / 1000000.0;
                    } else if (n1.contains(q)) {
                        s1 = (1000 - _query.getPartialHostname().get().length()) / 1000000.0;
                    }
                    if (n2.equals(q)) {
                        s2 = 1.0;
                    } else if (n2.startsWith(q)) {
                        s2 = (1000000 - _query.getPartialHostname().get().length()) / 1000000.0;
                    } else if (n2.contains(q)) {
                        s2 = (1000 - _query.getPartialHostname().get().length()) / 1000000.0;
                    }
                }

                return Double.compare(s1, s2);
            }
            return 0;
        }

        private final HostQuery _query;

        private static final long serialVersionUID = 1L;
    }
}
