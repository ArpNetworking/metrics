package tsdaggregator.statistics;

public class NStatistic extends BaseStatistic {

	@Override
	public Double calculate(Double[] unorderedValues) {
		return Integer.valueOf(unorderedValues.length).doubleValue();
	}

	@Override
	public String getName() {
		return "n";
	}

}
