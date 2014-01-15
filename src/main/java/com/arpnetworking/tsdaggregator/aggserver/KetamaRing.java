package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import net.jpountz.xxhash.XXHash32;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of a Ketama hash ring for use in mapping metrics to authoritative partition groups.
 *
 * @author barp
 */
public class KetamaRing {

    public static final String DEFAULT_LAYER = "default";

    /**
     * Serves as an entry in the ring.
     */
    public static class NodeEntry implements Comparable<NodeEntry> {
        @Nonnull
        private final String _nodeKey;
        @Nullable
        private final Object _mappedObject;
        private final int _vNodes;
        @Nonnull
        private final String _layer;
        @Nonnull
        private State _status;

        public NodeEntry(@Nonnull final String nodeKey, @Nullable final Object mappedObject,
                         final int vNodes, @Nonnull final String layer, @Nonnull State status) {
            this._nodeKey = nodeKey;
            this._mappedObject = mappedObject;
            this._vNodes = vNodes;
            this._layer = layer;
            this._status = status;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            @Nonnull final NodeEntry nodeEntry = (NodeEntry) o;

            if (!_layer.equals(nodeEntry._layer)) {
                return false;
            }
            if (!_nodeKey.equals(nodeEntry._nodeKey)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = _nodeKey.hashCode();
            result = 31 * result + _layer.hashCode();
            return result;
        }

        @Override
        public int compareTo(@Nonnull final NodeEntry o) {
            if (!_layer.equals(o._layer)) {
                return _layer.compareTo(o._layer);
            } else {
                return _nodeKey.compareTo(o._nodeKey);
            }
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("_nodeKey", _nodeKey)
                    .add("_mappedObject", _mappedObject)
                    .add("_vNodes", _vNodes)
                    .add("_layer", _layer)
                    .toString();
        }

        @Nonnull
        public String getNodeKey() {
            return _nodeKey;
        }

        @Nullable
        @SuppressWarnings("unchecked")
        public <T> T getMappedObject() {
            return (T)_mappedObject;
        }

        public int getvNodes() {
            return _vNodes;
        }

        @Nonnull
        public String getLayer() {
            return _layer;
        }

        @Nonnull
        public State getStatus() {
            return _status;
        }

        private void setStatus(@Nonnull final State status) {
            _status = status;
        }
    }

    final HashMap<Integer, NodeEntry> _nodes = new HashMap<>();
    final ConcurrentHashMap<String, ConcurrentSkipListMap<Integer, NodeEntry>> _ring =
            new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, ConcurrentSkipListSet<NodeEntry>> _ringNodes =
            new ConcurrentHashMap<>();
    final ConcurrentSkipListSet<NodeEntry> _activeNodes = new ConcurrentSkipListSet<>();
    final XXHash32 _hashFunction;

    public KetamaRing() {
        this(net.jpountz.xxhash.XXHashFactory.unsafeInstance().hash32());
    }

    public KetamaRing(XXHash32 hashFunction) {
        _hashFunction = hashFunction;
        initializeRingLayer(DEFAULT_LAYER);
    }

    public void addNode(@Nonnull String nodeKey, Object mappedObject) {
        addNode(nodeKey, mappedObject, DEFAULT_LAYER);
    }

    public void addNode(@Nonnull String nodeKey, Object mappedObject, @Nonnull String layer) {
        addNode(nodeKey, mappedObject, layer, State.Active);
    }

    public void addNode(@Nonnull String nodeKey, Object mappedObject, @Nonnull String layer, @Nonnull State status) {
        addNode(nodeKey, mappedObject, layer, status, 160);
    }

    public void addNode(@Nonnull String nodeKey, Object mappedObject, @Nonnull String layer, @Nonnull State status,
                        int vNodes) {
        byte[] bytes = nodeKey.getBytes(Charsets.UTF_8);
        int seed = 0;
        ConcurrentSkipListMap<Integer, NodeEntry> ringLayer = _ring.get(layer);
        if (ringLayer == null) {
            ringLayer = initializeRingLayer(layer);
        }

        ConcurrentSkipListSet<NodeEntry> ringNodesLayer = _ringNodes.get(layer);

        NodeEntry node = new NodeEntry(nodeKey, mappedObject, vNodes, layer, status);

        //If the node already exists, there's no need to add it to the list
        if (!ringNodesLayer.add(node)) {
            return;
        }

        for (int x = 0; x < vNodes; x++) {
            int hash = _hashFunction.hash(bytes, 0, bytes.length, seed);
            seed = hash;
            if (x == 0) {
                _nodes.put(hash, node);
            }

            boolean set = false;
            while (!set) {
                NodeEntry previous = ringLayer.putIfAbsent(hash, node);
                if (previous != null) {
                    //we have a collision
                    //resolve by picking the node with the highest lexicographical key
                    if (previous._nodeKey.compareTo(node._nodeKey) < 0) {
                        if (ringLayer.replace(hash, previous, node)) {
                            set = true;
                        }
                    }

                } else {
                    set = true;
                }
            }
        }

        if (status.equals(State.Active)) {
            _activeNodes.add(node);
        }
    }

    public void setNodeStatus(String node, State status) {
        setNodeStatus(node, DEFAULT_LAYER, status);
    }

    public void setNodeStatus(String node, String layer, State status) {
        NodeEntry entry = hash(node, layer);
        if (entry.getStatus().equals(State.Active)) {
            _activeNodes.remove(entry);
        }
        entry.setStatus(status);

        if (status.equals(State.Active)) {
            _activeNodes.add(entry);
        }
    }

    public ConcurrentSkipListMap<Integer, NodeEntry> initializeRingLayer(final String layer) {
        ConcurrentSkipListMap<Integer, NodeEntry> ringLayer = new ConcurrentSkipListMap<>();
        ConcurrentSkipListMap<Integer, NodeEntry> previous = _ring.putIfAbsent(layer, ringLayer);
        if (previous != null) {
            ringLayer = previous;
        }

        ConcurrentSkipListSet<NodeEntry> ringNodesLayer = new ConcurrentSkipListSet<>();
        _ringNodes.putIfAbsent(layer, ringNodesLayer);

        return ringLayer;
    }

    public NodeEntry hash(@Nonnull final String key) {
        return hash(key, DEFAULT_LAYER);
    }

    public int getHashCodeFor(@Nonnull final String key) {
        byte[] bytes = key.getBytes(Charsets.UTF_8);
        return _hashFunction.hash(bytes, 0, bytes.length, 0);
    }

    public NodeEntry hash(@Nonnull final String key, @Nonnull final String layer) {
        int hash = getHashCodeFor(key);
        ConcurrentSkipListMap<Integer, NodeEntry> ringLayer = _ring.get(layer);
        if (ringLayer == null) {
            throw new IllegalArgumentException("layer does not exist");
        }
        if (ringLayer.isEmpty()) {
            throw new IllegalStateException("layer is empty");
        }
        Map.Entry<Integer, NodeEntry> entry = ringLayer.ceilingEntry(hash);

        if (entry == null) {
            entry = ringLayer.firstEntry();
        }
        return entry.getValue();

    }

    public List<NodeEntry> hash(@Nonnull final String key, @Nonnull final String layer, final int serverCount) {
        byte[] bytes = key.getBytes(Charsets.UTF_8);
        int hash = _hashFunction.hash(bytes, 0, bytes.length, 0);
        ConcurrentSkipListMap<Integer, NodeEntry> ringLayer = _ring.get(layer);
        if (ringLayer == null) {
            throw new IllegalArgumentException("layer does not exist");
        }
        ConcurrentSkipListSet<NodeEntry> ringNodes = _ringNodes.get(layer).clone();
        ringNodes.retainAll(_activeNodes);
        int actualCount = Math.min(serverCount, ringNodes.size());
        if (actualCount == 0) {
            return Collections.emptyList();
        }
        List<NodeEntry> entries = Lists.newArrayListWithCapacity(actualCount);

        Map.Entry<Integer, NodeEntry> entry = ringLayer.ceilingEntry(hash);
        do {
            if (entry == null) {
                entry = ringLayer.firstEntry();
            }
            if (!entries.contains(entry.getValue()) && ringNodes.contains(entry.getValue())) {
                entries.add(entry.getValue());
            }
            entry = ringLayer.higherEntry(entry.getKey());
        } while (entries.size() < actualCount);
        return entries;
    }

    @Nonnull
    public Set<Map.Entry<Integer, NodeEntry>> getRingEntries() {
        return getRingEntries(DEFAULT_LAYER);
    }

    @Nonnull
    public Set<Map.Entry<Integer, NodeEntry>> getRingEntries(final String layer) {
        return _ring.get(layer).entrySet();
    }

    @Nonnull
    public Set<String> getLayers() {
        return _ring.keySet();
    }
}

