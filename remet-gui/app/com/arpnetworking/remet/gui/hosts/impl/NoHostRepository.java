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
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a dummy implementation of <code>HostRepository</code>. The use of
 * this repository serves as a marker to disable the host registry feature in
 * the user interface.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class NoHostRepository implements HostRepository {

    /**
     * Public constructor.
     */
    @Inject
    public NoHostRepository() {}

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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHost(final String hostName) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Deleting host")
                .addData("hostname", hostName)
                .log();
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
    public HostQueryResult query(final HostQuery query) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Querying")
                .addData("query", query)
                .log();
        return new DefaultHostQueryResult(Collections.<Host>emptyList(), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHostCount() {
        assertIsOpen();
        LOGGER.debug().setMessage("Getting host count").log();
        return 0;
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
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
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
    private static final Logger LOGGER = LoggerFactory.getLogger(NoHostRepository.class);
}
