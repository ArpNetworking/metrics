package com.arpnetworking.tsdaggregator.statistics;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for the SumStatistic class
 *
 * @author barp
 */
public class SumStatisticTests {
	@Test
	public void testConstruction() {
		@SuppressWarnings("UnusedAssignment") SumStatistic stat = new SumStatistic();
	}

	@Test
	public void testGetName() {
		SumStatistic stat = new SumStatistic();
		assertThat(stat.getName(), equalTo("sum"));
	}

	@Test
	public void testCalculate() {
		SumStatistic stat = new SumStatistic();
		Double[] vals = {12d, 18d, 5d};
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(35d));
	}
}
