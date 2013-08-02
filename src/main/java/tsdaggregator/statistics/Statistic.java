package tsdaggregator.statistics;

public interface Statistic {
	public Double calculate(Double[] unorderedValues);
	public String getName();
}
