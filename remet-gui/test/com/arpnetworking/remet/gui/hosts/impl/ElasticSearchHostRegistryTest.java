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

import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.HostQuery;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Tests for <code>ElasticSearchHostRegistry</code>.
 *
 * @author Ruchita Venugopal (rvenugopal at groupon dot com)
 */
public class ElasticSearchHostRegistryTest {

    @Before
    public void before() {
        _tempDirectory = Files.createTempDir();
        try {
            FileUtils.deleteDirectory(_tempDirectory);
        } catch (final IOException ioe) {
            // Do nothing
        }
        _repository = new ElasticSearchHostRepository(
                ImmutableSettings.settingsBuilder()
                        .put("cluster.name", "ElasticSearchHostRegistryTest")
                        .put("node.local", "true")
                        .put("node.data", "true")
                        .put("path.logs", _tempDirectory.getAbsolutePath() + "/logs")
                        .put("path.data", _tempDirectory.getAbsolutePath() + "/data")
                        .build(),
                ImmutableSettings.settingsBuilder()
                        .put("number_of_shards", "1")
                        .put("number_of_replicas", "0")
                        .put("refresh_interval", "1s")
                        .build());
        _repository.open();
    }

    @After
    public void tearDown() {
        _repository.close();
        try {
            FileUtils.deleteDirectory(_tempDirectory);
        } catch (final IOException ioe) {
            // Do nothing
        }
    }

    @Test
    public void testAddHost() throws InterruptedException {
        final Host expectedHost = addOrUpdateHost("testAddHost-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().partialHostname(Optional.of(expectedHost.getHostname())).execute();
        final Host actualHost = Iterables.getFirst(result.values(), null);
        Assert.assertEquals(expectedHost, actualHost);
        Assert.assertEquals(1, result.total());
    }

    @Test
    public void testUpdateHost() throws InterruptedException {
        addOrUpdateHost("testUpdateHost-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost = addOrUpdateHost("testUpdateHost-host1", MetricsSoftwareState.OLD_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().partialHostname(Optional.of(expectedHost.getHostname())).execute();
        final Host actualHost = Iterables.getFirst(result.values(), null);
        Assert.assertEquals(expectedHost, actualHost);
        Assert.assertEquals(1, result.total());
    }

    @Test
    public void testDeleteHost() throws InterruptedException {
        final Host deletedHost = addOrUpdateHost("testDeleteHost-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        _repository.deleteHost(deletedHost.getHostname());

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().partialHostname(Optional.of(deletedHost.getHostname())).execute();
        Assert.assertTrue(result.values().isEmpty());
        Assert.assertEquals(0, result.total());
    }

    @Test
    public void testFindAllHosts() throws InterruptedException {
        final Host expectedHost1 = addOrUpdateHost("testFindAllHostsA", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost2 = addOrUpdateHost("testFindAllHostsB", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost3 = addOrUpdateHost("testFindAllHostsC", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost4 = addOrUpdateHost("testFindAllHostsD", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().limit(10).execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(4, hosts.size());
        Assert.assertTrue(hosts.contains(expectedHost1));
        Assert.assertTrue(hosts.contains(expectedHost2));
        Assert.assertTrue(hosts.contains(expectedHost3));
        Assert.assertTrue(hosts.contains(expectedHost4));
    }

    @Test
    public void testFindHostsWithName() throws InterruptedException {
        final Host expectedHost = addOrUpdateHost("testFindHostsWithName-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("host-foo", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().partialHostname(Optional.of("testFindHostsWithName-host1")).execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(1, hosts.size());
        Assert.assertEquals(expectedHost, Iterables.getFirst(hosts, null));
    }

    @Test
    public void testFindHostsWithCluster() throws InterruptedException {
        final Host expectedHost = addOrUpdateHost("testFindHostsWithName-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, "cluster1");
        addOrUpdateHost("host-foo", MetricsSoftwareState.LATEST_VERSION_INSTALLED, "cluster2");

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery().cluster(Optional.of("cluster1")).execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(1, hosts.size());
        Assert.assertEquals(expectedHost, Iterables.getFirst(hosts, null));
    }

    @Test
    public void testFindHostsWithNamePrefix() throws InterruptedException {
        final Host expectedHost1 = addOrUpdateHost("testFindHostsWithNamePrefix-Foo", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost2 = addOrUpdateHost("testFindHostsWithNamePrefix-Bar", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost3 = addOrUpdateHost("hostfoo", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost4 = addOrUpdateHost("hostbar", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result1 = _repository.createQuery().partialHostname(Optional.of("testFindHostsWithNamePrefix")).execute();
        final List<? extends Host> hosts1 = result1.values();
        Assert.assertEquals(2, hosts1.size());
        Assert.assertTrue(hosts1.contains(expectedHost1));
        Assert.assertTrue(hosts1.contains(expectedHost2));

        final QueryResult<Host> result2 = _repository.createQuery().partialHostname(Optional.of("host")).execute();
        final List<? extends Host> hosts2 = result2.values();
        Assert.assertEquals(2, hosts2.size());
        Assert.assertTrue(hosts2.contains(expectedHost3));
        Assert.assertTrue(hosts2.contains(expectedHost4));
    }

    @Test
    public void testFindHostsWithNameAndState() throws InterruptedException {
        addOrUpdateHost("testFindHostsWithNameAndState-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost = addOrUpdateHost("testFindHostsWithNameAndState-host2", MetricsSoftwareState.OLD_VERSION_INSTALLED, null);
        addOrUpdateHost("host-foo", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("host-bar", MetricsSoftwareState.OLD_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery()
                .partialHostname(Optional.of("testFindHostsWithNameAndState"))
                .metricsSoftwareState(Optional.of(MetricsSoftwareState.OLD_VERSION_INSTALLED))
                .execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(1, hosts.size());
        Assert.assertTrue(hosts.contains(expectedHost));
    }

    @Test
    public void testFindHostsWithNameWithLimit() throws InterruptedException {
        addOrUpdateHost("testFindHostsWithNameWithLimit-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("testFindHostsWithNameWithLimit-host2", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("testFindHostsWithNameWithLimit-host3", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("testFindHostsWithNameWithLimit-host4", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery()
                .partialHostname(Optional.of("testFindHostsWithNameWithLimit"))
                .limit(1)
                .execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(1, hosts.size());
    }

    @Test
    public void testFindHostsSortByScoreDefault() throws InterruptedException {
        final Host expectedHost1 = addOrUpdateHost("abc-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost2 = addOrUpdateHost("host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery()
                .partialHostname(Optional.of("host"))
                .execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(2, hosts.size());
        Assert.assertEquals(expectedHost2, hosts.get(0));
        Assert.assertEquals(expectedHost1, hosts.get(1));
    }

    @Test
    public void testFindHostsSortByHostname() throws InterruptedException {
        final Host expectedHost1 = addOrUpdateHost("abc-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost2 = addOrUpdateHost("host-def", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery()
                .partialHostname(Optional.of("host"))
                .sortBy(Optional.of(HostQuery.Field.HOSTNAME))
                .execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(2, hosts.size());
        Assert.assertEquals(expectedHost1, hosts.get(0));
        Assert.assertEquals(expectedHost2, hosts.get(1));
    }

    @Test
    public void testFindHostsOffset() throws InterruptedException {
        addOrUpdateHost("a-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("b-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost1 = addOrUpdateHost("c-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost2 = addOrUpdateHost("d-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        final Host expectedHost3 = addOrUpdateHost("e-host", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        final QueryResult<Host> result = _repository.createQuery()
                .partialHostname(Optional.of("host"))
                .offset(Optional.of(2))
                .sortBy(Optional.of(HostQuery.Field.HOSTNAME))
                .execute();
        final List<? extends Host> hosts = result.values();
        Assert.assertEquals(3, hosts.size());
        Assert.assertEquals(expectedHost1, hosts.get(0));
        Assert.assertEquals(expectedHost2, hosts.get(1));
        Assert.assertEquals(expectedHost3, hosts.get(2));
        Assert.assertEquals(5, result.total());
    }

    @Test
    public void testCountHosts() throws InterruptedException {
        addOrUpdateHost("testCountHosts-host1", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("testCountHosts-host2", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);
        addOrUpdateHost("testCountHosts-host3", MetricsSoftwareState.OLD_VERSION_INSTALLED, null);
        addOrUpdateHost("testCountHosts-host4", MetricsSoftwareState.LATEST_VERSION_INSTALLED, null);

        // Indexing is asynchronous at an interval of 1 second (see @Before)
        Thread.sleep(2000);

        Assert.assertEquals(4, _repository.getHostCount());
        Assert.assertEquals(3, _repository.getHostCount(MetricsSoftwareState.LATEST_VERSION_INSTALLED));
        Assert.assertEquals(1, _repository.getHostCount(MetricsSoftwareState.OLD_VERSION_INSTALLED));
        Assert.assertEquals(0, _repository.getHostCount(MetricsSoftwareState.NOT_INSTALLED));
    }

    private Host addOrUpdateHost(final String name, final MetricsSoftwareState state, final String cluster) {
        final Host host = new DefaultHost.Builder()
                .setHostname(name)
                .setMetricsSoftwareState(state)
                .setCluster(cluster)
                .build();
        _repository.addOrUpdateHost(host);
        return host;
    }

    private ElasticSearchHostRepository _repository;
    private File _tempDirectory;
}
