package com.arpnetworking.tsdaggregator;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class LineDataTests {
	@Test
	public void ConstructTest()	{
		QueryLogParser parser = new QueryLogParser();
		Assert.assertNotNull(parser);
	}

	@Test
	public void ParseSingleEntry() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view=26383.8768005,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		ArrayList<Double> setView = map.get("set/view").getValues();
		assertThat(setView.size(), equalTo(1));
		assertThat(setView.get(0), equalTo(26383.8768005));
		DateTime timestamp = line.getTime();
		assertThat(timestamp, equalTo(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC())));
	}

	@Test
	public void ParseLegacyBadTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=13q00976184.02,set/view=26383.8768005,]");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void ParseLegacyBadMetric() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view=26383.8768q005,set/set=12817.1234]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		ArrayList<Double> setSet = map.get("set/set").getValues();
		assertThat(setSet.size(), equalTo(1));
		assertThat(setSet.get(0), equalTo(12817.1234));
		DateTime timestamp = line.getTime();
		assertThat(timestamp, equalTo(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC())));
	}

	@Test
	public void ParseSingleEntryWithExtraCommas() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view=26383.8768005,,,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		ArrayList<Double> setView = map.get("set/view").getValues();
		assertThat(setView.size(), equalTo(1));
		assertThat(setView.get(0), equalTo(26383.8768005));
		DateTime timestamp = line.getTime();
		assertThat(timestamp, equalTo(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC())));
	}

	@Test
	public void ParseSingleEntryWithExtraCommasAndWhitespace() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view=26383.8768005, ,,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		ArrayList<Double> setView = map.get("set/view").getValues();
		assertThat(setView.size(), equalTo(1));
		assertThat(setView.get(0), equalTo(26383.8768005));
		DateTime timestamp = line.getTime();
		assertThat(timestamp, equalTo(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC())));
	}

	@Test
	public void ParseSingleEntryNotEnded() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view-start=0,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(0));
	}

	@Test
	public void ParseSingleEntryWithStartedSuffix() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976184.02,set/view-start=26383.8768005,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		ArrayList<Double> setView = map.get("set/view").getValues();
		assertThat(setView.size(), equalTo(1));
		assertThat(setView.get(0), equalTo(26383.8768005));
		DateTime timestamp = line.getTime();
		assertThat(timestamp, equalTo(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC())));
	}

	@Test
	public void ParseMultipleEntries() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[initTimestamp=1300976164.85,passport/signin=395539.999008,passport/_signinValidatePassword=390913.009644,passport/signinValid=1,]");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(1d, map.get("passport/signinValid").getValues().get(0));
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1300976164.85d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void ParseNoTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("[passport/signin=395539.999008,passport/_signinValidatePassword=390913.009644,passport/signinValid=1,]");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}
        
    @Test
    public void Parse2aVersionEntry() {
        QueryLogParser data = new QueryLogParser();
        Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563},\"timers\":{\"qmanager\\/index\":[99496.126174927,106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
        Map<String, CounterVariable> map = line.getVariables();
        Assert.assertEquals(1, map.size());
        ArrayList<Double> vals = map.get("qmanager/index").getValues();
        Assert.assertEquals(99496.126174927d, vals.get(0));
        Assert.assertEquals(106616.37284927d, vals.get(1));
        DateTime timestamp = line.getTime();
        Assert.assertEquals(new DateTime((long)(1311574570.3563d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
    }

	@Test
	public void Parse2aVersionEntryBadTimerValue() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563,\"counter1\":15},\"timers\":{\"qmanager\\/index\":[\"99496.1261w74927\",106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(2));
		ArrayList<Double> vals = map.get("qmanager/index").getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(106616.37284927d));
		vals = map.get("counter1").getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1311574570.3563d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2aVersionEntryBadCounterValue() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563,\"counter1\":\"1w5\",\"counter2\":8},\"timers\":{\"qmanager\\/index\":[\"99496.126174927\",106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		ArrayList<Double> vals = map.get("qmanager/index").getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(99496.126174927d));
		assertThat(vals.get(1), equalTo(106616.37284927d));
		vals = map.get("counter1").getValues();
		assertThat(vals.size(), equalTo(0));
		vals = map.get("counter2").getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(8d));
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1311574570.3563d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2aVersionEntryBadTimestampValue() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":131157q570.3563},\"timers\":{\"qmanager\\/index\":[99496.1261w74927,106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2aVersionEntryNoTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1311574570.3563d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2aVersionEntryNoCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"timers\":{\"qmanager\\/index\":[99496.126174927,106616.37284927]},\"annotations\":[]}");
		//null due to no timestamp data
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2aVersionEntryNoAnnotations() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563},\"timers\":{\"qmanager\\/index\":[99496.126174927,106616.37284927]}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		Assert.assertEquals(1, map.size());
		ArrayList<Double> vals = map.get("qmanager/index").getValues();
		Assert.assertEquals(99496.126174927d, vals.get(0));
		Assert.assertEquals(106616.37284927d, vals.get(1));
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1311574570.3563d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2aEntryBadCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":[\"initTimestamp\",1311574570.3563, \"something\",1281.128],\"timers\":{\"qmanager\\/index\":[99496.126174927,106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2aEntryBadTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{\"initTimestamp\":1311574570.3563,\"something\":1281.128},\"timers\":[\"qmanager\\/index\",[99496.126174927,106616.37284927]],\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2aNoTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2a\",\"counters\":{},\"timers\":{\"qmanager\\/index\":[99496.126174927,106616.37284927]},\"annotations\":[]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

    @Test
    public void Parse2bVersionEntry() {
        QueryLogParser data = new QueryLogParser();
        Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
        Map<String, CounterVariable> map = line.getVariables();
        assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
        ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(1));
        assertThat(vals.get(0), equalTo(2070d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

        DateTime timestamp = line.getTime();
        Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
    }

	@Test
	public void Parse2bVersionMultipleTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionBadFinalTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"13w47527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		//Should use the init timestamp in this case
		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.486 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionBadBothTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"134752768q7.486\",\"finalTimestamp\":\"13475276w87.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2bVersionBadCounter() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":\"w7\",\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(0));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionBadTimer() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,\"1w844\"]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionNoCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(1));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionNoTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(2));

		CounterVariable counter1Var = map.get("counter1");
		ArrayList<Double> vals = counter1Var.getValues();
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bVersionNoAnnotations() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070,1844]}}");
		//no annotations means no timestamp => error
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2bUsesInitTimestampAsBackup() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"counter1\":7,\"counter2\":1},\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":{\"initTimestamp\":\"1347527687.486\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.486 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2bBadCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":[\"some_counter\",8],\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2bBadTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"some_counter\":8},\"timers\":[\"/incentive/bestfor\",[2070]],\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2bBadAnnotations() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"some_counter\":8},\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":[\"initTimestamp\",\"1347527687.486\",\"finalTimestamp\",\"1347527687.686\"]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2bNoTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2b\",\"counters\":{\"some_counter\":8},\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":{}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2cVersionMultiple() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(5));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(vals.get(1), equalTo(2d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cMissingTimestampFallback() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(5));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(vals.get(1), equalTo(2d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.486 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cBadTimestampFallback() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"134q7527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(5));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(vals.get(1), equalTo(2d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.486 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cBothBadTimestamp() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527w687.486\",\"finalTimestamp\":\"134q7527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2cBadCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":[\"counter1\",[7],\"counter2\",[1]],\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2cBadAnnotations() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":[\"initTimestamp\",\"1347527687.486\",\"finalTimestamp\",\"1347527687.686\"]}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void Parse2cBadValues() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,\"2w3\"],\"gauge2\":[15]},\"counters\":{\"counter1\":[\"7w\"],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,\"18q44\"]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(5));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(0));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cVersionMissingCounters() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(vals.get(1), equalTo(2d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cVersionMissingTimers() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"gauges\":{\"gauge1\":[1,2],\"gauge2\":[15]},\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(4));

		CounterVariable counter1Var = map.get("counter1");
		ArrayList<Double> vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable gauge1Var = map.get("gauge1");
		vals = gauge1Var.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(vals.get(1), equalTo(2d));
		assertThat(gauge1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		CounterVariable gauge2Var = map.get("gauge2");
		vals = gauge2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(15d));
		assertThat(gauge2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Gauge));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cVersionMissingGauge() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]},\"annotations\":{\"initTimestamp\":\"1347527687.486\",\"finalTimestamp\":\"1347527687.686\"}}");
		assertThat(optionalLine.isPresent(), equalTo(true));
		LogLine line = optionalLine.get();
		Map<String, CounterVariable> map = line.getVariables();
		assertThat(map.size(), equalTo(3));
		CounterVariable bestForTimer = map.get("/incentive/bestfor");
		ArrayList <Double> vals = bestForTimer.getValues();
		assertThat(vals.size(), equalTo(2));
		assertThat(vals.get(0), equalTo(2070d));
		assertThat(vals.get(1), equalTo(1844d));
		assertThat(bestForTimer.getMetricKind(), equalTo(CounterVariable.MetricKind.Timer));

		CounterVariable counter1Var = map.get("counter1");
		vals = counter1Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(7d));
		assertThat(counter1Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		CounterVariable counter2Var = map.get("counter2");
		vals = counter2Var.getValues();
		assertThat(vals.size(), equalTo(1));
		assertThat(vals.get(0), equalTo(1d));
		assertThat(counter2Var.getMetricKind(), equalTo(CounterVariable.MetricKind.Counter));

		DateTime timestamp = line.getTime();
		Assert.assertEquals(new DateTime((long)(1347527687.686 * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}

	@Test
	public void Parse2cVersionMissingAnnotations() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"2c\",\"counters\":{\"counter1\":[7],\"counter2\":[1]},\"timers\":{\"/incentive/bestfor\":[2070,1844]}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}

	@Test
	public void ParseUnknownVersion() {
		QueryLogParser data = new QueryLogParser();
		Optional<LogLine> optionalLine = data.parseLogLine("{\"version\":\"86x\",\"counters\":{\"some_counter\":8},\"timers\":{\"/incentive/bestfor\":[2070]},\"annotations\":{}}");
		assertThat(optionalLine.isPresent(), equalTo(false));
	}
}
