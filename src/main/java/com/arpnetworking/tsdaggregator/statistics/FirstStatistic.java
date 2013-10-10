/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.statistics;

/**
 * @author brandarp
 */
public class FirstStatistic extends BaseStatistic {

    @Override
    public Double calculate(Double[] values) {
        return values[0];
    }

    @Override
    public String getName() {
        return "first";
    }

}
