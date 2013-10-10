package com.arpnetworking.tsdaggregator;

import java.util.ArrayList;

/**
 * A variable and data to describe the input to a statistic calculator.
 *
 * @author barp
 */
public class CounterVariable {
    /**
     * The kind of metric recorded.
     *
     * @author barp
     */
    public enum MetricKind {
        Counter,
        Timer,
        Gauge
    }

    private MetricKind _metricKind = MetricKind.Counter;
    private ArrayList<Double> _values;

    public CounterVariable(MetricKind kind, ArrayList<Double> values) {
        this._values = values;
        this._metricKind = kind;
    }

    public MetricKind getMetricKind() {
        return _metricKind;
    }

    public ArrayList<Double> getValues() {
        return _values;
    }
}
