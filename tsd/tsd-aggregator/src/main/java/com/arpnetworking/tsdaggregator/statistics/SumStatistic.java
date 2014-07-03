package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Sums the entries.
 *
 * @author barp
 */
public class SumStatistic extends BaseStatistic {
    @Override
    public Double calculate(@Nonnull Double[] unorderedValues) {
        Double sum = 0d;
        for (Double val : unorderedValues) {
            sum += val;
        }
        return sum;
    }

    @Nonnull
    @Override
    public String getName() {
        return "sum";
    }
}
