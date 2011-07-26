package tsdaggregator;

import java.util.*;

import org.joda.time.*;

public class TSData {
	private String _MetricName;
	private Set<TSAggregation> _Aggregations = new HashSet<TSAggregation>();
	
	public TSData(String metricName, Set<Period> aggregations, AggregationListener listener, String hostName, String serviceName)
	{
		_MetricName = metricName;
		for (Period period : aggregations) {
			_Aggregations.add(new TSAggregation(_MetricName, period, listener, hostName, serviceName));
		}
	}
	
	public void addMetric(ArrayList<Double> data, DateTime time) {
		for (TSAggregation aggregation : _Aggregations) {
                    for (Double val : data) {
			aggregation.addSample(val, time);
                    }
		}
	}
	
	public void close() {
		for (TSAggregation aggregation : _Aggregations) {
			aggregation.close();
		}
	}
	
	public String getMetricName()
	{
		return _MetricName;
	}
}
