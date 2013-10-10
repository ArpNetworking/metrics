package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

/**
 * Interface to describe a class that publishes aggregations.
 *
 * @author barp
 */
public interface AggregationPublisher {
    void recordAggregation(AggregatedData[] data);

    void close();
}
