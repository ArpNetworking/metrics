package tsdaggregator;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import org.joda.time.Period;
import tsdaggregator.publishing.AggregationPublisher;
import tsdaggregator.statistics.Statistic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes lines of data from a log file
 *
 * @author barp
 */
public class LineProcessor {
    private LogParser parser;
    private Set<Statistic> timerStatisticsClasses;
    private Set<Statistic> counterStatisticsClasses;
	private Set<Statistic> gaugeStatisticsClasses;
    private String hostName;
    private String serviceName;
    private Set<Period> periods;
    private AggregationPublisher listener;
    private ConcurrentHashMap<String, TSData> aggregations;
    static final Logger _Logger = Logger.getLogger(LineProcessor.class);
    private AggregationCloser _aggregationCloser;

    private class AggregationCloser implements Runnable {
        private volatile boolean run = true;
        private final double rotationFactor = 0.5d;
        private final int ROTATION_CHECK_MILLIS = 500;

        @Override
        public void run() {
            while (run) {
                try {
                    Thread.sleep(ROTATION_CHECK_MILLIS);
                    //_Logger.info("Checking rotations on " + aggregations.size() + " TSData objects");
                    for (Map.Entry<String, TSData> entry : aggregations.entrySet()) {
                        //_Logger.info("Check rotate on " + entry.getKey());
                        entry.getValue().checkRotate(rotationFactor);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    _Logger.error("Interrupted!", e);
                }
            }
        }

        public void shutdown() {
            run = false;
        }
    }

    public LineProcessor(LogParser parser, Set<Statistic> timerStatisticsClasses, Set<Statistic> counterStatisticsClasses, Set<Statistic> gaugeStatisticsClasses,
                         String hostName, String serviceName, Set<Period> periods, AggregationPublisher listener) {
        this.parser = parser;
        this.timerStatisticsClasses = timerStatisticsClasses;
        this.counterStatisticsClasses = counterStatisticsClasses;
		this.gaugeStatisticsClasses = gaugeStatisticsClasses;
        this.hostName = hostName;
        this.serviceName = serviceName;
        this.periods = periods;
        this.listener = listener;
        this.aggregations = new ConcurrentHashMap<>();

        startAggregationCloser();
    }

    public void shutdown() {
        _aggregationCloser.shutdown();
    }

    private void startAggregationCloser() {
        _aggregationCloser = new AggregationCloser();
        final Thread aggregationCloserThread = new Thread(_aggregationCloser);
        aggregationCloserThread.setDaemon(true);
        aggregationCloserThread.start();
    }

    public void invoke(String line) {
		Optional<LogLine> optionalData = parser.parseLogLine(line);
		if (!optionalData.isPresent()) {
			return;
		}

		LogLine data = optionalData.get();

		//Loop over all metrics
        for (Map.Entry<String, CounterVariable> entry : data.getVariables().entrySet()) {
			//Find the TSData associated with a metric
            TSData tsdata = aggregations.get(entry.getKey());
			//If the metric isn't already listed, create a new TSData for it
            if (tsdata == null) {
				switch (entry.getValue().getMetricKind()) {
					case Counter:
						tsdata = new TSData(entry.getKey(), periods, listener, hostName, serviceName, counterStatisticsClasses);
						break;
					case Timer:
						tsdata = new TSData(entry.getKey(), periods, listener, hostName, serviceName, timerStatisticsClasses);
						break;
					case Gauge:
						tsdata = new TSData(entry.getKey(), periods, listener, hostName, serviceName, gaugeStatisticsClasses);
						break;
                    default:
                        _Logger.warn("unknown metric kind, defaulting to counter statistics. metricKind = " + entry.getValue().getMetricKind().toString());
                        tsdata = new TSData(entry.getKey(), periods, listener, hostName, serviceName, counterStatisticsClasses);
                        break;
				}
                TSData returned = aggregations.putIfAbsent(entry.getKey(), tsdata);
                if (returned != null) {
                    tsdata = returned;
                }
            }
            tsdata.addMetric(entry.getValue().getValues(), data.getTime());
        }
    }

    public void closeAggregations() {
        //close all aggregations
        for (Map.Entry<String, TSData> entry : aggregations.entrySet()) {
            entry.getValue().close();
        }
    }
}
