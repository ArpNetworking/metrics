package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Takes the mean of the entries.
 *
 * @author barp
 */
public class MeanStatistic extends BaseStatistic {

    @Nonnull
    @Override
    public Double calculate(@Nonnull Double[] orderedValues) {
        if (orderedValues.length == 0) {
            return 0d;
        }
        double sum = 0;
        for (Double val : orderedValues) {
            sum += val;
        }
        return sum / (double) orderedValues.length;
    }

    @Nonnull
    @Override
    public String getName() {
        return "mean";
    }

}
