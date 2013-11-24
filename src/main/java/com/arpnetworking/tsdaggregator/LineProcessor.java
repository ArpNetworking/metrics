package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import org.joda.time.Period;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Processes lines of data from a log file.
 *
 * @author barp
 */
public class LineProcessor {
    private final LogParser _parser;
    private final Set<Statistic> _timerStatisticsClasses;
    private final Set<Statistic> _counterStatisticsClasses;
    private final Set<Statistic> _gaugeStatisticsClasses;
    private final String _hostName;
    private final String _serviceName;
    private final Set<Period> _periods;
    private final AggregationPublisher _listener;
    @Nonnull
    private final ConcurrentHashMap<String, TSData> _aggregations;
    private AggregationCloser _aggregationCloser;
    private static final Logger LOGGER = Logger.getLogger(LineProcessor.class);

    private class AggregationCloser implements Runnable {
        private volatile boolean _run = true;
        private final double _rotationFactor = 0.5d;
        private final int _rotationCheckMillis = 500;

        @Override
        public void run() {
            while (_run) {
                try {
                    Thread.sleep(_rotationCheckMillis);
                    for (@Nonnull Map.Entry<String, TSData> entry : _aggregations.entrySet()) {
                        entry.getValue().checkRotate(_rotationFactor);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    LOGGER.error("Interrupted!", e);
                }
            }
        }

        public void shutdown() {
            _run = false;
        }
    }

    public LineProcessor(LogParser parser, Set<Statistic> timerStatisticsClasses,
                         Set<Statistic> counterStatisticsClasses, Set<Statistic> gaugeStatisticsClasses,
                         String hostName, String serviceName, Set<Period> periods, AggregationPublisher listener) {
        this._parser = parser;
        this._timerStatisticsClasses = timerStatisticsClasses;
        this._counterStatisticsClasses = counterStatisticsClasses;
        this._gaugeStatisticsClasses = gaugeStatisticsClasses;
        this._hostName = hostName;
        this._serviceName = serviceName;
        this._periods = periods;
        this._listener = listener;
        this._aggregations = new ConcurrentHashMap<>();

        startAggregationCloser();
    }

    public void shutdown() {
        _aggregationCloser.shutdown();
    }

    private void startAggregationCloser() {
        _aggregationCloser = new AggregationCloser();
        @Nonnull final Thread aggregationCloserThread = new Thread(_aggregationCloser);
        aggregationCloserThread.setDaemon(true);
        aggregationCloserThread.start();
    }

    public void invoke(String line) {
        @Nonnull Optional<LogLine> optionalData = _parser.parseLogLine(line);
        if (!optionalData.isPresent()) {
            return;
        }

        LogLine data = optionalData.get();

        //Loop over all metrics
        for (@Nonnull Map.Entry<String, CounterVariable> entry : data.getVariables().entrySet()) {
            //Find the TSData associated with a metric
            TSData tsdata = _aggregations.get(entry.getKey());
            //If the metric isn't already listed, create a new TSData for it
            if (tsdata == null) {
                switch (entry.getValue().getMetricKind()) {
                    case Counter:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _counterStatisticsClasses);
                        break;
                    case Timer:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _timerStatisticsClasses);
                        break;
                    case Gauge:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _gaugeStatisticsClasses);
                        break;
                    default:
                        LOGGER.warn("unknown metric kind, defaulting to counter statistics. metricKind = " +
                                entry.getValue().getMetricKind().toString());
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _counterStatisticsClasses);
                        break;
                }
                TSData returned = _aggregations.putIfAbsent(entry.getKey(), tsdata);
                if (returned != null) {
                    tsdata = returned;
                }
            }
            tsdata.addMetric(entry.getValue().getValues(), data.getTime());
        }
    }

    public void closeAggregations() {
        //close all aggregations
        for (@Nonnull Map.Entry<String, TSData> entry : _aggregations.entrySet()) {
            entry.getValue().close();
        }
    }
}
