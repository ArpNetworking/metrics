package tsdaggregator;

public class MeanStatistic implements Statistic {

	@Override
	public Double calculate(Double[] orderedValues) {
        if (orderedValues.length == 0) {
            return 0d;
        }
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
