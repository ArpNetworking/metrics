package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Serves as a data class for storing data for aggregated values after computation.
 *
 * @author barp
 */
public final class AggregatedData {
    final Statistic _statistic;
    final String _service;
    final String _host;
    final String _metric;
    final Double _value;
    final DateTime _periodStart;
    final Period _period;

    public AggregatedData(final Statistic statistic, final String service, final String host, final String metric,
                          final Double value, final DateTime periodStart, final Period period) {
        _statistic = statistic;
        _service = service;
        _host = host;
        _metric = metric;
        _value = value;
        _periodStart = periodStart;
        _period = period;
    }

    public Period getPeriod() {
        return _period;
    }

    public Statistic getStatistic() {
        return _statistic;
    }

    public String getService() {
        return _service;
    }

    public String getHost() {
        return _host;
    }

    public Double getValue() {
        return _value;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    public String getMetric() {
        return _metric;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AggregatedData data = (AggregatedData) o;

        if (!_host.equals(data._host)) {
            return false;
        }
        if (!_metric.equals(data._metric)) {
            return false;
        }
        if (!_period.equals(data._period)) {
            return false;
        }
        if (!_periodStart.equals(data._periodStart)) {
            return false;
        }
        if (!_service.equals(data._service)) {
            return false;
        }
        if (!_statistic.equals(data._statistic)) {
            return false;
        }
        if (!_value.equals(data._value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _statistic.hashCode();
        result = 31 * result + _service.hashCode();
        result = 31 * result + _host.hashCode();
        result = 31 * result + _metric.hashCode();
        result = 31 * result + _value.hashCode();
        result = 31 * result + _periodStart.hashCode();
        result = 31 * result + _period.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("Statistic", _statistic)
                .add("Service", _service)
                .add("Host", _host)
                .add("Metric", _metric)
                .add("Value", _value)
                .add("PeriodStart", _periodStart)
                .add("Period", _period)
                .toString();
    }
}
