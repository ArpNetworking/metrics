package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

public class ConsolePublisher implements AggregationPublisher {
	private static ConsolePublisher _Instance;
	public synchronized static ConsolePublisher getInstance() {
		if (_Instance == null) {
			_Instance = new ConsolePublisher();
		}
		return _Instance;
	}
	@Override
	public void recordAggregation(AggregatedData[] data) {
		for (AggregatedData d : data) {
			System.out.println(d.getHost() + "::" + d.getService() + "::" + d.getMetric()
					+ " " + d.getPeriodStart() + " [" + d.getPeriod() + "] " + 
					d.getStatistic().getName() + ": " +
					d.getValue().toString());
		}
		
	}
	@Override
	public void close() {
		return;		
	}	
}
