package tsdaggregator.statistics;

/**
 * Sums the entries
 *
 * @author barp
 */
public class SumStatistic extends BaseStatistic {
    @Override
    public Double calculate(Double[] unorderedValues) {
        Double sum = 0d;
        for (Double val : unorderedValues) {
            sum += val;
        }
        return sum;
    }

    @Override
    public String getName() {
        return "sum";
    }
}
