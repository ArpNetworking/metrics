package tsdaggregator;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import org.joda.time.Period;
import tsdaggregator.publishing.AggregationPublisher;
import tsdaggregator.statistics.Statistic;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/14/12
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
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
    private Map<String, TSData> aggregations;
    static final Logger _Logger = Logger.getLogger(LineProcessor.class);

    public LineProcessor(LogParser parser, Set<Statistic> timerStatisticsClasses, Set<Statistic> counterStatisticsClasses, Set<Statistic> gaugeStatisticsClasses,
                         String hostName, String serviceName, Set<Period> periods, AggregationPublisher listener, Map<String, TSData> aggregations) {
        this.parser = parser;
        this.timerStatisticsClasses = timerStatisticsClasses;
        this.counterStatisticsClasses = counterStatisticsClasses;
		this.gaugeStatisticsClasses = gaugeStatisticsClasses;
        this.hostName = hostName;
        this.serviceName = serviceName;
        this.periods = periods;
        this.listener = listener;
        this.aggregations = aggregations;
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
                aggregations.put(entry.getKey(), tsdata);
            }
            tsdata.addMetric(entry.getValue().getValues(), data.getTime());
        }
    }
}
