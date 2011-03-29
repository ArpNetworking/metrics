package tsdaggregator;

public class TPStatistic implements Statistic{
	Double _TStat = 0.0;
	public TPStatistic(Double tstat) {
		_TStat = tstat;
	}
	
	public String getName()	{
		return "tp" + _TStat.toString();
	}
	
	public  Double calculate(Double[] orderedValues) {
		int index = (int) (Math.ceil(_TStat * (orderedValues.length - 1)));
		return orderedValues[index];
	}
}
