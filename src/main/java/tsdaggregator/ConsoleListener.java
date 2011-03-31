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
	public void recordAggregation(AggregatedData data) {
		System.out.println(data.getHost() + "\\" + data.getService()
				+ " " + data.getPeriodStart() + " [" + data.getPeriod() + "] " + 
				data.getStatistic().getName() + ": " +
				data.getValue().toString());
		
	}
}
