package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.statistics.*;
import org.joda.time.Period;

import java.util.*;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Configuration for a set of files.
 *
 * @author barp
 */
public class Configuration {
    private final boolean _useRemet;
    private final String _remetAddress;
    @Nonnull
    private final String[] _files;
    private final Class<? extends LogParser> _parserClass;
    private final Set<Period> _periods;
    private final Pattern _filterPattern;
    private final boolean _tailFiles;
    private final Set<Statistic> _timerStatistics;
    private final Set<Statistic> _counterStatistics;
    private final Set<Statistic> _gaugeStatistics;
    private final String _serviceName;
    private final String _clusterName;
    private final String _hostName;
    private final boolean _useMonitord;
    private final String _monitordAddress;
    private final boolean _useRRD;
    private final String _metricsUri;
    private final String _outputFile;
    private final boolean _startClusterAggServer;
    private final int _clusterAggServerPort;
    private final String _clusterAggHost;

    public String getClusterAggHost() {
        return _clusterAggHost;
    }

    private Configuration(@Nonnull Builder builder) {
        _useRemet = builder.shouldUseRemet();
        _remetAddress = builder.getRemetAddress();
        _files = builder.getFiles();
        _parserClass = builder.getParserClass();
        _periods = builder.getPeriods();
        _filterPattern = builder.getFilterPattern();
        _tailFiles = builder.shouldTail();
        _timerStatistics = builder.getTimerStatistics();
        _counterStatistics = builder.getCounterStatistics();
        _gaugeStatistics = builder.getGaugeStatistics();
        _serviceName = builder.getServiceName();
        _clusterName = builder.getClusterName();
        _hostName = builder.getHostName();
        _useMonitord = builder.shouldUseMonitord();
        _monitordAddress = builder.getMonitordAddress();
        _useRRD = builder.shouldUseRRD();
        _metricsUri = builder.getMetricsUri();
        _outputFile = builder.getOutputFile();
        _startClusterAggServer = builder.isClusterAggServer();
        _clusterAggServerPort = builder.getClusterAggPort();
        _clusterAggHost = builder.getClusterAggHost();
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public Set<Statistic> getTimerStatistics() {
        return _timerStatistics;
    }

    public Set<Statistic> getCounterStatistics() {
        return _counterStatistics;
    }

    public Set<Statistic> getGaugeStatistics() {
        return _gaugeStatistics;
    }

    public String getServiceName() {
        return _serviceName;
    }

    public String getClusterName() {
        return _clusterName;
    }

    public String getHostName() {
        return _hostName;
    }

    public boolean shouldUseMonitord() {
        return _useMonitord;
    }

    public String getMonitordAddress() {
        return _monitordAddress;
    }

    public boolean shouldUseRRD() {
        return _useRRD;
    }

    public boolean useRemet() {
        return _useRemet;
    }

    public String getMetricsUri() {
        return _metricsUri;
    }

    public String getOutputFile() {
        return _outputFile;
    }

    public String getRemetAddress() {
        return _remetAddress;
    }

    @Nonnull
    public List<String> getFiles() {
        return Arrays.asList(this._files);
    }

    public Class<? extends LogParser> getParserClass() {
        return _parserClass;
    }

    public Pattern getFilterPattern() {
        return _filterPattern;
    }

    public Set<Period> getPeriods() {
        return _periods;
    }

    public boolean shouldTailFiles() {
        return _tailFiles;
    }

    public boolean shouldStartClusterAggServer() {
        return _startClusterAggServer;
    }

    public int getClusterAggServerPort() {
        return _clusterAggServerPort;
    }

    public Boolean shouldUseUpstreamAgg() {
        return _clusterAggHost != null;
    }

    /**
     * Builder for a Configuration class.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        public static final Set<Statistic> DEFAULT_COUNTER_STATS = Collections.unmodifiableSet(
                new HashSet<Statistic>() {{
                    add(new MeanStatistic());
                    add(new SumStatistic());
                    add(new NStatistic());
                }});
        public static final Set<Statistic> DEFAULT_TIMER_STATS = Collections.unmodifiableSet(
                new HashSet<Statistic>() {{
                    add(new TP50());
                    add(new TP99());
                    add(new MeanStatistic());
                    add(new NStatistic());
                }});
        public static final Set<Statistic> DEFAULT_GAGE_STATS = Collections.unmodifiableSet(
                new HashSet<Statistic>() {{
                    add(new TP0());
                    add(new TP100());
                    add(new MeanStatistic());
                }});
        static final Map<String, Class<? extends Statistic>> STATISTIC_MAP =
                new HashMap<String, Class<? extends Statistic>>() {{
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
        private boolean _useRemet = false;
        private String _remetAddress = REMET_DEFAULT_URI;
        private boolean _useMonitord = false;
        private String _monitordAddress = MONITORD_DEFAULT_URI;
        @Nonnull
        private ArrayList<String> _files = new ArrayList<>();
        private Class<? extends LogParser> _parserClass = QueryLogParser.class;
        private Set<Period> _periods = new TreeSet<>();
        private Pattern _filterPattern = Pattern.compile(".*");
        private boolean _shouldTail = false;
        private Set<Statistic> _timerStatistics;
        private Set<Statistic> _counterStatistics;
        private Set<Statistic> _gaugeStatistics;
        private String _clusterName = "";
        private String _serviceName = "";
        private String _hostName = "";
        private boolean _useRRD = false;
        private String _metricsUri = "";
        private String _outputFile = "";
        private boolean _clusterAggServer = false;
        private int _clusterAggPort = 7065;
        private boolean _useUpstreamAgg = false;
        private String _clusterAggHost;

        private Builder() {
        }

        public String getMonitordAddress() {
            return _monitordAddress;
        }

        public String getMetricsUri() {
            return _metricsUri;
        }

        public String getOutputFile() {
            return _outputFile;
        }

        public boolean shouldUseMonitord() {
            return _useMonitord;
        }

        public String getClusterName() {
            return _clusterName;
        }

        public String getServiceName() {
            return _serviceName;
        }

        public boolean shouldUseRemet() {
            return _useRemet;
        }

        public boolean shouldUseRRD() {
            return _useRRD;
        }

        public String getRemetAddress() {
            return _remetAddress;
        }

        @Nonnull
        public String[] getFiles() {
            return _files.toArray(new String[_files.size()]);
        }

        public Class<? extends LogParser> getParserClass() {
            return _parserClass;
        }

        public Set<Period> getPeriods() {
            return _periods;
        }

        public Pattern getFilterPattern() {
            return _filterPattern;
        }

        public boolean shouldTail() {
            return _shouldTail || _useRemet;
        }

        public Set<Statistic> getTimerStatistics() {
            return _timerStatistics;
        }

        public Set<Statistic> getCounterStatistics() {
            return _counterStatistics;
        }

        @Nonnull
        public Configuration create() {
            return new Configuration(this);
        }

        @Nonnull
        public Builder remet(String address) {
            _useRemet = true;
            _remetAddress = address;
            return this;
        }

        @Nonnull
        public Builder useRemet(boolean use) {
            _useRemet = use;
            return this;
        }

        @Nonnull
        public Builder monitord(String address) {
            _useMonitord = true;
            _monitordAddress = address;
            return this;
        }

        @Nonnull
        public Builder useMonitord(boolean use) {
            _useMonitord = use;
            return this;
        }

        @Nonnull
        public Builder files(String[] files) {
            this._files = new ArrayList<>();
            Collections.addAll(this._files, files);
            return this;
        }

        @Nonnull
        public Builder addFile(String file) {
            this._files.add(file);
            return this;
        }

        @Nonnull
        public Builder parser(Class<? extends LogParser> parserClass) {
            this._parserClass = parserClass;
            return this;
        }

        @Nonnull
        public Builder periods(Set<Period> periods) {
            this._periods = periods;
            return this;
        }

        public Set<Statistic> getGaugeStatistics() {
            return _gaugeStatistics;
        }

        @Nonnull
        public Builder filterPattern(Pattern pattern) {
            this._filterPattern = pattern;
            return this;
        }

        @Nonnull
        public Builder tail() {
            tail(true);
            return this;
        }

        @Nonnull
        public Builder tail(boolean shouldTail) {
            this._shouldTail = shouldTail;
            return this;
        }

        @Nonnull
        public Builder timerStats(Set<Statistic> stats) {
            this._timerStatistics = stats;
            return this;
        }

        @Nonnull
        public Builder counterStats(Set<Statistic> stats) {
            this._counterStatistics = stats;
            return this;
        }

        @Nonnull
        public Builder gaugeStats(Set<Statistic> stats) {
            this._gaugeStatistics = stats;
            return this;
        }

        @Nonnull
        public Builder serviceName(String name) {
            this._serviceName = name;
            return this;
        }

        @Nonnull
        public Builder clusterName(String name) {
            this._clusterName = name;
            return this;
        }

        public String getHostName() {
            return _hostName;
        }

        @Nonnull
        public Builder hostName(String name) {
            this._hostName = name;
            return this;
        }

        @Nonnull
        public Builder rrd() {
            this._useRRD = true;
            return this;
        }

        @Nonnull
        public Builder clusterAgg() {
            this._clusterAggServer = true;
            return this;
        }

        public boolean isClusterAggServer() {
            return this._clusterAggServer;
        }

        @Nonnull
        public Builder clusterAggPort(int port) {
            this._clusterAggPort = port;
            return this;
        }

        public int getClusterAggPort() {
            return this._clusterAggPort;
        }

        @Nonnull
        public Builder outputFile(String file) {
            this._outputFile = file;
            return this;
        }

        @Nonnull
        public Builder metricsUri(String uri) {
            this._metricsUri = uri;
            return this;
        }

        @Nonnull
        public Builder aggHost(String aggregationHost) {
            this._clusterAggHost = aggregationHost;
            this._useUpstreamAgg = true;
            return this;
        }

        public String getClusterAggHost() {
            return _clusterAggHost;
        }

        public boolean shouldUseUpstreamAgg() {
            return this._useUpstreamAgg;
        }
    }
}
