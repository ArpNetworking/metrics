package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

public interface AggregationPublisher {
	void recordAggregation(AggregatedData[] data);
	void close();
}
