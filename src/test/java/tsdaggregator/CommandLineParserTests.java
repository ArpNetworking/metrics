package tsdaggregator;

import org.hamcrest.Matcher;
import org.joda.time.Period;
import org.junit.Test;
import tsdaggregator.statistics.Statistic;
import tsdaggregator.statistics.TP0;
import tsdaggregator.statistics.TP100;
import tsdaggregator.util.InitializeExceptionStatistic;

import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

/**
 * Tests for the command line parser
 *
 * @author barp
 */
public class CommandLineParserTests {
	private static class TestResolver implements HostResolver {

		@Override
		public String getLocalHostName() throws UnknownHostException {
			return "testBox";
		}
	}

	TestResolver testResolver = new TestResolver();

	@Test(expected = ConfigException.class)
	public void testErrorEmptyCommandLine() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		parser.parse(new String[0]);
	}

	@Test(expected = ConfigException.class)
	public void testInvalidOptions() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"--somebadarg"};
		parser.parse(args);
	}

	@Test
	public void testBasicArgsRemet() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--remet", "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.useRemet(), equalTo(true));
		assertThat(config.shouldTailFiles(), equalTo(true));
	}

	@Test
	public void testBasicArgsRemetUri() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String uri = "http://remet-uri.org/";
		String[] args = new String[]{"-f", "somefile.log", "--remet", uri, "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.useRemet(), equalTo(true));
		assertThat(config.getRemetAddress(), equalTo(uri));
	}

	@Test
	public void testBasicArgsMonitord() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--monitord", "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.shouldUseMonitord(), equalTo(true));
	}

	@Test
	public void testBasicArgsMonitordUri() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String uri = "http://monitord.uri.com/";
		String[] args = new String[]{"-f", "somefile.log", "--monitord", uri, "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.shouldUseMonitord(), equalTo(true));
		assertThat(config.getMonitordAddress(), equalTo(uri));
	}

	@Test(expected = ConfigException.class)
	public void testBasicArgsOutputFileMissingArg() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "-o", "-s", "service"};
		parser.parse(args);
	}

	@Test
	public void testBasicArgsOutputFile() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String outputFile = "output.file.txt";
		String[] args = new String[]{"-f", "somefile.log", "-o", outputFile, "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.getOutputFile(), equalTo(outputFile));
	}

	@Test
	public void testBasicArgsOutputUrl() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String uri = "http://localhost:1891";
		String[] args = new String[]{"-f", "somefile.log", "-u", uri, "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.getMetricsUri(), equalTo(uri));
	}

	@Test(expected = ConfigException.class)
	public void testBasicArgsOutputUrlMissing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "-u", "-s", "service"};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testBasicArgsInputFileMissing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "-o", "some.output.txt", "-s", "service"};
		parser.parse(args);
	}

	@Test
	public void testBasicArgsOutputRrd() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service"};
		Configuration config = parser.parse(args);
		assertThat(config.shouldUseRRD(), equalTo(true));
	}

	@Test(expected = ConfigException.class)
	public void testMissingOutputMethod() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "-s", "service"};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testMissingServiceName() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--remet"};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testParserMissing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--remet", "-s", "service", "-p"};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testParserNonExistentClass() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--remet", "-s", "service", "-p", "some.non.existing.class"};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testParserNonParserClass() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--remet", "-s", "service", "-p", String.class.getCanonicalName()};
		parser.parse(args);
	}

	@Test
	public void testParserValid() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-p", QueryLogParser.class.getCanonicalName()};
		Configuration config = parser.parse(args);
		assertThat(config.getParserClass(), org.hamcrest.Matchers.typeCompatibleWith(QueryLogParser.class));
	}

	@Test(expected = ConfigException.class)
	public void testHostNameMissing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-h"};
		parser.parse(args);
	}

	@Test
	public void testHostName() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String hostName = "my.host.name-1";
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-h", hostName};
		Configuration config = parser.parse(args);
		assertThat(config.getHostName(), equalTo(hostName));
	}

	@Test(expected = ConfigException.class)
	public void testHostResolveException() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(new HostResolver() {
			@Override
			public String getLocalHostName() throws UnknownHostException {
				throw new UnknownHostException();
			}
		});
		String[] args = new String[]{"-f", "somefile.log", "--remet", "-s", "service"};
		parser.parse(args);
	}

	@Test
	public void testExtension() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-e", ".txt"};
		Configuration config = parser.parse(args);
		assertThat(config.getFilterPattern().matcher("some.file.txt").matches(), is(true));
		assertThat(config.getFilterPattern().matcher("some.file.nottxt").matches(), is(false));
	}

	@Test
	public void testExtensionRegex() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-e", "^some\\.(.*)\\.txt$"};
		Configuration config = parser.parse(args);
		assertThat(config.getFilterPattern().matcher("some.file.txt").matches(), is(true));
		assertThat(config.getFilterPattern().matcher("some.file.txt2").matches(), is(false));
	}

	@Test
	public void testExtensionMultiple() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-e", ".txt", "-e", ".csv"};
		Configuration config = parser.parse(args);
		assertThat(config.getFilterPattern().matcher("some.file.csv").matches(), is(true));
		assertThat(config.getFilterPattern().matcher("some.file.txt").matches(), is(true));
		assertThat(config.getFilterPattern().matcher("some.file.xml").matches(), is(false));
	}

	@Test
	public void testPeriodParsing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-d", "PT10M"};
		Configuration config = parser.parse(args);
		Set<Period> periods = config.getPeriods();
		assertThat(periods.size(), equalTo(1));
		assertThat(periods, hasItem(Period.minutes(10)));
	}

	@Test
	public void testMultiplePeriodParsing() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-d", "PT10M", "PT20M"};
		Configuration config = parser.parse(args);
		Set<Period> periods = config.getPeriods();
		assertThat(periods.size(), equalTo(2));
		assertThat(periods, hasItem(Period.minutes(10)));
		assertThat(periods, hasItem(Period.minutes(20)));
	}

	@Test
	public void testGaugeStats() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-gs", "min", "max"};
		Configuration config = parser.parse(args);
		Set<Statistic> stats = config.getGaugeStatistics();
		assertThat(stats.size(), equalTo(2));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP100.class)));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP0.class)));
	}

	@Test
	public void testTimerStats() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-ts", "min", "max"};
		Configuration config = parser.parse(args);
		Set<Statistic> stats = config.getTimerStatstics();
		assertThat(stats.size(), equalTo(2));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP100.class)));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP0.class)));
	}

	@Test
	public void testCounterStats() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-cs", "min", "max"};
		Configuration config = parser.parse(args);
		Set<Statistic> stats = config.getCounterStatistics();
		assertThat(stats.size(), equalTo(2));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP100.class)));
		assertThat(stats, hasItem((Matcher)org.hamcrest.Matchers.is(TP0.class)));
	}

	@Test(expected = ConfigException.class)
	public void testStatInitializationProblem() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-gs", InitializeExceptionStatistic.class.getCanonicalName()};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testBadStatistic() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-gs", String.class.getCanonicalName()};
		parser.parse(args);
	}

	@Test(expected = ConfigException.class)
	public void testNonExistentStatistic() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "--rrd", "-s", "service", "-gs", "some.class.that.doesnt.exist"};
		parser.parse(args);
	}

	@Test
	public void testPrintUsage() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		parser.printUsage(stream);
	}

	@Test
	public void testBasicArgsOutputFileTail() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		String[] args = new String[]{"-f", "somefile.log", "-o", "output.file.txt", "-s", "service", "-l"};
		Configuration config = parser.parse(args);
		assertThat(config.shouldTailFiles(), equalTo(true));
	}

	@Test
	public void testBasicArgsInputFile() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String inputFile = "somefile.log";
		String[] args = new String[]{"-f", inputFile, "-o", "output.file.txt", "-s", "service"};
		Configuration config = parser.parse(args);
		List<String> files = config.getFiles();
		assertThat(files.size(), equalTo(1));
		assertThat(files, hasItem(inputFile));
	}

	@Test
	public void testBasicArgsInputFiles() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String inputFile = "somefile.log";
		final String inputFile2 = "anotherfile.log";
		String[] args = new String[]{"-f", inputFile, inputFile2, "-o", "output.file.txt", "-s", "service"};
		Configuration config = parser.parse(args);
		List<String> files = config.getFiles();
		assertThat(files.size(), equalTo(2));
		assertThat(files, hasItem(inputFile));
		assertThat(files, hasItem(inputFile2));
	}

	@Test
	public void testBasicArgsServiceName() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String serviceName = "service_name";
		String[] args = new String[]{"-f", "somefile.log", "-o", "output.file.txt", "-s", serviceName};
		Configuration config = parser.parse(args);
		assertThat(config.getServiceName(), equalTo(serviceName));
	}

	@Test
	public void testBasicArgsClusterName() throws ConfigException {
		CommandLineParser parser = new CommandLineParser(testResolver);
		final String clusterName = "cluster_name";
		String[] args = new String[]{"-f", "somefile.log", "-o", "output.file.txt", "-s", "service", "-c", clusterName};
		Configuration config = parser.parse(args);
		assertThat(config.getClusterName(), equalTo(clusterName));
	}
}
