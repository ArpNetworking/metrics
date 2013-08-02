/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator.statistics;

/**
 *
 * @author brandarp
 */
public class LastStatistic extends BaseStatistic {

    @Override
    public Double calculate(Double[] values) {
        return values[values.length - 1];
    }

    @Override
    public String getName() {
        return "last";
    }    
}
