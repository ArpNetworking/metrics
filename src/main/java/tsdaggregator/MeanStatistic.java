package tsdaggregator;

public class MeanStatistic implements Statistic {

	@Override
	public Double calculate(Double[] orderedValues) {
		double sum = 0;
		for (Double val : orderedValues) {
			sum += val;
		}
		return sum / (double)orderedValues.length;
	}

	@Override
	public String getName() {
		return "mean";
	}

}
