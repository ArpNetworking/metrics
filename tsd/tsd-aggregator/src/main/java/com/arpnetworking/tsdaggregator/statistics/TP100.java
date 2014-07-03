/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * Top percentile 100% statistic (max).
 *
 * @author barp
 */
public class TP100 extends TPStatistic {
    public TP100() {
        super(100d);
    }

    @Nonnull
    @Override
    public String getName() {
        return "max";
    }
}
