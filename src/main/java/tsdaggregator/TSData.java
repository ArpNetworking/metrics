package tsdaggregator;

import java.util.*;

import org.joda.time.*;

public class TSData {
	private String _MetricName;
	private Set<TSAggregation> _Aggregations = new HashSet<TSAggregation>();
	
	public TSData(String metricName, Set<AggregationSpecifier> aggregations)
	{
		for (AggregationSpecifier spec : aggregations) {
			_Aggregations.add(new TSAggregation(spec));
		}
	}
	
	public void addMetric(Double data, DateTime time) {
		for (TSAggregation aggregation : _Aggregations) {
			aggregation.addSample(data, time);
		}
	}
	
	public String getMetricName()
	{
		return _MetricName;
	}
}
