package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Class representing a metric and a set of aggregations.
 */
public class TSData {
    private final Set<TSAggregation> _aggregations = new HashSet<>();

    public TSData(String metricName, @Nonnull Set<Period> aggregations, AggregationPublisher listener, String hostName,
                  String serviceName, @Nonnull Set<Statistic> statistics) {
        for (Period period : aggregations) {
            _aggregations.add(new TSAggregation(metricName, period, listener, hostName, serviceName, statistics));
        }
    }

    public void addMetric(@Nonnull ArrayList<Double> data, @Nonnull DateTime time) {
        for (TSAggregation aggregation : _aggregations) {
            for (Double val : data) {
                aggregation.addSample(val, time);
            }
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
}
