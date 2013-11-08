package com.arpnetworking.tsdaggregator.aggserver;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

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
        ring.addNode("test.server.com:11820", null);
    }

    @Test
    public void getEntries() {
        KetamaRing ring = new KetamaRing();
        ring.addNode("test.server.com:11820", null);
        ring.addNode("test2.server.com:11820", null);
        ring.addNode("test3.server.com:11820", null);
        ring.addNode("test4.server.com:11820", null);
        ring.addNode("test5.server.com:11820", null);
        ring.addNode("test6.server.com:11820", null);
        final Set<Map.Entry<Integer,KetamaRing.NodeEntry>> ringEntries = ring.getRingEntries();
        for (Map.Entry<Integer, KetamaRing.NodeEntry> entry : ringEntries) {
            System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue().nodeKey);
        }
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
            counts[(int)nodeEntry.mappedObject]++;
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
        double fracDev = stdDev / (samples / buckets);
        Assert.assertThat(fracDev, Matchers.lessThanOrEqualTo(0.1D));
    }
}
