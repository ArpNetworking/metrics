package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Class representing a metric and a set of aggregations.
 */
public class TSData {
    private final Set<TSAggregation> _aggregations = Sets.newHashSet();

    public TSData(@Nonnull String metricName, @Nonnull Set<Period> aggregations, @Nonnull AggregationPublisher listener,
                  @Nonnull String hostName, @Nonnull String serviceName, @Nonnull Set<Statistic> statistics) {
        for (@Nonnull Period period : aggregations) {
            _aggregations.add(new TSAggregation(metricName, period, listener, hostName, serviceName, statistics));
        }
    }

    public void addMetric(@Nonnull ArrayList<Double> data, @Nonnull DateTime time) {
        for (@Nonnull TSAggregation aggregation : _aggregations) {
            for (Double val : data) {
                aggregation.addSample(val, time);
            }
        }
    }

    public void checkRotate(double rotateFactor) {
        for (@Nonnull TSAggregation agg : _aggregations) {
            agg.checkRotate(rotateFactor);
        }
    }

    public void close() {
        for (@Nonnull TSAggregation aggregation : _aggregations) {
            aggregation.close();
        }
    }
}
