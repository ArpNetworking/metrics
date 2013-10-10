package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Counts the entries.
 *
 * @author barp
 */
public class NStatistic extends BaseStatistic {

    @Override
    public Double calculate(@Nonnull Double[] unorderedValues) {
        return Integer.valueOf(unorderedValues.length).doubleValue();
    }

    @Nonnull
    @Override
    public String getName() {
        return "n";
    }

}
