package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Serves as a data class for storing data for aggregated values after computation.
 *
 * @author barp
 */
public final class AggregatedData {
    private final Statistic _statistic;
    private final String _service;
    private final String _host;
    private final String _metric;
    private final double _value;
    private final DateTime _periodStart;
    private final Period _period;
    @Nonnull
    private final List<Double> _samples;

    public AggregatedData(final Statistic statistic, final String service, final String host, final String metric,
                          final double value, final DateTime periodStart, final Period period, final Double[] samples) {
        _statistic = statistic;
        _service = service;
        _host = host;
        _metric = metric;
        _value = value;
        _periodStart = periodStart;
        _period = period;
        _samples = Arrays.asList(samples);
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

    public double getValue() {
        return _value;
    }

    public DateTime getPeriodStart() {
        return _periodStart;
    }

    public String getMetric() {
        return _metric;
    }

    @Nonnull
    public List<Double> getSamples() {
        return _samples;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        @Nonnull final AggregatedData that = (AggregatedData) o;

        if (Double.compare(that._value, _value) != 0) {
            return false;
        }
        if (!_host.equals(that._host)) {
            return false;
        }
        if (!_metric.equals(that._metric)) {
            return false;
        }
        if (!_period.equals(that._period)) {
            return false;
        }
        if (!_periodStart.equals(that._periodStart)) {
            return false;
        }
        if (!_service.equals(that._service)) {
            return false;
        }
        if (!_statistic.equals(that._statistic)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = _statistic.hashCode();
        result = 31 * result + _service.hashCode();
        result = 31 * result + _host.hashCode();
        result = 31 * result + _metric.hashCode();
        temp = Double.doubleToLongBits(_value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
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
