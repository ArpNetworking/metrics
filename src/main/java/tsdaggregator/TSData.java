package tsdaggregator;

import org.joda.time.DateTime;
import org.joda.time.Period;
import tsdaggregator.publishing.AggregationPublisher;
import tsdaggregator.statistics.Statistic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TSData {
	private String _MetricName;
	private Set<TSAggregation> _Aggregations = new HashSet<TSAggregation>();
	
	public TSData(String metricName, Set<Period> aggregations, AggregationPublisher listener, String hostName, String serviceName, Set<Statistic> statistics)
	{
		_MetricName = metricName;
		for (Period period : aggregations) {
			_Aggregations.add(new TSAggregation(_MetricName, period, listener, hostName, serviceName, statistics));
		}
	}
	
	public void addMetric(ArrayList<Double> data, DateTime time) {
		for (TSAggregation aggregation : _Aggregations) {
            for (Double val : data) {
                aggregation.addSample(val, time);
            }
		}
	}

    public void checkRotate() {
        for (TSAggregation agg : _Aggregations) {
            agg.checkRotate();
        }
    }

    public void checkRotate(long rotateMillis) {
        for (TSAggregation agg : _Aggregations) {
            agg.checkRotate(rotateMillis);
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
