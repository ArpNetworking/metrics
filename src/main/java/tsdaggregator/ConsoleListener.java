package tsdaggregator;

public class ConsoleListener implements AggregationListener {
	private static ConsoleListener _Instance;
	public synchronized static ConsoleListener getInstance() {
		if (_Instance == null) {
			_Instance = new ConsoleListener();
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
