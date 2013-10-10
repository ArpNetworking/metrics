package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.publishing.AggregationPublisher;
import com.arpnetworking.tsdaggregator.statistics.OrderedStatistic;
import com.arpnetworking.tsdaggregator.statistics.Statistic;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds samples, delegates calculations to statistics, and emits the calculated statistics.
 */
public class TSAggregation {

    final Period _period;
    int _numberOfSamples = 0;
    final ArrayList<Double> _samples = new ArrayList<Double>();
    DateTime _periodStart = new DateTime(0);
    final Set<Statistic> _orderedStatistics = new HashSet<>();
    final Set<Statistic> _unorderedStatistics = new HashSet<>();
    final String _metric;
    final String _hostName;
    final String _serviceName;
    AggregationPublisher _listener;

    static final Logger LOGGER = Logger.getLogger(TSAggregation.class);

    public TSAggregation(String metric, Period period, AggregationPublisher listener, String hostName,
                         String serviceName, Set<Statistic> statistics) {
        _metric = metric;
        _period = period;
        addStatistics(statistics, _orderedStatistics, _unorderedStatistics);
        _hostName = hostName;
        _serviceName = serviceName;
        _listener = listener;
    }

    private void addStatistics(Set<Statistic> stats, Set<Statistic> orderedStatsSet, Set<Statistic> unorderedStatsSet) {
        for (Statistic s : stats) {
            addStatistic(s, orderedStatsSet, unorderedStatsSet);
        }
    }

    private void addStatistic(Statistic s, Set<Statistic> orderedStatsSet, Set<Statistic> unorderedStatsSet) {
        if (s instanceof OrderedStatistic) {
            orderedStatsSet.add(s);
        } else {
            unorderedStatsSet.add(s);
        }
    }

    public void addSample(Double value, DateTime time) {
        rotateAggregation(time);
        _samples.add(value);
        _numberOfSamples++;
        LOGGER.debug("Added sample to aggregation: time = " + time.toString());
    }

    public void checkRotate() {
        rotateAggregation(new DateTime().minus(Duration.standardSeconds(60)));
    }

    public void checkRotate(double rotateFactor) {
        Duration rotateDuration = Duration.millis(
                (long) (_period.toDurationFrom(_periodStart).getMillis() * rotateFactor));
        rotateAggregation(DateTime.now().minus(new Duration(rotateDuration)));
    }

    private void rotateAggregation(DateTime time) {
        LOGGER.debug("Checking roll. Period is " + _period + ", Roll time is " + _periodStart.plus(_period));
        if (time.isAfter(_periodStart.plus(_period))) {
            //Calculate the start of the new aggregation
            LOGGER.debug("We're rolling");
            DateTime hour = time.hourOfDay().roundFloorCopy();
            DateTime startPeriod = hour;
            while (!(startPeriod.isBefore(time) && startPeriod.plus(_period).isAfter(time))
                    && (!startPeriod.equals(time))) {
                startPeriod = startPeriod.plus(_period);
            }
            LOGGER.debug("New start period is " + startPeriod);
            emitAggregations();
            _periodStart = startPeriod;
            _numberOfSamples = 0;
            _samples.clear();
        }
    }

    public void close() {
        emitAggregations();
    }

    public AggregationPublisher getListener() {
        return _listener;
    }

    public void setListener(AggregationPublisher listener) {
        _listener = listener;
    }

    private void emitAggregations() {
        LOGGER.debug("Emitting aggregations; " + _samples.size() + " samples");
        Double[] dsamples = _samples.toArray(new Double[0]);
        if (dsamples.length == 0) {
            return;
        }
        ArrayList<AggregatedData> aggregates = new ArrayList<AggregatedData>();
        for (Statistic stat : _unorderedStatistics) {
            Double value = stat.calculate(dsamples);
            if (_listener != null) {
                AggregatedData data = new AggregatedData(stat, _serviceName, _hostName, _metric, value,
                        _periodStart, _period);
                aggregates.add(data);
            }
        }
        //only sort if there are ordered statistics
        if (_orderedStatistics.size() > 0) {
            Arrays.sort(dsamples);
            for (Statistic stat : _orderedStatistics) {
                Double value = stat.calculate(dsamples);
                if (_listener != null) {
                    AggregatedData data = new AggregatedData(stat, _serviceName, _hostName, _metric, value,
                            _periodStart, _period);
                    aggregates.add(data);
                }
            }
        }
        LOGGER.debug("Writing " + aggregates.size() + " aggregation records");
        _listener.recordAggregation(aggregates.toArray(new AggregatedData[0]));
    }
}
