package com.arpnetworking.tsdaggregator;


import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.collect.Sets;
import org.jmock.Mockery;
import org.joda.time.Period;
import org.junit.Test;

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
        Set<Statistic> timerStats = Sets.newHashSet();
        Set<Statistic> counterStats = Sets.newHashSet();
        Set<Statistic> gaugeStats = Sets.newHashSet();
        String hostName = "host";
        String service = "serviceName";
        Set<Period> periods = Sets.newHashSet();
        periods.add(Period.seconds(7));
        periods.add(Period.minutes(3));
        AggregationPublisher publisher = mockery.mock(AggregationPublisher.class);
        LineProcessor processor = new LineProcessor(parser, timerStats, counterStats, gaugeStats, hostName, service, periods, publisher);
        processor.shutdown();
    }

}
