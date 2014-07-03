package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.google.common.collect.Maps;

import java.util.HashMap;
import javax.annotation.Nonnull;

/**
 * An rrd publisher that maintains all the rrd database for a cluster.
 *
 * @author barp
 */
public class RRDClusterPublisher implements AggregationPublisher {
    private final HashMap<String, RRDSinglePublisher> _listeners = Maps.newHashMap();

    @Override
    public void recordAggregation(@Nonnull AggregatedData[] data) {
        for (@Nonnull AggregatedData d : data) {
            @Nonnull String rrdName =
                    d.getHost() + "." + d.getMetric() + "." + d.getPeriod().toString() + d.getStatistic().getName() +
                            ".rrd";
            if (!_listeners.containsKey(rrdName)) {
                _listeners.put(rrdName, new RRDSinglePublisher(d));
            }
            RRDSinglePublisher listener = _listeners.get(rrdName);
            listener.storeData(d);
        }
    }


    @Override
    public void close() {
    }
}
