package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

/**
 * A publisher for an upstream tsdaggregator cluster aggregation server.
 *
 * @author barp
 */
public class ClusterAggregationPublisher implements AggregationPublisher {
    private final String _host;
    private final String _cluster;
    private final String _aggHost;
    private final int _aggPort;

    public ClusterAggregationPublisher(String aggregationHost, String host, String cluster) {
        _host = host;
        _cluster = cluster;
        String[] splitHost = aggregationHost.split(":");
        _aggHost = splitHost[0];
        int port = 7065;
        if (splitHost.length > 1) {
            try {
                port = Integer.parseInt(splitHost[1]);
            } catch (NumberFormatException ignored) {
                port = 7065;
            }
        }

        _aggPort = port;
    }

    @Override
    public void recordAggregation(final AggregatedData[] data) {
        for (AggregatedData d : data) {
        }
    }

    @Override
    public void close() {
    }
}
