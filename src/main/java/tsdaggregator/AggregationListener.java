package tsdaggregator;

public interface AggregationListener {
	void recordAggregation(AggregatedData[] data);
	void close();
}
