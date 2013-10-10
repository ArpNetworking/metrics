package com.arpnetworking.tsdaggregator.statistics;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import javax.annotation.Nonnull;

/**
 * Tests for the TPStatistic classes
 *
 * @author barp
 */
public class PStatTests {
	private final Double[] percs = generateNumber(100);
	private final Double[] thousands = generateNumber(1000);

	@Nonnull
    private Double[] generateNumber(int number) {
		ArrayList<Double> vals = new ArrayList<>();
		for (double x = 0; x < number; x++) {
			vals.add(x);
		}
		return vals.toArray(new Double[vals.size()]);
	}

	@Test
	public void testConstructionTP0() {
		TP0 stat = new TP0();
		assertThat(stat.toString(), equalTo("min"));
	}

	@Test
	public void testBaseTStatEquality() {
		TP0 first = new TP0();
		TP0 second = new TP0();
		TP100 third = new TP100();
		NStatistic nStatistic = new NStatistic();
        //noinspection ObjectEqualsNull
        assertThat(first.equals(null), equalTo(false));
		assertThat(first.equals(first), equalTo(true));
        assertThat(first.equals(second), equalTo(true));
        //noinspection ObjectEqualsNull
        assertThat(nStatistic.equals(null), equalTo(false));
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(nStatistic.equals(first), equalTo(false));
        assertThat(nStatistic.equals(nStatistic), equalTo(true));
		assertThat(second.equals(first), equalTo(true));
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(first.equals(third), equalTo(false));
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(first.equals(nStatistic), equalTo(false));
        //noinspection ObjectEqualsNull
        assertThat(first.equals(null), equalTo(false));
	}

	@Test
	public void testGetNameTP0() {
		TP0 stat = new TP0();
		assertThat(stat.getName(), equalTo("min"));
	}

	@Test
	public void testCalculateTP0() {
		TP0 stat = new TP0();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(0d));
	}

	@Test
	public void testConstructionTP50() {
		@SuppressWarnings("UnusedAssignment") TP50 stat = new TP50();
	}

	@Test
	public void testGetNameTP50() {
		TP50 stat = new TP50();
		assertThat(stat.getName(), equalTo("tp50"));
	}

	@Test
	public void testCalculateTP50() {
		TP50 stat = new TP50();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(50d));
	}

	@Test
	public void testConstructionTP90() {
		@SuppressWarnings("UnusedAssignment") TP90 stat = new TP90();
	}

	@Test
	public void testGetNameTP90() {
		TP90 stat = new TP90();
		assertThat(stat.getName(), equalTo("tp90"));
	}

	@Test
	public void testCalculateTP90() {
		TP90 stat = new TP90();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(90d));
	}

	@Test
	public void testConstructionTP95() {
		@SuppressWarnings("UnusedAssignment") TP95 stat = new TP95();
	}

	@Test
	public void testGetNameTP95() {
		TP95 stat = new TP95();
		assertThat(stat.getName(), equalTo("tp95"));
	}

	@Test
	public void testCalculateTP95() {
		TP95 stat = new TP95();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(95d));
	}

	@Test
	public void testConstructionTP99() {
		@SuppressWarnings("UnusedAssignment") TP99 stat = new TP99();
	}

	@Test
	public void testGetNameTP99() {
		TP99 stat = new TP99();
		assertThat(stat.getName(), equalTo("tp99"));
	}

	@Test
	public void testCalculateTP99() {
		TP99 stat = new TP99();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(99d));
	}

	@Test
	public void testConstructionTP99p9() {
		@SuppressWarnings("UnusedAssignment") TP99p9 stat = new TP99p9();
	}

	@Test
	public void testGetNameTP99p9() {
		TP99p9 stat = new TP99p9();
		assertThat(stat.getName(), equalTo("tp99.9"));
	}

	@Test
	public void testCalculateTP99p9() {
		TP99p9 stat = new TP99p9();
		Double[] vals = thousands;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(999d));
	}

	@Test
	public void testConstructionTP100() {
		@SuppressWarnings("UnusedAssignment") TP100 stat = new TP100();
	}

	@Test
	public void testGetNameTP100() {
		TP100 stat = new TP100();
		assertThat(stat.getName(), equalTo("max"));
	}

	@Test
	public void testCalculateTP100() {
		TP100 stat = new TP100();
		Double[] vals = percs;
		Double calculated = stat.calculate(vals);
		assertThat(calculated, equalTo(99d));
	}
}
