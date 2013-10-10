package com.arpnetworking.tsdaggregator.statistics;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for the MeanStatistic class
 *
 * @author barp
 */
public class MeanStatisticTests {
	@Test
	public void testConstruction() {
		@SuppressWarnings("UnusedAssignment") MeanStatistic stat = new MeanStatistic();
	}

	@Test
	public void testGetName() {
		MeanStatistic stat = new MeanStatistic();
		assertThat(stat.getName(), equalTo("mean"));
	}

	@Test
	public void testCalculate() {
		MeanStatistic stat = new MeanStatistic();
		Double[] vals = {12d, 20d, 7d};
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(13d));
	}

	@Test
	public void testCalculateWithNoEntries() {
		MeanStatistic stat = new MeanStatistic();
		Double[] vals = {};
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(0d));
	}
}
