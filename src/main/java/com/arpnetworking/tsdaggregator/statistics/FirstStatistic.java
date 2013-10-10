/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * @author brandarp
 */
public class FirstStatistic extends BaseStatistic {

    @Override
    public Double calculate(Double[] values) {
        return values[0];
    }

    @Nonnull
    @Override
    public String getName() {
        return "first";
    }

}
