/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator;

/**
 *
 * @author brandarp
 */
public class FirstStatistic implements Statistic {

    @Override
    public Double calculate(Double[] values) {
        return values[0];
    }

    @Override
    public String getName() {
        return "first";
    }
    
}
