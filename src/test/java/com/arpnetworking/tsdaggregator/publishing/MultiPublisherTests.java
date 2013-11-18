package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.arpnetworking.tsdaggregator.statistics.SumStatistic;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

/**
 * Tests for the MultiPublisher class
 *
 * @author barp
 */
public class MultiPublisherTests {
	@Test
	public void testConstruction() {
		@SuppressWarnings("UnusedAssignment") MultiPublisher publisher = new MultiPublisher();
	}

	@Test
	public void testPublishSendsToAllChildren() {
		MultiPublisher publisher = new MultiPublisher();
		final AggregatedData[] data = new AggregatedData[1];
		Mockery context = new Mockery();
		final AggregationPublisher mockPub1 = context.mock(AggregationPublisher.class, "child1");
		final AggregationPublisher mockPub2 = context.mock(AggregationPublisher.class, "child2");
		context.checking(new Expectations() {{
			oneOf(mockPub1).recordAggregation(with(Expectations.same(data)));
			oneOf(mockPub2).recordAggregation(with(Expectations.same(data)));

		}});

		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.addListener(mockPub1);
		publisher.addListener(mockPub2);
		publisher.recordAggregation(data);
		context.assertIsSatisfied();
	}

	@Test
	public void testClosesAllChildren() {
		MultiPublisher publisher = new MultiPublisher();
		Mockery context = new Mockery();
		final AggregationPublisher mockPub1 = context.mock(AggregationPublisher.class, "child1");
		final AggregationPublisher mockPub2 = context.mock(AggregationPublisher.class, "child2");
		context.checking(new Expectations() {{
			oneOf(mockPub1).close();
			oneOf(mockPub2).close();
		}});

		publisher.addListener(mockPub1);
		publisher.addListener(mockPub2);
		publisher.close();
		context.assertIsSatisfied();
	}
}
