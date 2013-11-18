package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.arpnetworking.tsdaggregator.statistics.NStatistic;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

/**
 * Tests for the BufferingPublisher class
 *
 * @author barp
 */
public class BufferingPublisherTests {
    @Test
    public void testConstruct() {
        Mockery context = new Mockery();
        AggregationPublisher p1 = context.mock(AggregationPublisher.class);
        @SuppressWarnings("UnusedAssignment") BufferingPublisher publisher = new BufferingPublisher(p1);
        @SuppressWarnings("UnusedAssignment") BufferingPublisher publisher2 = new BufferingPublisher(p1, 10);
    }

    @Test
    public void testDoesNotPublishUnderBuffer() {
        Mockery context = new Mockery();
        final AggregationPublisher p1 = context.mock(AggregationPublisher.class);
        BufferingPublisher publisher = new BufferingPublisher(p1, 10);
        context.checking(new Expectations() {{
            never(p1).recordAggregation(with(any(AggregatedData[].class)));
        }});

        for (int x = 0; x < 9; x++) {
            AggregatedData[] data = new AggregatedData[1];
            data[0] = new AggregatedData(new NStatistic(), "service" + x, "host", "metric", 1d, DateTime.now(), Period.minutes(5), new Double[]{});

            publisher.recordAggregation(data);
        }

        context.assertIsSatisfied();
    }

    @Test
    public void testDoesPublishOverBuffer() {
        Mockery context = new Mockery();
        final AggregationPublisher p1 = context.mock(AggregationPublisher.class);
        BufferingPublisher publisher = new BufferingPublisher(p1, 10);
        context.checking(new Expectations() {{
            one(p1).recordAggregation(with(any(AggregatedData[].class)));
        }});

        for (int x = 0; x < 10; x++) {
            AggregatedData[] data = new AggregatedData[1];
            data[0] = new AggregatedData(new NStatistic(), "service" + x, "host", "metric", 1d, DateTime.now(), Period.minutes(5), new Double[]{});

            publisher.recordAggregation(data);
        }

        context.assertIsSatisfied();
    }

    @Test
    public void testCloseSendsBufferedAggregations() {
        Mockery context = new Mockery();
        final AggregationPublisher p1 = context.mock(AggregationPublisher.class);
        BufferingPublisher publisher = new BufferingPublisher(p1, 10);
        context.checking(new Expectations() {{
            one(p1).recordAggregation(with(any(AggregatedData[].class)));
            one(p1).close();
        }});

        for (int x = 0; x < 8; x++) {
            AggregatedData[] data = new AggregatedData[1];
            data[0] = new AggregatedData(new NStatistic(), "service" + x, "host", "metric", 1d, DateTime.now(), Period.minutes(5), new Double[]{});

            publisher.recordAggregation(data);
        }

        publisher.close();

        context.assertIsSatisfied();
    }
}
