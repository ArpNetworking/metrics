package com.arpnetworking.tsdaggregator;

import org.hamcrest.Matcher;
import org.joda.time.Period;
import org.junit.Test;
import com.arpnetworking.tsdaggregator.statistics.MeanStatistic;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.arpnetworking.tsdaggregator.statistics.TP100;
import com.arpnetworking.tsdaggregator.statistics.TP50;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the configuration builder
 *
 * @author barp
 */
public class ConfigurationBuilderTests {
	@Test
	public void TestConstruct() {
		Configuration.Builder builder = Configuration.builder();
		assertThat(builder, notNullValue());
	}

	@Test
	public void TestTail() {
		Configuration.Builder builder = Configuration.builder();
		builder.tail();
		Configuration config = builder.create();
		assertThat(config.shouldTailFiles(), equalTo(true));
		builder.tail(false);
		config = builder.create();
		assertThat(config.shouldTailFiles(), equalTo(false));
	}

	@Test
	public void TestRemetImpliesTail() {
		Configuration.Builder builder = Configuration.builder();
		builder.useRemet(true);
		Configuration config = builder.create();
		assertThat(config.shouldTailFiles(), equalTo(true));
		builder.useRemet(false);
		config = builder.create();
		assertThat(config.shouldTailFiles(), equalTo(false));
	}

	@Test
	public void TestMetricsUri() {
		Configuration.Builder builder = Configuration.builder();
		final String metricsUri = "http://some_metrics.uri.com/metrics/foo";
		builder.metricsUri(metricsUri);
		Configuration config = builder.create();
		assertThat(config.getMetricsUri(), equalTo(metricsUri));
	}

	@Test
	public void TestRemetUri() {
		Configuration.Builder builder = Configuration.builder();
		final String remetUri = "http://some_metrics.uri.com/remet/foo";
		builder.remet(remetUri);
		Configuration config = builder.create();
		assertThat(config.getRemetAddress(), equalTo(remetUri));
		assertThat(config.useRemet(), equalTo(true));
	}

	@Test
	public void TestOutputRrd() {
		Configuration.Builder builder = Configuration.builder();
		builder.rrd();
		Configuration config = builder.create();
		assertThat(config.shouldUseRRD(), equalTo(true));
	}

	@Test
	public void TestOutputFile() {
		Configuration.Builder builder = Configuration.builder();
		final String file = "some.file.txt";
		builder.outputFile(file);
		Configuration config = builder.create();
		assertThat(config.getOutputFile(), equalTo(file));
	}

	@Test
	public void TestHostName() {
		Configuration.Builder builder = Configuration.builder();
		final String host = "hostname";
		builder.hostName(host);
		Configuration config = builder.create();
		assertThat(config.getHostName(), equalTo(host));
	}

	@Test
	public void TestClusterName() {
		Configuration.Builder builder = Configuration.builder();
		final String cluster = "service_cluster";
		builder.clusterName(cluster);
		Configuration config = builder.create();
		assertThat(config.getClusterName(), equalTo(cluster));
	}

	@Test
	public void TestServiceName() {
		Configuration.Builder builder = Configuration.builder();
		final String serviceName = "tsdaggregator";
		builder.serviceName(serviceName);
		Configuration config = builder.create();
		assertThat(config.getServiceName(), equalTo(serviceName));
	}

	@Test
	public void TestGaugeStats() {
		Configuration.Builder builder = Configuration.builder();
		Set<Statistic> stats = new HashSet<>();
		stats.add(new TP100());
		stats.add(new TP50());
		builder.gaugeStats(stats);
		Configuration config = builder.create();
		Set<Statistic> returnedStats = config.getGaugeStatistics();
		assertThat(returnedStats.size(), equalTo(2));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(TP100.class)));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(TP50.class)));
	}

	@Test
	public void TestCounterStats() {
		Configuration.Builder builder = Configuration.builder();
		Set<Statistic> stats = new HashSet<>();
		stats.add(new MeanStatistic());
		stats.add(new TP50());
		builder.counterStats(stats);
		Configuration config = builder.create();
		Set<Statistic> returnedStats = config.getCounterStatistics();
		assertThat(returnedStats.size(), equalTo(2));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(MeanStatistic.class)));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(TP50.class)));
	}

	@Test
	public void TestTimerStats() {
		Configuration.Builder builder = Configuration.builder();
		Set<Statistic> stats = new HashSet<>();
		stats.add(new MeanStatistic());
		stats.add(new TP50());
		builder.timerStats(stats);
		Configuration config = builder.create();
		Set<Statistic> returnedStats = config.getTimerStatistics();
		assertThat(returnedStats.size(), equalTo(2));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(MeanStatistic.class)));
		assertThat(returnedStats, hasItem((Matcher) org.hamcrest.Matchers.is(TP50.class)));
	}

	@Test
	public void TestPeriods() {
		Configuration.Builder builder = Configuration.builder();
		Set<Period> periods = new HashSet<>();
		periods.add(Period.minutes(8));
		periods.add(Period.minutes(15));
		builder.periods(periods);
		Configuration config = builder.create();
		Set<Period> returnedPeriods = config.getPeriods();
		assertThat(returnedPeriods.size(), equalTo(2));
		assertThat(returnedPeriods, hasItem(Period.minutes(8)));
		assertThat(returnedPeriods, hasItem(Period.minutes(15)));
	}

	@Test
	public void TestParserClass() {
		Configuration.Builder builder = Configuration.builder();
		Class<? extends LogParser> parser = QueryLogParser.class;
		builder.parser(parser);
		Configuration config = builder.create();
		assertThat(config.getParserClass(), (Matcher)equalTo(QueryLogParser.class));
	}

	@Test
	public void TestMonitord() {
		Configuration.Builder builder = Configuration.builder();
		final String monitordUri = "http://monitord.com/report/something";
		builder.useMonitord(true);
		Configuration config = builder.create();
		assertThat(config.shouldUseMonitord(), equalTo(true));

		builder.useMonitord(false);
		config = builder.create();
		assertThat(config.shouldUseMonitord(), equalTo(false));

		builder.monitord(monitordUri);
		config = builder.create();
		assertThat(config.getMonitordAddress(), equalTo(monitordUri));
		assertThat(config.shouldUseMonitord(), equalTo(true));
	}

	@Test
	public void TestFilterPattern() {
		Configuration.Builder builder = Configuration.builder();
		final Pattern pattern = Pattern.compile(".*\\.txt");
		builder.filterPattern(pattern);
		Configuration config = builder.create();
		assertThat(config.getFilterPattern(), equalTo(pattern));
	}

	@Test
	public void TestFiles() {
		Configuration.Builder builder = Configuration.builder();
		final String[] files = new String[] {"file1", "file2"};
		builder.files(files);
		Configuration config = builder.create();
		List<String> returnedFiles = config.getFiles();

		assertThat(returnedFiles, hasItem("file1"));
		assertThat(returnedFiles, hasItem("file2"));

		builder.addFile("file3");
		config = builder.create();
		returnedFiles = config.getFiles();
		assertThat(returnedFiles, hasItem("file1"));
		assertThat(returnedFiles, hasItem("file2"));
		assertThat(returnedFiles, hasItem("file3"));
	}
}
