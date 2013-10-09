package tsdaggregator;

import com.google.common.base.Charsets;
import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import tsdaggregator.statistics.Statistic;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Description goes here
 *
 * @author barp
 */
public class CommandLineParser {
	private final Option inputFileOption = Option.builder("f").argName("input_file").longOpt("file").hasArgs().desc("file to be parsed").build();
	private final Option serviceOption = Option.builder("s").argName("service").longOpt("service").hasArg().desc("service name").build();
	private final Option hostOption = Option.builder("h").argName("host").longOpt("host").hasArg().desc("host the metrics were generated on").build();
	private final Option clusterOption = Option.builder("c").argName("cluster").longOpt("cluster").hasArg().desc("name of the cluster the host is in").build();
	private final Option uriOption = Option.builder("u").argName("uri").longOpt("uri").hasArg().desc("metrics server uri").build();
	private final Option outputFileOption = Option.builder("o").argName("output_file").longOpt("output").hasArg().desc("output file").build();
	private final Option parserOption = Option.builder("p").argName("parser").longOpt("parser").hasArg().desc("parser to use to parse log lines").build();
	private final Option periodOption = Option.builder("d").argName("period").longOpt("period").hasArgs().desc("aggregation time period in ISO 8601 standard notation (multiple allowed)").build();
	private final Option counterStatisticOption = Option.builder("cs").argName("stat").longOpt("counterstat").hasArgs().desc("statistics of aggregation to record for counters (multiple allowed)").build();
	private final Option timerStatisticOption = Option.builder("ts").argName("stat").longOpt("timerstat").hasArgs().desc("statistics of aggregation to record for timers (multiple allowed)").build();
	private final Option gaugeStatisticOption = Option.builder("gs").argName("stat").longOpt("gaugestat").hasArgs().desc("statistics of aggregation to record for gauge (multiple allowed)").build();
	private final Option extensionOption = Option.builder("e").argName("extension").longOpt("extension").hasArgs().desc("extension of files to parse - uses a union of arguments as a regex (multiple allowed)").build();
	private final Option tailOption = Option.builder("l").longOpt("tail").hasArg(false).desc("\"tail\" or follow the file and do not terminate").build();
	private final Option rrdOption = Option.builder().longOpt("rrd").hasArg(false).desc("build or write to rrd databases").build();
	private final Option remetOption = Option.builder().longOpt("remet").optionalArg(true).numberOfArgs(1).argName("uri").desc("send data to a local remet server").build();
	private final Option monitordOption = Option.builder().longOpt("monitord").optionalArg(true).numberOfArgs(1).argName("uri").desc("send data to a monitord server").build();
    private final Option clusterAgg = Option.builder().longOpt("aggserver").optionalArg(true).numberOfArgs(1).argName("port").desc("starts the cluster-level aggregation server").build();
	private final Options options = new Options();

	private final HostResolver hostResolver;

	public CommandLineParser(HostResolver hostResolver) {
		options.addOption(inputFileOption);
		options.addOption(serviceOption);
		options.addOption(hostOption);
		options.addOption(clusterOption);
		options.addOption(uriOption);
		options.addOption(outputFileOption);
		options.addOption(parserOption);
		options.addOption(periodOption);
		options.addOption(timerStatisticOption);
		options.addOption(counterStatisticOption);
		options.addOption(gaugeStatisticOption);
		options.addOption(extensionOption);
		options.addOption(tailOption);
		options.addOption(rrdOption);
		options.addOption(remetOption);
		options.addOption(monitordOption);
        options.addOption(clusterAgg);
		this.hostResolver = hostResolver;
	}

	public Configuration parse(String[] args) throws ConfigException {
		org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
		CommandLine cl;
		Configuration.Builder builder = Configuration.builder();
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			throw new ConfigException("Error parsing command line args", e);
		}

		if (!cl.hasOption(inputFileOption.getLongOpt())) {
			throw new ConfigException("no file found, must specify file on the command line");
		}

		if (!cl.hasOption(serviceOption.getLongOpt())) {
			throw new ConfigException("service name must be specified");
		}

		if (!cl.hasOption(uriOption.getLongOpt()) && !cl.hasOption(outputFileOption.getLongOpt()) &&
				!cl.hasOption(remetOption.getLongOpt()) && !cl.hasOption(monitordOption.getLongOpt()) &&
				!cl.hasOption(rrdOption.getLongOpt())) {
			throw new ConfigException("no output mode specified");
		}

		if (cl.hasOption(parserOption.getLongOpt())) {
			String lineParser = cl.getOptionValue(parserOption.getLongOpt());
			Class parserClass;
			try {
				parserClass = Class.forName(lineParser);
				if (LogParser.class.isAssignableFrom(parserClass)) {
					@SuppressWarnings("unchecked")
					Class<LogParser> typedParserClass = (Class<LogParser>)parserClass;
					builder.parser(typedParserClass);
				} else {
					throw new ConfigException("parser class [" + lineParser + "] does not implement required LogParser interface");
				}
			} catch (ClassNotFoundException ex) {
				throw new ConfigException("could not find parser class [" + lineParser + "] on classpath");
			}
		}

		if (cl.hasOption(remetOption.getLongOpt())) {
			builder.useRemet(true);
			String remetUri = cl.getOptionValue(remetOption.getLongOpt());
			if (remetUri != null) {
				builder.remet(remetUri);
			}

		}

		if (cl.hasOption(monitordOption.getLongOpt())) {
			builder.useMonitord(true);
			String monitordUri = cl.getOptionValue(monitordOption.getLongOpt());
			if (monitordUri != null) {
				builder.monitord(monitordUri);
			}
		}

		if (cl.hasOption(rrdOption.getLongOpt())) {
			builder.rrd();
		}

		if (cl.hasOption(uriOption.getLongOpt())) {
			builder.metricsUri(cl.getOptionValue(uriOption.getLongOpt()));
		}


		if (cl.hasOption(outputFileOption.getLongOpt())) {
			builder.outputFile(cl.getOptionValue(outputFileOption.getLongOpt()));
		}

		if (cl.hasOption(tailOption.getLongOpt())) {
			builder.tail();
		}

        if (cl.hasOption(clusterAgg.getLongOpt())) {
            builder.clusterAgg();
            String portString = cl.getOptionValue(clusterAgg.getLongOpt());
            if (portString != null) {
                try {
                int port = Integer.parseInt(portString);
                    builder.clusterAggPort(port);
                } catch (NumberFormatException e) {
                    throw new ConfigException("cluster aggregation port not an integer as expected");
                }
            }
        }

		String[] files = cl.getOptionValues(inputFileOption.getLongOpt());
		builder.files(files);

		builder.periods(getPeriods(cl));
		builder.filterPattern(getPattern(cl));

		builder.counterStats(getStatistics(cl, counterStatisticOption, Configuration.Builder.DEFAULT_COUNTER_STATS));
		builder.timerStats(getStatistics(cl, timerStatisticOption, Configuration.Builder.DEFAULT_TIMER_STATS));
		builder.gaugeStats(getStatistics(cl, gaugeStatisticOption, Configuration.Builder.DEFAULT_GAGE_STATS));

		builder.clusterName(cl.getOptionValue(clusterOption.getLongOpt()));
		builder.serviceName(cl.getOptionValue(serviceOption.getLongOpt()));
		builder.hostName(getHostName(cl));

		return builder.create();
	}

	private String getHostName(CommandLine cl) throws ConfigException {
		String hostName = cl.getOptionValue(hostOption.getLongOpt());
		if (!cl.hasOption(hostOption.getLongOpt())) {
			try {
				hostName = hostResolver.getLocalHostName();
			} catch (UnknownHostException e) {
				throw new ConfigException("host name not specified and could not determine hostname automatically, please specify explicitly");
			}
		}
		return hostName;
	}

	private Set<Statistic> getStatistics(CommandLine cl, Option statisticOption, Set<Statistic> defaultStats) throws ConfigException {
		Set<Statistic> statsClasses = new HashSet<>();
		if (cl.hasOption(statisticOption.getLongOpt())) {
			String[] statisticsStrings = cl.getOptionValues(statisticOption.getLongOpt());
			buildStats(statsClasses, statisticsStrings);
		} else {
			statsClasses = defaultStats;
		}
		return statsClasses;
	}

	private Pattern getPattern(CommandLine cl) {
		Pattern filter = Pattern.compile(".*");
		if (cl.hasOption(extensionOption.getLongOpt())) {
			String[] filters = cl.getOptionValues(extensionOption.getLongOpt());
			StringBuilder builder = new StringBuilder();
			int x = 0;
			for (String f : filters) {
				String filterPart = buildFilter(f);
				if (x > 0) {
					builder.append("||");
				}
				builder.append("(").append(filterPart).append(")");
				x++;
			}
			filter = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
		}
		return filter;
	}

	private String buildFilter(String filter) {
		final String regexTrigger = "*+[]{}$^()|";
		boolean treatAsRegex = false;
		for (char c : regexTrigger.toCharArray()) {
			if (filter.indexOf(c) >= 0) {
				treatAsRegex = true;
				break;
			}
		}

		if (!treatAsRegex) {
			return ".*" + filter.replace(".", "\\.") + ".*";
		} else {
			return filter;
		}

	}

	private static void buildStats(Set<Statistic> statsClasses, String[] statisticsStrings) throws ConfigException {
		for (String statString : statisticsStrings) {
			try {
				Class statClass;
				if (Configuration.Builder.STATISTIC_MAP.containsKey(statString)) {
					statClass = Configuration.Builder.STATISTIC_MAP.get(statString);
				} else {
					statClass = ClassLoader.getSystemClassLoader().loadClass(statString);
				}
				Statistic stat;
				if (!Statistic.class.isAssignableFrom(statClass)) {
					final String error = "Statistic class [" + statString + "] does not implement required Statistic interface";
					throw new ConfigException(error);
				}
				try {
					stat = (Statistic)statClass.newInstance();
					statsClasses.add(stat);
				} catch (InstantiationException | IllegalAccessException ex) {
					final String error = "Could not instantiate statistic [" + statString + "]";
					throw new ConfigException(error, ex);
				}
			} catch (ClassNotFoundException ex) {
				final String error = "could not find statistic class [" + statString + "] on classpath";
				throw new ConfigException(error, ex);
			}
		}
	}

	private Set<Period> getPeriods(CommandLine cl) {
		List<String> periodOptions = new ArrayList<>();
		periodOptions.add("PT5M");
		if (cl.hasOption(remetOption.getLongOpt())) {
			periodOptions.add("PT1S");
		}

		if (cl.hasOption(periodOption.getLongOpt())) {
			periodOptions = Arrays.asList(cl.getOptionValues(periodOption.getLongOpt()));
		}

		Set<Period> periods = new HashSet<>();
		PeriodFormatter periodParser = ISOPeriodFormat.standard();
		for (String p : periodOptions) {
			periods.add(periodParser.parsePeriod(p));
		}
		return periods;
	}

	public void printUsage(OutputStream stream) {
		HelpFormatter formatter = new HelpFormatter();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(stream, Charsets.UTF_8));
		formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, "tsdaggregator", null,
				options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null, true);
		pw.flush();
	}
}
