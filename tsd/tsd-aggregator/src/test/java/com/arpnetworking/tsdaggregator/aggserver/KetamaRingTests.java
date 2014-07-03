package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description goes here
 *
 * @author barp
 */
public class KetamaRingTests {
    @Test
    public void addSingleServer() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
    }

    @Test
    public void getEntries() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
        ring.addNode("test2.server.com:11820", null, "server");
        ring.addNode("test3.server.com:11820", null, "server");
        ring.addNode("test4.server.com:11820", null, "server");
        ring.addNode("test5.server.com:11820", null, "server");
        ring.addNode("test6.server.com:11820", null, "server");
        final Set<Map.Entry<Integer,KetamaRing.NodeEntry>> ringEntries = ring.getRingEntries();
        for (Map.Entry<Integer, KetamaRing.NodeEntry> entry : ringEntries) {
            System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue().getNodeKey());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHashBadLayer() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
        ring.addNode("test2.server.com:11820", null, "server");
        ring.addNode("test3.server.com:11820", null, "server");
        ring.addNode("test4.server.com:11820", null, "server");
        ring.addNode("test5.server.com:11820", null, "server");
        ring.addNode("test6.server.com:11820", null, "server");
        List<KetamaRing.NodeEntry> entries = ring.hash("test", "doesnotexist", 3);
    }

    @Test
    public void getMultipleHashes() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
        ring.addNode("test2.server.com:11820", null, "server");
        ring.addNode("test3.server.com:11820", null, "server");
        ring.addNode("test4.server.com:11820", null, "server");
        ring.addNode("test5.server.com:11820", null, "server");
        ring.addNode("test6.server.com:11820", null, "server");
        List<KetamaRing.NodeEntry> entries = ring.hash("test", "server", 3);
        Assert.assertThat(entries.size(), CoreMatchers.equalTo(3));
        Set<String> hosts = Sets.newHashSet();
        for (KetamaRing.NodeEntry entry : entries) {
            hosts.add(entry.getNodeKey());
        }
        Assert.assertThat(hosts.size(), CoreMatchers.equalTo(3));
    }

    @Test
    public void getMultipleHashesWithNonActive() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
        ring.addNode("test2.server.com:11820", null, "server");
        ring.addNode("test3.server.com:11820", null, "server", State.ShuttingDown);
        ring.addNode("test4.server.com:11820", null, "server", State.ComingOnline);
        ring.addNode("test5.server.com:11820", null, "server", State.Offline);
        ring.addNode("test6.server.com:11820", null, "server");
        List<KetamaRing.NodeEntry> entries = ring.hash("test", "server", 4);
        Assert.assertThat(entries.size(), CoreMatchers.equalTo(3));
        Set<String> hosts = Sets.newHashSet();
        for (KetamaRing.NodeEntry entry : entries) {
            hosts.add(entry.getNodeKey());
        }
        Assert.assertThat(hosts.size(), CoreMatchers.equalTo(3));
        Assert.assertThat(hosts, Matchers.hasItem("test.server.com:11820"));
        Assert.assertThat(hosts, Matchers.hasItem("test2.server.com:11820"));
        Assert.assertThat(hosts, Matchers.hasItem("test6.server.com:11820"));
    }

    @Test
    public void setStatus() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null, "server");
        ring.addNode("test2.server.com:11820", null, "server");
        ring.addNode("test3.server.com:11820", null, "server", State.ShuttingDown);
        ring.addNode("test4.server.com:11820", null, "server", State.ComingOnline);
        ring.addNode("test5.server.com:11820", null, "server");
        ring.addNode("test6.server.com:11820", null, "server");
        ring.setNodeStatus("test5.server.com:11820", "server", State.Offline);
        List<KetamaRing.NodeEntry> entries = ring.hash("test", "server", 4);
        Assert.assertThat(entries.size(), CoreMatchers.equalTo(3));
        Set<String> hosts = Sets.newHashSet();
        for (KetamaRing.NodeEntry entry : entries) {
            hosts.add(entry.getNodeKey());
        }
        Assert.assertThat(hosts.size(), CoreMatchers.equalTo(3));
        Assert.assertThat(hosts, Matchers.hasItem("test.server.com:11820"));
        Assert.assertThat(hosts, Matchers.hasItem("test2.server.com:11820"));
        Assert.assertThat(hosts, Matchers.hasItem("test6.server.com:11820"));
    }

    @Test
    public void testBucketFairness() {
        KetamaRing ring = new KetamaRing();
        int buckets = 8;
        int samples = 10000;
        int[] counts = new int[buckets];
        for (int x = 0; x < buckets; x++) {
            ring.addNode("test" + x + ".server.com:11820", x);
        }

        for (int x = 0; x < samples; x++) {
            final KetamaRing.NodeEntry nodeEntry = ring.hash("sample" + x);
            counts[(Integer)nodeEntry.getMappedObject()]++;
        }

        double mean = 0;
        double o2 = 0;
        for (int x = 0; x < buckets; x++) {
            mean += counts[x];
        }
        mean /= buckets;

        for (int x = 0; x < buckets; x++) {
            o2 += Math.pow(counts[x] - mean, 2);
        }

        double stdDev = Math.sqrt(o2 / buckets);
        double fracDev = stdDev / (samples / (double)buckets);
        Assert.assertThat(fracDev, Matchers.lessThanOrEqualTo(0.1D));
    }
}
