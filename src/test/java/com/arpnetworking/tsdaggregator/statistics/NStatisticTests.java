package com.arpnetworking.tsdaggregator.statistics;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for the NStatistic class
 *
 * @author barp
 */
public class NStatisticTests {
    @Test
    public void testConstruction() {
        @SuppressWarnings("UnusedAssignment") NStatistic stat = new NStatistic();
    }

    @Test
    public void testGetName() {
        NStatistic stat = new NStatistic();
        assertThat(stat.getName(), equalTo("n"));
    }

    @Test
    public void testCalculate() {
        NStatistic stat = new NStatistic();
        Double[] vals = {12d, 18d, 5d};
        Double calculated = stat.calculate(vals);
        assertThat(calculated, equalTo(3d));
    }
}
