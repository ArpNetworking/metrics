package com.arpnetworking.tsdaggregator;


import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import org.jmock.Mockery;
import org.joda.time.Period;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for the LineProcessor class
 *
 * @author barp
 */
public class LineProcessorTests {
    @Test
    public void testConstructor() {
        Mockery mockery = new Mockery();
        LogParser parser = mockery.mock(LogParser.class);
        Set<Statistic> timerStats = new HashSet<>();
        Set<Statistic> counterStats = new HashSet<>();
        Set<Statistic> gaugeStats = new HashSet<>();
        String hostName = "host";
        String service = "serviceName";
        Set<Period> periods = new HashSet<>();
        periods.add(Period.seconds(7));
        periods.add(Period.minutes(3));
        AggregationPublisher publisher = mockery.mock(AggregationPublisher.class);
        LineProcessor processor = new LineProcessor(parser, timerStats, counterStats, gaugeStats, hostName, service, periods, publisher);
        processor.shutdown();
    }

}
