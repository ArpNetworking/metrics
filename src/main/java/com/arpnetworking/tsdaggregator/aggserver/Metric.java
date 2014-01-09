package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Objects;
import org.joda.time.Period;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents what we know about a metric
 *
 * @author barp
 */
public class Metric {
    private String _cluster;
    private final String _name;

    private final ConcurrentSkipListSet<Period> _periods = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<String> _aggregations = new ConcurrentSkipListSet<>();

    public Metric(String cluster, String name) {
        _cluster = cluster;
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public String getCluster() {
        return _cluster;
    }

    public boolean addPeriod(Period period) {
        return _periods.add(period);
    }

    public boolean addAggregation(String aggregation) {
        return _aggregations.add(aggregation);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("_cluster", _cluster).add("_name", _name).add("_periods", _periods)
                .add("_aggregations", _aggregations).toString();
    }

    public boolean hasAggregation(final String statistic) {
        return _aggregations.contains(statistic);
    }

    public boolean hasPeriod(final Period period) {
        return _periods.contains(period);
    }
}
