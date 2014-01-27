package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Objects;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents what we know about a metric.
 *
 * @author barp
 */
public class Metric implements Comparable<Metric> {
    private String _cluster;
    private String _service;
    private final String _name;

    private final ConcurrentSkipListSet<String> _periods = new ConcurrentSkipListSet<String>();
    private final ConcurrentSkipListSet<String> _aggregations = new ConcurrentSkipListSet<String>();

    public Metric(String cluster, String service, String name) {
        _cluster = cluster;
        _service = service;
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public String getCluster() {
        return _cluster;
    }

    public String getService() {
        return _service;
    }

    public boolean addPeriod(String period) {
        return _periods.add(period);
    }

    public int addPeriods(Iterable<String> periods) {
        int ret = 0;
        for (String period : periods) {
            ret = _periods.add(period) ? ret + 1 : ret;
        }
        return ret;
    }

    public boolean addAggregation(String aggregation) {
        return _aggregations.add(aggregation);
    }

    public int addAggregations(Iterable<String> aggregations) {
        int ret = 0;
        for (String agg : aggregations) {
            ret = _aggregations.add(agg) ? ret + 1 : ret;
        }
        return ret;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("_cluster", _cluster).add("_name", _name).add("_periods", _periods)
                .add("_aggregations", _aggregations).toString();
    }

    public boolean hasAggregation(final String statistic) {
        return _aggregations.contains(statistic);
    }

    public boolean hasPeriod(final String period) {
        return _periods.contains(period);
    }

    @Override
    public int compareTo(final Metric o) {
        int comp = _cluster.compareTo(o._cluster);
        if (comp == 0) {
            comp = _service.compareTo(o._service);
        }
        if (comp == 0) {
            comp = _name.compareTo(o._name);
        }
        return comp;
    }
}
