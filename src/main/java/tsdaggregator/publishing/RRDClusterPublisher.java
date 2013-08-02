package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 2:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class RRDClusterPublisher implements AggregationPublisher {
    HashMap<String, RRDSinglePublisher> _Listeners = new HashMap<String, RRDSinglePublisher>();
    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (AggregatedData d : data) {
            String rrdName = d.getHost() + "." + d.getMetric() + "." + d.getPeriod().toString() + d.getStatistic().getName() + ".rrd";
            if (!_Listeners.containsKey(rrdName)) {
                _Listeners.put(rrdName, new RRDSinglePublisher(d));
            }
            RRDSinglePublisher listener = _Listeners.get(rrdName);
            listener.storeData(d);
        }
    }



    @Override
    public void close() {
    }
}
