package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

public class ConsolePublisher implements AggregationPublisher {
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
