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
		int index = (int) (Math.ceil((_TStat / 100) * (orderedValues.length - 1)));
		return orderedValues[index];
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TPStatistic) {
			TPStatistic other = (TPStatistic)obj;
			return other._TStat.equals(_TStat);
		}
		return false;
	}
}
