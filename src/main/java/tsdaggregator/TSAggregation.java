package tsdaggregator;

import java.util.AbstractList;
import java.util.ArrayList;

import org.apache.commons.collections.list.*;
import org.joda.time.*;

public class TSAggregation {
	Period _Period;
	Integer _NumberOfSamples = 0;
	@SuppressWarnings("unchecked")
	AbstractList<Double> _Samples = new TreeList();
	DateTime _PeriodStart = new DateTime(0);
	ArrayList<Statistic> _Statistics = new ArrayList<Statistic>();
	AggregationListener _Listener;
	
	
	public TSAggregation(Period period, AggregationListener listener) {
		_Period = period;
		_Statistics.add(new TPStatistic(100d));
		_Statistics.add(new TPStatistic(50d));
		_Statistics.add(new TPStatistic(0d));
		_Listener = listener;
	}
	
	public void addSample(Double value, DateTime time) {
		rotateAggregation(time);
		_Samples.add(value);
		_NumberOfSamples++;
	}
	
	private void rotateAggregation(DateTime time){
		if (time.isAfter(_PeriodStart.plus(_Period))) {
			//Calculate the start of the new aggregation
			DateTime hour = time.hourOfDay().roundFloorCopy();
			DateTime startPeriod = hour;
			while (!(startPeriod.isBefore(time) && startPeriod.plus(_Period).isAfter(time))) {
				startPeriod = startPeriod.plus(_Period);
			}
			emitAggregations();
			_PeriodStart = startPeriod;			
			_NumberOfSamples = 0;
			_Samples.clear();
		}
	}
	
	public AggregationListener getListener() {
		return _Listener;
	}

	public void setListener(AggregationListener listener) {
		_Listener = listener;
	}

	private void emitAggregations()
	{
		Double[] dsamples = _Samples.toArray(new Double[0]);
		if (dsamples.length == 0) {
			return;
		}
		for (Statistic stat : _Statistics) {
			Double value = stat.calculate(dsamples);
			if (_Listener != null) {
				AggregatedData data = new AggregatedData();
				data.setPeriod(_Period);
				data.setHost("localhost");
				data.setService("localservice");
				data.setStatistic(stat);
				data.setValue(value);
				data.setPeriodStart(_PeriodStart);
				_Listener.recordAggregation(data);
			}
		}
	}
}
