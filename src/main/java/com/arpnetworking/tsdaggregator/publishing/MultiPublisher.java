package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

import java.util.ArrayList;
import javax.annotation.Nonnull;

/**
 * A publisher that wraps multiple others and publishes to all of them.
 *
 * @author barp
 */
public class MultiPublisher implements AggregationPublisher {
    private final ArrayList<AggregationPublisher> _listeners = new ArrayList<>();

    public void addListener(AggregationPublisher listener) {
        _listeners.add(listener);
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (@Nonnull AggregationPublisher listener : _listeners) {
            listener.recordAggregation(data);
        }
    }

    @Override
    public void close() {
        for (@Nonnull AggregationPublisher listener : _listeners) {
            listener.close();
        }
    }
}
