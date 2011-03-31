package tsdaggregator;

import org.joda.time.DateTime;
import org.joda.time.Period;

public class AggregatedData {
	Statistic _Statistic;
	String _Service;
	String _Host;
	Double _Value;
	DateTime _PeriodStart;
	Period _Period;
	
	public Period getPeriod() {
		return _Period;
	}
	public void setPeriod(Period period) {
		_Period = period;
	}
	public Statistic getStatistic() {
		return _Statistic;
	}
	public void setStatistic(Statistic statistic) {
		_Statistic = statistic;
	}
	public String getService() {
		return _Service;
	}
	public void setService(String service) {
		_Service = service;
	}
	public String getHost() {
		return _Host;
	}
	public void setHost(String host) {
		_Host = host;
	}
	public Double getValue() {
		return _Value;
	}
	public void setValue(Double value) {
		_Value = value;
	}
	public DateTime getPeriodStart() {
		return _PeriodStart;
	}
	public void setPeriodStart(DateTime periodStart) {
		_PeriodStart = periodStart;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AggregatedData) {
			AggregatedData other = (AggregatedData)obj;
			return other._Host.equals(_Host) &&
				other._Period.equals(_Period) &&
				other._PeriodStart.equals(_PeriodStart) &&
				other._Service.equals(_Service) &&
				other._Statistic.equals(_Statistic) &&
				other._Value.equals(_Value);
		}
		return false;
	}
}
