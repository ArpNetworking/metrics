package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class representing a metric and a set of aggregations.
 */
public class TSData {
    private String _metricName;
    private Set<TSAggregation> _aggregations = new HashSet<TSAggregation>();

    public TSData(String metricName, Set<Period> aggregations, AggregationPublisher listener, String hostName,
                  String serviceName, Set<Statistic> statistics) {
        _metricName = metricName;
        for (Period period : aggregations) {
            _aggregations.add(new TSAggregation(_metricName, period, listener, hostName, serviceName, statistics));
        }
    }

    public void addMetric(ArrayList<Double> data, DateTime time) {
        for (TSAggregation aggregation : _aggregations) {
            for (Double val : data) {
                aggregation.addSample(val, time);
            }
        }
    }

    public void checkRotate() {
        for (TSAggregation agg : _aggregations) {
            agg.checkRotate();
        }
    }

    public void checkRotate(double rotateFactor) {
        for (TSAggregation agg : _aggregations) {
            agg.checkRotate(rotateFactor);
        }
    }

    public void close() {
        for (TSAggregation aggregation : _aggregations) {
            aggregation.close();
        }
    }

    public String getMetricName() {
        return _metricName;
    }
}
