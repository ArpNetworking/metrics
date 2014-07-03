package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;

import javax.annotation.Nonnull;

/**
 * A publisher that writes to System.out.
 *
 * @author barp
 */
public class ConsolePublisher implements AggregationPublisher {
    @Override
    public void recordAggregation(@Nonnull AggregatedData[] data) {
        for (@Nonnull AggregatedData d : data) {
            System.out.println(d.getHost() + "::" + d.getService() + "::" + d.getMetric()
                    + " " + d.getPeriodStart() + " [" + d.getPeriod() + "] " +
                    d.getStatistic().getName() + ": " +
                    d.getValue());
        }

    }

    @Override
    public void close() {
    }
}
