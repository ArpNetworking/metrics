package com.arpnetworking.tsdaggregator.statistics;

import org.apache.log4j.Logger;

import java.text.DecimalFormat;

/**
 * Base statistic for percentile based statistics.
 *
 * @author barp
 */
public class TPStatistic extends BaseStatistic implements OrderedStatistic {
    static final Logger LOGGER = Logger.getLogger(TPStatistic.class);
    static final DecimalFormat FORMAT = new DecimalFormat("##0.#");
    Double _tStat = 0.0;

    public TPStatistic(Double tstat) {
        _tStat = tstat;
    }

    public String getName() {
        return "tp" + FORMAT.format(_tStat);
    }

    public Double calculate(Double[] orderedValues) {
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
