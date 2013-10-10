/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

/**
 * Top percentile 50% statistic (median).
 *
 * @author barp
 */

public class TP50 extends TPStatistic {
    public TP50() {
        super(50d);
    }
}
