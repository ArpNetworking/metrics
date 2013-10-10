package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.statistics.TPStatistic;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class TPStatisticTests {

	@Test
	public void TestTP0Stat() {
		TPStatistic tp0 = new TPStatistic(0d);
		Double[] vals = new Double[] {1d, 2d, 3d, 4d, 5d};
		
		Double tp0stat = tp0.calculate(vals);
		Assert.assertThat(tp0stat, CoreMatchers.is(1d));		
	}
	
	@Test
	public void TestTP100Stat() {
		TPStatistic tp100 = new TPStatistic(100d);
		Double[] vals = new Double[] {1d, 2d, 3d, 4d, 5d};
		
		Double tpstat = tp100.calculate(vals);
		Assert.assertThat(tpstat, CoreMatchers.is(5d));		
	}
	
	@Test
	public void TestTP99StatSmallSet() {
		TPStatistic tp = new TPStatistic(99d);
		Double[] vals = new Double[] {1d, 2d, 3d, 4d, 5d};
		
		Double tpstat = tp.calculate(vals);
		Assert.assertThat(tpstat, CoreMatchers.is(5d));		
	}
	
	@Test
	public void TestTP99Stat() {
		TPStatistic tp = new TPStatistic(99d);
		ArrayList<Double> vList = new ArrayList<>();
		for (Integer x = 0; x < 100; x++) {
			vList.add(x.doubleValue());
		}
		Double[] vals = vList.toArray(new Double[vList.size()]);
		
		Double tpstat = tp.calculate(vals);
		Assert.assertThat(tpstat, CoreMatchers.is(99d));		
	}
	
	@Test
	public void TestTP50Stat() {
		TPStatistic tp = new TPStatistic(50d);
		ArrayList<Double> vList = new ArrayList<>();
		for (Integer x = 0; x < 100; x++) {
			vList.add(x.doubleValue());
		}
		Double[] vals = vList.toArray(new Double[vList.size()]);
		
		Double tpstat = tp.calculate(vals);
		Assert.assertThat(tpstat, CoreMatchers.is(50d));		
	}
	
	@Test
	public void TestTP999Stat() {
		TPStatistic tp = new TPStatistic(99.9d);
		ArrayList<Double> vList = new ArrayList<>();
		for (Integer x = 0; x < 10000; x++) {
			vList.add(x.doubleValue());
		}
		Double[] vals = vList.toArray(new Double[vList.size()]);
		
		Double tpstat = tp.calculate(vals);
		Assert.assertThat(tpstat, CoreMatchers.is(9990d));		
	}
}
