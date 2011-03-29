package tsdaggregator;

import org.joda.time.*;

public class AggregationSpecifier {
	
	private Period _Period;
	
	public AggregationSpecifier(Period period)
	{
		_Period = period;
	}
	
	public Period getPeriod()
	{
		return _Period;
	}
}
