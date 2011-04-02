package tsdaggregator;

import java.util.*;

import org.joda.time.*;

public class TSData {
	private String _MetricName;
	private Set<TSAggregation> _Aggregations = new HashSet<TSAggregation>();
	
	public TSData(String metricName, Set<Period> aggregations)
	{
		_MetricName = metricName;
		for (Period period : aggregations) {
			_Aggregations.add(new TSAggregation(_MetricName, period, ConsoleListener.getInstance()));
		}
	}
	
	public void addMetric(Double data, DateTime time) {
		for (TSAggregation aggregation : _Aggregations) {
			aggregation.addSample(data, time);
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
