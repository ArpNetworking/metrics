package tsdaggregator;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 2:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class RRDClusterListener implements AggregationListener {
    HashMap<String, RRDSingleListener> _Listeners = new HashMap<String, RRDSingleListener>();
    @Override
    public void recordAggregation(AggregatedData[] data) {
        for (AggregatedData d : data) {
            String rrdName = d.getHost() + "." + d.getMetric() + "." + d.getPeriod().toString() + d.getStatistic().getName() + ".rrd";
            if (!_Listeners.containsKey(rrdName)) {
                _Listeners.put(rrdName, new RRDSingleListener(d));
            }
            RRDSingleListener listener = _Listeners.get(rrdName);
            listener.storeData(d);
        }
    }



    @Override
    public void close() {
    }
}
