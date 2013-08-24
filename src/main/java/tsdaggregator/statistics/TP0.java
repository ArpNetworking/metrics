/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator.statistics;

/**
 *
 * @author brandarp
 */
public class TP0 extends TPStatistic {
	@Override
	public String getName() {
		return "min";
	}

	public TP0() {
        super(0d);
    }
}
