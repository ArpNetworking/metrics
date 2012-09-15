package tsdaggregator;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 11:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class MultiListener implements AggregationListener {
    private ArrayList<AggregationListener> _Listeners = new ArrayList<AggregationListener>();

    public void addListener(AggregationListener listener) {
        _Listeners.add(listener);
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (AggregationListener listener : _Listeners) {
            listener.recordAggregation(data);
        }
    }

    @Override
    public void close() {
        for (AggregationListener listener : _Listeners) {
            listener.close();
        }
    }
}
