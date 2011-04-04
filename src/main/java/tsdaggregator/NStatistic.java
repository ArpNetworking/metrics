package tsdaggregator;

public class NStatistic implements Statistic {

	@Override
	public Double calculate(Double[] orderedValues) {
		return Integer.valueOf(orderedValues.length).doubleValue();
	}

	@Override
	public String getName() {
		return "n";
	}

}
