package tsdaggregator.statistics;

/**
 * Description goes here
 *
 * @author barp
 */
public abstract class BaseStatistic implements Statistic {
    @Override
    public String toString() {
        return getName();
    }
}
