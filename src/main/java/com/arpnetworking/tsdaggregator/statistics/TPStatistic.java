package com.arpnetworking.tsdaggregator.statistics;

import java.text.DecimalFormat;
import javax.annotation.Nonnull;

/**
 * Base statistic for percentile based statistics.
 *
 * @author barp
 */
public class TPStatistic extends BaseStatistic implements OrderedStatistic {
    private static final DecimalFormat FORMAT = new DecimalFormat("##0.#");
    private Double _tStat = 0.0;

    public TPStatistic(Double tstat) {
        _tStat = tstat;
    }

    @Nonnull
    public String getName() {
        return "tp" + FORMAT.format(_tStat);
    }

    public Double calculate(@Nonnull Double[] orderedValues) {
        int index = (int) (Math.ceil((_tStat / 100) * (orderedValues.length - 1)));
        return orderedValues[index];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TPStatistic) {
            TPStatistic other = (TPStatistic) obj;
            return other._tStat.equals(_tStat);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _tStat.hashCode();
    }
}
