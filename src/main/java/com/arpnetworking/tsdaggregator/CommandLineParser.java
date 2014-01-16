package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Class to parse command line arguments and build a Config object from them.
 *
 * @author barp
 */
class CommandLineParser {
    private final Option _inputFileOption = Option.builder("f").argName("input_file").longOpt("file").hasArgs()
            .desc("file to be parsed").build();
    private final Option _serviceOption = Option.builder("s").argName("service").longOpt("service").hasArg()
            .desc("service name").build();
    private final Option _hostOption = Option.builder("h").argName("host").longOpt("host").hasArg()
            .desc("host the metrics were generated on").build();
    private final Option _clusterOption = Option.builder("c").argName("cluster").longOpt("cluster").hasArg()
            .desc("name of the cluster the host is in").build();
    private final Option _uriOption = Option.builder("u").argName("uri").longOpt("uri").hasArg()
            .desc("metrics server uri").build();
    private final Option _outputFileOption = Option.builder("o").argName("output_file").longOpt("output").hasArg()
            .desc("output file").build();
    private final Option _parserOption = Option.builder("p").argName("parser").longOpt("parser").hasArg()
            .desc("parser to use to parse log lines").build();
    private final Option _periodOption = Option.builder("d").argName("period").longOpt("period").hasArgs()
            .desc("aggregation time period in ISO 8601 standard notation (multiple allowed)").build();
    private final Option _counterStatisticOption = Option.builder("cs").argName("stat").longOpt("counterstat").hasArgs()
            .desc("statistics of aggregation to record for counters (multiple allowed)").build();
    private final Option _timerStatisticOption = Option.builder("ts").argName("stat").longOpt("timerstat").hasArgs()
            .desc("statistics of aggregation to record for timers (multiple allowed)").build();
    private final Option _gaugeStatisticOption = Option.builder("gs").argName("stat").longOpt("gaugestat").hasArgs()
            .desc("statistics of aggregation to record for gauge (multiple allowed)").build();
    private final Option _extensionOption = Option.builder("e").argName("extension").longOpt("extension").hasArgs()
            .desc("extension of files to parse - uses a union of arguments as a regex (multiple allowed)").build();
    private final Option _tailOption = Option.builder("l").longOpt("tail").hasArg(false)
            .desc("\"tail\" or follow the file and do not terminate").build();
    private final Option _rrdOption = Option.builder().longOpt("rrd").hasArg(false)
            .desc("build or write to rrd databases").build();
    private final Option _remetOption = Option.builder().longOpt("remet").optionalArg(true).numberOfArgs(1)
            .argName("uri").desc("send data to a local remet server").build();
    private final Option _monitordOption = Option.builder().longOpt("monitord").optionalArg(true).numberOfArgs(1)
            .argName("uri").desc("send data to a monitord server").build();
    private final Option _clusterAgg = Option.builder().longOpt("aggserver").numberOfArgs(1).optionalArg(true)
            .argName("port").desc("starts the cluster-level aggregation server").build();
    private final Option _upstreamAgg = Option.builder().longOpt("upstreamagg").hasArg()
            .argName("host").desc("send data to an upstream cluster aggregator").build();
    private final Option _configFilesOption = Option.builder().longOpt("config").hasArgs().argName("file")
            .desc("read config files for configuration sets").build();
    private final  Option _redisServer = Option.builder("r").longOpt("redis").hasArgs().argName("server")
            .desc("redis server to bootstrap agg server").build();
    private final Options _options = new Options();
    private final HostResolver _hostResolver;

    public CommandLineParser(HostResolver hostResolver) {
        _options.addOption(_inputFileOption);
        _options.addOption(_serviceOption);
        _options.addOption(_hostOption);
        _options.addOption(_clusterOption);
        _options.addOption(_uriOption);
        _options.addOption(_outputFileOption);
        _options.addOption(_parserOption);
        _options.addOption(_periodOption);
        _options.addOption(_timerStatisticOption);
        _options.addOption(_counterStatisticOption);
        _options.addOption(_gaugeStatisticOption);
        _options.addOption(_extensionOption);
        _options.addOption(_tailOption);
        _options.addOption(_rrdOption);
        _options.addOption(_remetOption);
        _options.addOption(_monitordOption);
        _options.addOption(_clusterAgg);
        _options.addOption(_upstreamAgg);
        _options.addOption(_configFilesOption);
        _options.addOption(_redisServer);
        this._hostResolver = hostResolver;
    }

    private static void buildStats(@Nonnull Set<Statistic> statsClasses, @Nonnull String[] statisticsStrings)
            throws ConfigException {
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
                    @Nonnull final String error = "Statistic class [" + statString + "] does not implement required " +
                            "Statistic interface";
                    throw new ConfigException(error);
                }
                try {
                    stat = (Statistic) statClass.newInstance();
                    statsClasses.add(stat);
                } catch (@Nonnull InstantiationException ex) {
                    @Nonnull final String error = "Could not instantiate statistic [" + statString + "]";
                    throw new ConfigException(error, ex);
                } catch (IllegalAccessException ex) {
                    @Nonnull final String error = "Could not instantiate statistic [" + statString + "]";
                    throw new ConfigException(error, ex);
                }
            } catch (ClassNotFoundException ex) {
                @Nonnull final String error = "could not find statistic class [" + statString + "] on classpath";
                throw new ConfigException(error, ex);
            }
        }
    }

    @Nonnull
    public Configuration parse(String[] args) throws ConfigException {
        @Nonnull org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
        CommandLine cl;
        @Nonnull Configuration.Builder builder = Configuration.builder();
        builder.valid(true);
        boolean isBaseConfig = false;
        try {
            cl = parser.parse(_options, args);
        } catch (ParseException e) {
            throw new ConfigException("Error parsing command line args", e);
        }

        if (cl.hasOption(_configFilesOption.getLongOpt())) {
            isBaseConfig = true;
        }

        if (!cl.hasOption(_inputFileOption.getLongOpt()) && !cl.hasOption(_clusterAgg.getLongOpt())) {
            if (!isBaseConfig) {
                throw new ConfigException(
                        "no file found, must specify file on the command line or start in cluster aggregation mode");
            }
            builder.valid(false);
        }

        if (!cl.hasOption(_serviceOption.getLongOpt()) && !cl.hasOption(_clusterAgg.getLongOpt())) {
            if (!isBaseConfig) {
                throw new ConfigException("service name must be specified");
            }
            builder.valid(false);
        }

        if (cl.hasOption(_clusterAgg.getLongOpt()) && !cl.hasOption(_redisServer.getLongOpt())) {
            throw new ConfigException("redis server must be specified if cluster aggregation mode is enabled");
        }

        if (!cl.hasOption(_uriOption.getLongOpt()) && !cl.hasOption(_outputFileOption.getLongOpt()) &&
                !cl.hasOption(_remetOption.getLongOpt()) && !cl.hasOption(_monitordOption.getLongOpt()) &&
                !cl.hasOption(_rrdOption.getLongOpt()) && !cl.hasOption(_upstreamAgg.getLongOpt())) {
            if (!isBaseConfig) {
                throw new ConfigException("no output mode specified");
            }
            builder.valid(false);
        }

        if (cl.hasOption(_parserOption.getLongOpt())) {
            String lineParser = cl.getOptionValue(_parserOption.getLongOpt());
            Class parserClass;
            try {
                parserClass = Class.forName(lineParser);
                if (LogParser.class.isAssignableFrom(parserClass)) {
                    @Nonnull @SuppressWarnings("unchecked")
                    Class<LogParser> typedParserClass = (Class<LogParser>) parserClass;
                    builder.parser(typedParserClass);
                } else {
                    throw new ConfigException("parser class [" + lineParser +
                            "] does not implement required LogParser interface");
                }
            } catch (ClassNotFoundException ex) {
                throw new ConfigException("could not find parser class [" + lineParser + "] on classpath", ex);
            }
        }

        if (cl.hasOption(_remetOption.getLongOpt())) {
            builder.useRemet(true);
            String remetUri = cl.getOptionValue(_remetOption.getLongOpt());
            if (remetUri != null) {
                builder.remet(remetUri);
            }

        }

        if (cl.hasOption(_monitordOption.getLongOpt())) {
            builder.useMonitord(true);
            String monitordUri = cl.getOptionValue(_monitordOption.getLongOpt());
            if (monitordUri != null) {
                builder.monitord(monitordUri);
            }
        }

        if (cl.hasOption(_rrdOption.getLongOpt())) {
            builder.rrd();
        }

        if (cl.hasOption(_uriOption.getLongOpt())) {
            builder.metricsUri(cl.getOptionValue(_uriOption.getLongOpt()));
        }

        if (cl.hasOption(_upstreamAgg.getLongOpt())) {
            builder.aggHost(cl.getOptionValue(_upstreamAgg.getLongOpt()));
        }


        if (cl.hasOption(_outputFileOption.getLongOpt())) {
            builder.outputFile(cl.getOptionValue(_outputFileOption.getLongOpt()));
        }

        if (cl.hasOption(_tailOption.getLongOpt())) {
            builder.tail();
        }

        if (cl.hasOption(_clusterAgg.getLongOpt())) {
            builder.clusterAgg();
            String portString = cl.getOptionValue(_clusterAgg.getLongOpt());
            if (portString != null) {
                try {
                    int port = Integer.parseInt(portString);
                    builder.clusterAggPort(port);
                } catch (NumberFormatException e) {
                    throw new ConfigException("cluster aggregation port not an integer as expected", e);
                }
            }

            builder.redisHost(cl.getOptionValues(_redisServer.getLongOpt()));
        }

        if (cl.hasOption(_inputFileOption.getLongOpt())) {
            String[] files = cl.getOptionValues(_inputFileOption.getLongOpt());
            builder.files(files);
        }

        if (cl.hasOption(_configFilesOption.getLongOpt())) {
            String[] files = cl.getOptionValues(_configFilesOption.getLongOpt());
            builder.configFiles(files);
        }

        builder.periods(getPeriods(cl));
        builder.filterPattern(getPattern(cl));

        builder.counterStats(getStatistics(cl, _counterStatisticOption, Configuration.Builder.DEFAULT_COUNTER_STATS));
        builder.timerStats(getStatistics(cl, _timerStatisticOption, Configuration.Builder.DEFAULT_TIMER_STATS));
        builder.gaugeStats(getStatistics(cl, _gaugeStatisticOption, Configuration.Builder.DEFAULT_GAGE_STATS));

        builder.clusterName(cl.getOptionValue(_clusterOption.getLongOpt()));
        builder.serviceName(cl.getOptionValue(_serviceOption.getLongOpt()));
        builder.hostName(getHostName(cl));

        return builder.create();
    }

    private String getHostName(@Nonnull CommandLine cl) throws ConfigException {
        String hostName = cl.getOptionValue(_hostOption.getLongOpt());
        if (!cl.hasOption(_hostOption.getLongOpt())) {
            try {
                hostName = _hostResolver.getLocalHostName();
            } catch (UnknownHostException e) {
                throw new ConfigException("host name not specified and could not determine hostname automatically, " +
                        "please specify explicitly", e);
            }
        }
        return hostName;
    }

    private Set<Statistic> getStatistics(@Nonnull CommandLine cl, @Nonnull Option statisticOption,
                                         Set<Statistic> defaultStats)
            throws ConfigException {
        Set<Statistic> statsClasses = Sets.newHashSet();
        if (cl.hasOption(statisticOption.getLongOpt())) {
            String[] statisticsStrings = cl.getOptionValues(statisticOption.getLongOpt());
            buildStats(statsClasses, statisticsStrings);
        } else {
            statsClasses = defaultStats;
        }
        return statsClasses;
    }

    private Pattern getPattern(@Nonnull CommandLine cl) {
        Pattern filter = Pattern.compile(".*");
        if (cl.hasOption(_extensionOption.getLongOpt())) {
            String[] filters = cl.getOptionValues(_extensionOption.getLongOpt());
            @Nonnull StringBuilder builder = new StringBuilder();
            int x = 0;
            for (@Nonnull String f : filters) {
                @Nonnull String filterPart = buildFilter(f);
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

    @Nonnull
    private String buildFilter(@Nonnull String filter) {
        @Nonnull final String regexTrigger = "*+[]{}$^()|";
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

    @Nonnull
    private Set<Period> getPeriods(@Nonnull CommandLine cl) {
        @Nonnull List<String> periodOptions = Lists.newArrayList();
        periodOptions.add("PT5M");
        if (cl.hasOption(_remetOption.getLongOpt())) {
            periodOptions.add("PT1S");
        }

        if (cl.hasOption(_periodOption.getLongOpt())) {
            periodOptions = Arrays.asList(cl.getOptionValues(_periodOption.getLongOpt()));
        }

        @Nonnull Set<Period> periods = Sets.newHashSet();
        PeriodFormatter periodParser = ISOPeriodFormat.standard();
        for (String p : periodOptions) {
            periods.add(periodParser.parsePeriod(p));
        }
        return periods;
    }

    public void printUsage(OutputStream stream) {
        @Nonnull HelpFormatter formatter = new HelpFormatter();
        @Nonnull PrintWriter pw = new PrintWriter(new OutputStreamWriter(stream, Charsets.UTF_8));
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, "tsdaggregator", null,
                _options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null, true);
        pw.flush();
    }
}
