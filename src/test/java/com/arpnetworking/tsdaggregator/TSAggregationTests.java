package com.arpnetworking.tsdaggregator;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.arpnetworking.tsdaggregator.publishing.ConsolePublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;

import java.util.HashSet;
import java.util.Set;

@RunWith(JMock.class)
public class TSAggregationTests {
	Mockery context = new JUnit4Mockery();

	@Test
	public void testContruct() {
		Period period = new Period(0, 5, 0, 0);
        Set<Statistic> stats = new HashSet<>();
		TSAggregation agg = new TSAggregation("test metric", period, new ConsolePublisher(), "testHost", "testService", stats);
		Assert.assertNotNull(agg);
	}
	
//	@Test
//	public void TestSimpleAggregation() {
//		final Period period = new Period(0, 5, 0, 0);
//		final AggregationPublisher listener = context.mock(AggregationPublisher.class);
//		Set<Statistic> stats = new HashSet<Statistic>();
//		stats.add(new TPStatistic(100d));
//		stats.add(new TPStatistic(50d));
//		stats.add(new TPStatistic(0d));
//		
//		final AggregatedData tp100data = new AggregatedData() {{ 
//			setPeriod(period); 
//			setHost("localhost");
//			setPeriodStart(new DateTime(2011, 1, 3, 15, 20, 0, 0, DateTimeZone.UTC));
//			setService("localservice");
//			setStatistic(new TPStatistic(100d));
//			setValue(5d);
//		}};
//		
//		final AggregatedData tp0data = new AggregatedData() {{ 
//			setPeriod(period); 
//			setHost("localhost");
//			setPeriodStart(new DateTime(2011, 1, 3, 15, 20, 0, 0, DateTimeZone.UTC));
//			setService("localservice");
//			setStatistic(new TPStatistic(0d));
//			setValue(1d);
//		}};
//		
//		final AggregatedData tp50data = new AggregatedData() {{ 
//			setPeriod(period); 
//			setHost("localhost");
//			setPeriodStart(new DateTime(2011, 1, 3, 15, 20, 0, 0, DateTimeZone.UTC));
//			setService("localservice");
//			setStatistic(new TPStatistic(50d));
//			setValue(3d);
//		}};
//		
//		context.checking(new Expectations() {{
//			oneOf(listener).recordAggregation(with(equal(tp100data)));
//			oneOf(listener).recordAggregation(with(equal(tp0data)));
//			oneOf(listener).recordAggregation(with(equal(tp50data)));
//		}});
//		
//		TSAggregation agg = new TSAggregation("foometric", period, listener, stats);
//		agg.addSample(1d, new DateTime(2011, 1, 3, 15, 23, 38, 181, DateTimeZone.UTC));
//		agg.addSample(2d, new DateTime(2011, 1, 3, 15, 23, 39, 181, DateTimeZone.UTC));
//		agg.addSample(3d, new DateTime(2011, 1, 3, 15, 23, 40, 181, DateTimeZone.UTC));
//		agg.addSample(4d, new DateTime(2011, 1, 3, 15, 23, 41, 181, DateTimeZone.UTC));
//		agg.addSample(5d, new DateTime(2011, 1, 3, 15, 23, 42, 181, DateTimeZone.UTC));
//		agg.addSample(2d, new DateTime(2011, 1, 3, 15, 26, 38, 181, DateTimeZone.UTC));
//	}
}
