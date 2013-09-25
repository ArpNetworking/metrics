package tsdaggregator.util;

import tsdaggregator.statistics.Statistic;

/**
 * Throws an exception on initialization
 *
 * @author barp
 */
public class InitializeExceptionStatistic implements Statistic {
	public InitializeExceptionStatistic(String arg) {
	}

	@Override
	public Double calculate(Double[] unorderedValues) {
		return 0d;
	}

	@Override
	public String getName() {
		return "InitializationExceptionStatistic";
	}
}
