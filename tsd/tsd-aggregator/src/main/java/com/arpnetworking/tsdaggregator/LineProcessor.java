/**
 * Copyright 2014 Brandon Arp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.model.Metric;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.observer.Observable;
import com.arpnetworking.utility.observer.Observer;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Processes lines of data from a log file.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LineProcessor implements Observer {

    /**
     * Public constructor.
     * 
     * @param timerStatisticsClasses The statistics to use for timers.
     * @param counterStatisticsClasses The statistics to use for counters.
     * @param gaugeStatisticsClasses The statistics to use for gauges.
     * @param hostName The name of the host.
     * @param serviceName The name of the service.
     * @param periods The periods to aggregate over.
     * @param listener The destination for completed metric period aggregates.
     */
    // CHECKSTYLE.OFF: ParameterNumber - Needs to be refactored.
    public LineProcessor(
            final Set<Statistic> timerStatisticsClasses,
            final Set<Statistic> counterStatisticsClasses,
            final Set<Statistic> gaugeStatisticsClasses,
            final String hostName,
            final String serviceName,
            final Set<Period> periods,
            final Sink listener) {
        // CHECKSTYLE.ON: ParameterNumber - Needs to be refactored.
        this._timerStatisticsClasses = timerStatisticsClasses;
        this._counterStatisticsClasses = counterStatisticsClasses;
        this._gaugeStatisticsClasses = gaugeStatisticsClasses;
        this._hostName = hostName;
        this._serviceName = serviceName;
        this._periods = periods;
        this._listener = listener;
        this._aggregations = new ConcurrentHashMap<String, TSData>();
        this._lastLineReadTime = new AtomicReference<DateTime>(DateTime.now());
        startAggregationCloser();
    }

    /**
     * Stop the line processor.
     */
    public void shutdown() {
        _aggregationCloser.shutdown();
    }

    private void startAggregationCloser() {
        _aggregationCloser = new AggregationCloser();
        final Thread aggregationCloserThread = new Thread(_aggregationCloser);
        aggregationCloserThread.setDaemon(true);
        aggregationCloserThread.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notify(final Observable observable, final Object event) {
        if (!(event instanceof Record)) {
            LOGGER.error(String.format("Observed unsupported event; event=%s", event));
            return;
        }
        final Record data = (Record) event;

        final DateTime now = DateTime.now();
        _lastLineReadTime.set(now);

        // Loop over all metrics
        for (@Nonnull
        final Map.Entry<String, ? extends Metric> entry : data.getMetrics().entrySet()) {
            // Find the TSData associated with a metric
            TSData tsdata = _aggregations.get(entry.getKey());
            // If the metric isn't already listed, create a new TSData for it
            if (tsdata == null) {
                switch (entry.getValue().getType()) {
                    case COUNTER:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _counterStatisticsClasses);
                    break;
                    case TIMER:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _timerStatisticsClasses);
                    break;
                    case GAUGE:
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _gaugeStatisticsClasses);
                    break;
                    default:
                        LOGGER.warn("unknown metric kind, defaulting to counter statistics. metricKind = "
                                + entry.getValue().getType().toString());
                        tsdata = new TSData(entry.getKey(), _periods, _listener, _hostName, _serviceName,
                                _counterStatisticsClasses);
                    break;
                }
            }

            // If there's a place to record it, remember the values & update the
            // last written time
            if (tsdata != null) {
                tsdata.addMetric(entry.getValue().getValues(), data.getTime());
            }
        }
    }

    /**
     * Close all aggregations.
     */
    public void closeAggregations() {
        for (@Nonnull
        final Map.Entry<String, TSData> entry : _aggregations.entrySet()) {
            entry.getValue().close();
        }
    }

    private final Set<Statistic> _timerStatisticsClasses;
    private final Set<Statistic> _counterStatisticsClasses;
    private final Set<Statistic> _gaugeStatisticsClasses;
    private final String _hostName;
    private final String _serviceName;
    private final Set<Period> _periods;
    private final Sink _listener;
    private final ConcurrentHashMap<String, TSData> _aggregations;
    private final AtomicReference<DateTime> _lastLineReadTime;
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
                    // Wait at least 3 seconds to roll the file
                    if (_lastLineReadTime.get().isBefore(DateTime.now().minus(Duration.standardSeconds(3)))) {
                        for (@Nonnull
                        final Map.Entry<String, TSData> entry : _aggregations.entrySet()) {
                            entry.getValue().checkRotate(_rotationFactor);
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                    LOGGER.error("Interrupted!", e);
                }
            }
        }

        public void shutdown() {
            _run = false;
        }
    }
}
