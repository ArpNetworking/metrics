package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

import java.util.ArrayList;

/**
 * A publisher that wraps and buffers another.
 *
 * @author barp
 */
public class BufferingPublisher implements AggregationPublisher {
    private final ArrayList<AggregatedData> _data = new ArrayList<AggregatedData>();
    private final AggregationPublisher _wrapped;
    private final int _buffer;

    public BufferingPublisher(AggregationPublisher wrapped) {
        this(wrapped, 15);
    }

    public BufferingPublisher(AggregationPublisher wrapped, int buffer) {
        _buffer = buffer;
        _wrapped = wrapped;
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (AggregatedData d : data) {
            _data.add(d);
        }
        if (_data.size() >= _buffer) {
            emitStats();
        }
    }

    private void emitStats() {
        _wrapped.recordAggregation(_data.toArray(new AggregatedData[0]));
        _data.clear();
    }

    @Override
    public void close() {
        emitStats();
        _wrapped.close();
    }
}
