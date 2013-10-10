package com.arpnetworking.tsdaggregator.statistics;

/**
 * Interface for a statistic calculator.
 *
 * @author barp
 */
public interface Statistic {
    public Double calculate(Double[] unorderedValues);

    public String getName();
}
