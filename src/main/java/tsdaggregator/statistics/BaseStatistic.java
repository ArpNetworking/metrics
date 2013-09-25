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

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
	}
}
