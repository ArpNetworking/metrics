package com.arpnetworking.tsdaggregator.util;

import com.arpnetworking.tsdaggregator.statistics.Statistic;

import javax.annotation.Nonnull;

/**
 * Throws an exception on initialization
 *
 * @author barp
 */
public class InitializeExceptionStatistic implements Statistic {
	public InitializeExceptionStatistic(String needsArg) {
	}

	@Nonnull
    @Override
	public Double calculate(Double[] unorderedValues) {
		return 0d;
	}

	@Nonnull
    @Override
	public String getName() {
		return "InitializationExceptionStatistic";
	}
}
