package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 11:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class MultiPublisher implements AggregationPublisher {
    private ArrayList<AggregationPublisher> _Listeners = new ArrayList<AggregationPublisher>();

    public void addListener(AggregationPublisher listener) {
        _Listeners.add(listener);
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (AggregationPublisher listener : _Listeners) {
            listener.recordAggregation(data);
        }
    }

    @Override
    public void close() {
        for (AggregationPublisher listener : _Listeners) {
            listener.close();
        }
    }
}
