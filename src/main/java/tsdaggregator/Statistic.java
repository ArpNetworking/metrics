package tsdaggregator;

public interface Statistic {
	public Double calculate(Double[] orderedValues);
	public String getName();
}
