package tsdaggregator;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 11:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class CounterVariable {
	public enum MetricKind {
		Counter,
		Timer,
		Gauge
	}

    private MetricKind metricKind = MetricKind.Counter;
    private ArrayList<Double> values;

    public CounterVariable(MetricKind kind, ArrayList<Double> values) {
        this.values = values;
        this.metricKind = kind;
    }

	public MetricKind getMetricKind() {
		return metricKind;
	}

	public ArrayList<Double> getValues() {
        return values;
    }
}
