package tsdaggregator;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 11:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class CounterVariable {
    private Boolean isCounter = false;
    private ArrayList<Double> values;

    public CounterVariable(Boolean isCounter, ArrayList<Double> values) {
        this.values = values;
        this.isCounter= isCounter;
    }

    public Boolean getCounter() {
        return isCounter;
    }

    public ArrayList<Double> getValues() {
        return values;
    }
}
