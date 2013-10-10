/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Top percentile 0% statistic (min).
 *
 * @author barp
 */

public class TP0 extends TPStatistic {
    @Nonnull
    @Override
    public String getName() {
        return "min";
    }

    public TP0() {
        super(0d);
    }
}
