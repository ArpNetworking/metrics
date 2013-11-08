package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Charsets;
import net.jpountz.xxhash.XXHash32;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Implementation of a Ketama hash ring for use in mapping metrics to authoritative partition groups
 *
 * @author barp
 */
public class KetamaRing {
    public static class NodeEntry {
        public final String nodeKey;
        public final Object mappedObject;
        public final int vNodes;

        public NodeEntry(final String nodeKey, final Object mappedObject, final int vNodes) {
            this.nodeKey = nodeKey;
            this.mappedObject = mappedObject;
            this.vNodes = vNodes;
        }
    }
    final HashMap<Integer, NodeEntry> _nodes = new HashMap<>();
    final ConcurrentSkipListMap<Integer, NodeEntry> _ring = new ConcurrentSkipListMap<>();
    final XXHash32 _hashFunction;

    public KetamaRing() {
        this(net.jpountz.xxhash.XXHashFactory.unsafeInstance().hash32());
    }

    public KetamaRing(XXHash32 hashFunction) {
        _hashFunction = hashFunction;
    }

    public void addNode(String nodeKey, Object mappedObject) {
        addNode(nodeKey, mappedObject, 160);
    }

    public void addNode(String nodeKey, Object mappedObject, int vNodes) {
        byte[] bytes = nodeKey.getBytes(Charsets.UTF_8);
        int seed = 0;
        NodeEntry node = new NodeEntry(nodeKey, mappedObject, vNodes);
        for (int x = 0; x < vNodes; x++) {
            int hash = _hashFunction.hash(bytes, 0, bytes.length, seed);
            seed = hash;
            if (x == 0) {
                _nodes.put(hash, node);
            }

            boolean set = false;
            while (!set) {
                NodeEntry previous = _ring.putIfAbsent(hash, node);
                if (previous != null) {
                    //we have a collision
                    //resolve by picking the node with the highest lexicographical key
                    if (previous.nodeKey.compareTo(node.nodeKey) < 0) {
                        if (_ring.replace(hash, previous, node)) {
                            set = true;
                        }
                    }

                } else {
                    set = true;
                }
            }
        }
    }

    public NodeEntry hash(String key) {
        byte[] bytes = key.getBytes(Charsets.UTF_8);
        Integer hash = _hashFunction.hash(bytes, 0, bytes.length, 0);
        Map.Entry<Integer, NodeEntry> entry = _ring.ceilingEntry(hash);
        if (entry == null) {
            entry = _ring.firstEntry();
        }
        return entry.getValue();
    }

    public Set<Map.Entry<Integer, NodeEntry>> getRingEntries() {
        return _ring.entrySet();
    }
}

