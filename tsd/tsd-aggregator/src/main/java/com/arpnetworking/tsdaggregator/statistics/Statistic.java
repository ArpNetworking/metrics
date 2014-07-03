package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Interface for a statistic calculator.
 *
 * @author barp
 */
public interface Statistic {
    public Double calculate(Double[] unorderedValues);

    @Nonnull
    public String getName();
}
