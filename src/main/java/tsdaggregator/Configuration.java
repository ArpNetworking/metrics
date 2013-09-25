package tsdaggregator;

import org.joda.time.Period;
import tsdaggregator.statistics.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Configuration for a set of files
 *
 * @author barp
 */
public class Configuration {
	private final boolean useRemet;
	private final String remetAddress;
	private final String[] files;
	private final Class<? extends LogParser> parserClass;
	private final Set<Period> periods;
	private final Pattern filterPattern;
	private final boolean tailFiles;
	private final Set<Statistic> timerStatstics;
	private final Set<Statistic> counterStatistics;
	private final Set<Statistic> gaugeStatistics;
	private final String serviceName;
	private final String clusterName;
	private final String hostName;
	private final boolean useMonitord;
	private final String monitordAddress;
	private final boolean useRRD;
	private final String metricsUri;
	private final String outputFile;

	protected Configuration(Builder builder) {
		useRemet = builder.shouldUseRemet();
		remetAddress = builder.getRemetAddress();
		files = builder.getFiles();
		parserClass = builder.getParserClass();
		periods = builder.getPeriods();
		filterPattern = builder.getFilterPattern();
		tailFiles = builder.shouldTail();
		timerStatstics = builder.getTimerStatistics();
		counterStatistics = builder.getCounterStatistics();
		gaugeStatistics = builder.getGaugeStatistics();
		serviceName = builder.getServiceName();
		clusterName = builder.getClusterName();
		hostName = builder.getHostName();
		useMonitord = builder.shouldUseMonitord();
		monitordAddress = builder.getMonitordAddress();
		useRRD = builder.shouldUseRRD();
		metricsUri = builder.getMetricsUri();
		outputFile = builder.getOutputFile();
	}

	public static Builder builder() {
		return new Builder();
	}

	public Set<Statistic> getTimerStatstics() {
		return timerStatstics;
	}

	public Set<Statistic> getCounterStatistics() {
		return counterStatistics;
	}

	public Set<Statistic> getGaugeStatistics() {
		return gaugeStatistics;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getHostName() {
		return hostName;
	}

	public boolean shouldUseMonitord() {
		return useMonitord;
	}

	public String getMonitordAddress() {
		return monitordAddress;
	}

	public boolean shouldUseRRD() {
		return useRRD;
	}

	public boolean useRemet() {
		return useRemet;
	}

	public String getMetricsUri() {
		return metricsUri;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public String getRemetAddress() {
		return remetAddress;
	}

	public List<String> getFiles() {
        List<String> files = Arrays.asList(this.files);

		return files;
	}

	public Class<? extends LogParser> getParserClass() {
		return parserClass;
	}

	public Pattern getFilterPattern() {
		return filterPattern;
	}

	public Set<Period> getPeriods() {
		return periods;
	}

	public boolean shouldTailFiles() {
		return tailFiles;
	}

	public static class Builder {
		public static final Set<Statistic> DEFAULT_COUNTER_STATS = Collections.unmodifiableSet(new HashSet<Statistic>() {{
			add(new MeanStatistic());
			add(new SumStatistic());
			add(new NStatistic());
		}});
		public static final Set<Statistic> DEFAULT_TIMER_STATS = Collections.unmodifiableSet(new HashSet<Statistic>() {{
			add(new TP50());
			add(new TP99());
			add(new MeanStatistic());
			add(new NStatistic());
		}});
		public static final Set<Statistic> DEFAULT_GAGE_STATS = Collections.unmodifiableSet(new HashSet<Statistic>() {{
			add(new TP0());
			add(new TP100());
			add(new MeanStatistic());
		}});
		static final Map<String, Class<? extends Statistic>> STATISTIC_MAP = new HashMap<String, Class<? extends Statistic>>() {{
			put("n", NStatistic.class);
			put("mean", MeanStatistic.class);
			put("sum", SumStatistic.class);
			put("p0", TP0.class);
			put("min", TP0.class);
			put("p100", TP100.class);
			put("max", TP100.class);
			put("p50", TP50.class);
			put("median", TP50.class);
			put("p90", TP90.class);
			put("p99", TP99.class);
			put("p99.9", TP99p9.class);
			put("p999", TP99p9.class);
			put("first", FirstStatistic.class);
			put("last", LastStatistic.class);
		}};
		private static final String MONITORD_DEFAULT_URI = "http://monitord:8080/results";
		private static final String REMET_DEFAULT_URI = "http://localhost:7090/report";
		private boolean useRemet = false;
		private String remetAddress = REMET_DEFAULT_URI;
		private boolean useMonitord = false;
		private String monitordAddress = MONITORD_DEFAULT_URI;
		private ArrayList<String> files = new ArrayList<>();
		private Class<? extends LogParser> parserClass = QueryLogParser.class;
		private Set<Period> periods = new TreeSet<>();
		private Pattern filterPattern = Pattern.compile(".*");
		private boolean shouldTail = false;
		private Set<Statistic> timerStatistics;
		private Set<Statistic> counterStatistics;
		private Set<Statistic> gaugeStatistics;
		private String clusterName = "";
		private String serviceName = "";
		private String hostName = "";
		private boolean useRRD = false;
		private String metricsUri = "";
		private String outputFile = "";

		private Builder() {
		}

		public String getMonitordAddress() {
			return monitordAddress;
		}

		public String getMetricsUri() {
			return metricsUri;
		}

		public String getOutputFile() {
			return outputFile;
		}

		public boolean shouldUseMonitord() {
			return useMonitord;
		}

		public String getClusterName() {
			return clusterName;
		}

		public String getServiceName() {
			return serviceName;
		}

		public boolean shouldUseRemet() {
			return useRemet;
		}

		public boolean shouldUseRRD() {
			return useRRD;
		}

		public String getRemetAddress() {
			return remetAddress;
		}

		public String[] getFiles() {
			return files.toArray(new String[0]);
		}

		public Class<? extends LogParser> getParserClass() {
			return parserClass;
		}

		public Set<Period> getPeriods() {
			return periods;
		}

		public Pattern getFilterPattern() {
			return filterPattern;
		}

		public boolean shouldTail() {
			return shouldTail || useRemet;
		}

		public Set<Statistic> getTimerStatistics() {
			return timerStatistics;
		}

		public Set<Statistic> getCounterStatistics() {
			return counterStatistics;
		}

		public Configuration create() {
			Configuration conf =  new Configuration(this);
			return conf;
		}

		public Builder remet(String address) {
			useRemet = true;
			remetAddress = address;
			return this;
		}

		public Builder useRemet(boolean use) {
			useRemet = use;
			return this;
		}

		public Builder monitord(String address) {
			useMonitord = true;
			monitordAddress = address;
			return this;
		}

		public Builder useMonitord(boolean use) {
			useMonitord = use;
			return this;
		}

		public Builder files(String[] files) {
			this.files = new ArrayList<>();
			for (String file : files) {
				this.files.add(file);
			}
			return this;
		}

		public Builder addFile(String file) {
			this.files.add(file);
			return this;
		}

		public Builder parser(Class<? extends LogParser> parserClass) {
			this.parserClass = parserClass;
			return this;
		}

		public Builder periods(Set<Period> periods) {
			this.periods = periods;
			return this;
		}

		public Set<Statistic> getGaugeStatistics() {
			return gaugeStatistics;
		}

		public Builder filterPattern(Pattern pattern) {
			this.filterPattern = pattern;
			return this;
		}

		public Builder tail() {
			tail(true);
			return this;
		}

		public Builder tail(boolean shouldTail) {
			this.shouldTail = shouldTail;
			return this;
		}

		public Builder timerStats(Set<Statistic> stats) {
			this.timerStatistics = stats;
			return this;
		}

		public Builder counterStats(Set<Statistic> stats) {
			this.counterStatistics = stats;
			return this;
		}

		public Builder gaugeStats(Set<Statistic> stats) {
			this.gaugeStatistics = stats;
			return this;
		}

		public Builder serviceName(String name) {
			this.serviceName = name;
			return this;
		}

		public Builder clusterName(String name) {
			this.clusterName = name;
			return this;
		}

		public String getHostName() {
			return hostName;
		}

		public Builder hostName(String name) {
			this.hostName = name;
			return this;
		}

		public Builder rrd() {
			this.useRRD = true;
			return this;
		}

		public Builder outputFile(String file) {
			this.outputFile = file;
			return this;
		}

		public Builder metricsUri(String uri) {
			this.metricsUri = uri;
			return this;
		}
	}
}
