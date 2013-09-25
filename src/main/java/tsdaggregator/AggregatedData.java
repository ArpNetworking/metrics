package tsdaggregator;

import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.Period;
import tsdaggregator.statistics.Statistic;

public final class AggregatedData {
	final Statistic _Statistic;
	final String _Service;
	final String _Host;
	final String _Metric;
	final Double _Value;
	final DateTime _PeriodStart;
	final Period _Period;

    public AggregatedData(final Statistic statistic, final String service, final String host, final String metric, final Double value, final DateTime periodStart, final Period period) {
        _Statistic = statistic;
        _Service = service;
        _Host = host;
        _Metric = metric;
        _Value = value;
        _PeriodStart = periodStart;
        _Period = period;
    }

    public Period getPeriod() {
		return _Period;
	}

	public Statistic getStatistic() {
		return _Statistic;
	}

	public String getService() {
		return _Service;
	}

	public String getHost() {
		return _Host;
	}

	public Double getValue() {
		return _Value;
	}

	public DateTime getPeriodStart() {
		return _PeriodStart;
	}

	public String getMetric() {
		return _Metric;
	}

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AggregatedData data = (AggregatedData) o;

        if (!_Host.equals(data._Host)) return false;
        if (!_Metric.equals(data._Metric)) return false;
        if (!_Period.equals(data._Period)) return false;
        if (!_PeriodStart.equals(data._PeriodStart)) return false;
        if (!_Service.equals(data._Service)) return false;
        if (!_Statistic.equals(data._Statistic)) return false;
        if (!_Value.equals(data._Value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _Statistic.hashCode();
        result = 31 * result + _Service.hashCode();
        result = 31 * result + _Host.hashCode();
        result = 31 * result + _Metric.hashCode();
        result = 31 * result + _Value.hashCode();
        result = 31 * result + _PeriodStart.hashCode();
        result = 31 * result + _Period.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("Statistic", _Statistic)
                .add("Service", _Service)
                .add("Host", _Host)
                .add("Metric", _Metric)
                .add("Value", _Value)
                .add("PeriodStart", _PeriodStart)
                .add("Period", _Period)
                .toString();
    }
}
