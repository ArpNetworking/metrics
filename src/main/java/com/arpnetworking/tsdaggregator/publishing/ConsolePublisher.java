package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

/**
 * A publisher that writes to System.out.
 *
 * @author barp
 */
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
