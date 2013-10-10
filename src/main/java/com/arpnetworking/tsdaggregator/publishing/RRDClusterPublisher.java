package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

import java.util.HashMap;
import javax.annotation.Nonnull;

/**
 * An rrd publisher that maintains all the rrd database for a cluster.
 *
 * @author barp
 */
public class RRDClusterPublisher implements AggregationPublisher {
    private final HashMap<String, RRDSinglePublisher> _listeners = new HashMap<>();

    @Override
    public void recordAggregation(@Nonnull AggregatedData[] data) {
        for (AggregatedData d : data) {
            String rrdName =
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
