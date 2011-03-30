package tsdaggregator;

import java.util.AbstractList;
import java.util.ArrayList;

import org.apache.commons.collections.list.*;
import org.joda.time.*;

public class TSAggregation {
	AggregationSpecifier _Specifier;
	Integer _NumberOfSamples = 0;
	@SuppressWarnings("unchecked")
	AbstractList<Double> _Samples = new TreeList();
	DateTime _PeriodStart = new DateTime(0);
	ArrayList<Statistic> _Statistics = new ArrayList<Statistic>();
	
	
	public TSAggregation(AggregationSpecifier specifier) {
		_Specifier = specifier;
		_Statistics.add(new TPStatistic(100d));
		_Statistics.add(new TPStatistic(50d));
		_Statistics.add(new TPStatistic(0d));
	}
	
	public void addSample(Double value, DateTime time) {
		rotateAggregation(time);
		_Samples.add(value);
		_NumberOfSamples++;
	}
	
	private void rotateAggregation(DateTime time){
		if (time.isAfter(_PeriodStart.plus(_Specifier.getPeriod()))) {
			//Calculate the start of the new aggregation
			DateTime hour = time.hourOfDay().roundFloorCopy();
			DateTime startPeriod = hour;
			while (!(startPeriod.isBefore(time) && startPeriod.plus(_Specifier.getPeriod()).isAfter(time))) {
				startPeriod = startPeriod.plus(_Specifier.getPeriod());
			}
			_PeriodStart = startPeriod;
			emitAggregations();
			_NumberOfSamples = 0;
			_Samples.clear();
		}
	}
	
	private void emitAggregations()
	{
		System.out.println("emitting statistics");
		Double[] dsamples = _Samples.toArray(new Double[0]);
		if (dsamples.length == 0) {
			return;
		}
		for (Statistic s : _Statistics) {
			System.out.println(s.getName() + ": " + s.calculate(dsamples));
		}
	}
}
