/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;

/**
 * @author brandarp
 */
public class LastStatistic extends BaseStatistic {

    @Override
    public Double calculate(@Nonnull Double[] values) {
        return values[values.length - 1];
    }

    @Nonnull
    @Override
    public String getName() {
        return "last";
    }
}
