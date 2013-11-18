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
import javax.annotation.Nonnull;

/**
 * Holds samples, delegates calculations to statistics, and emits the calculated statistics.
 */
public class TSAggregation {

    private static final Logger LOGGER = Logger.getLogger(TSAggregation.class);
    private final Period _period;
    private final ArrayList<Double> _samples = new ArrayList<>();
    private final Set<Statistic> _orderedStatistics = new HashSet<>();
    private final Set<Statistic> _unorderedStatistics = new HashSet<>();
    private final String _metric;
    private final String _hostName;
    private final String _serviceName;
    private final AggregationPublisher _listener;
    private int _numberOfSamples = 0;
    private DateTime _periodStart = new DateTime(0);

    public TSAggregation(@Nonnull String metric, @Nonnull Period period, @Nonnull AggregationPublisher listener,
                         @Nonnull String hostName, @Nonnull String serviceName, @Nonnull Set<Statistic> statistics) {
        _metric = metric;
        _period = period;
        addStatistics(statistics, _orderedStatistics, _unorderedStatistics);
        _hostName = hostName;
        _serviceName = serviceName;
        _listener = listener;
    }

    private void addStatistics(@Nonnull Set<Statistic> stats, @Nonnull Set<Statistic> orderedStatsSet,
                               @Nonnull Set<Statistic> unorderedStatsSet) {
        for (Statistic s : stats) {
            addStatistic(s, orderedStatsSet, unorderedStatsSet);
        }
    }

    private void addStatistic(Statistic s, @Nonnull Set<Statistic> orderedStatsSet,
                              @Nonnull Set<Statistic> unorderedStatsSet) {
        if (s instanceof OrderedStatistic) {
            orderedStatsSet.add(s);
        } else {
            unorderedStatsSet.add(s);
        }
    }

    public void addSample(Double value, @Nonnull DateTime time) {
        rotateAggregation(time);
        _samples.add(value);
        _numberOfSamples++;
        LOGGER.debug("Added sample to aggregation: time = " + time.toString());
    }

    public void checkRotate(double rotateFactor) {
        Duration rotateDuration = Duration.millis(
                (long) (_period.toDurationFrom(_periodStart).getMillis() * rotateFactor));
        rotateAggregation(DateTime.now().minus(new Duration(rotateDuration)));
    }

    private void rotateAggregation(@Nonnull DateTime time) {
        LOGGER.debug("Checking roll. Period is " + _period + ", Roll time is " + _periodStart.plus(_period));
        if (time.isAfter(_periodStart.plus(_period))) {
            //Calculate the start of the new aggregation
            LOGGER.debug("We're rolling");
            DateTime startPeriod = time.hourOfDay().roundFloorCopy();
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

    private void emitAggregations() {
        LOGGER.debug("Emitting aggregations; " + _samples.size() + " samples");
        Double[] dsamples = _samples.toArray(new Double[_samples.size()]);
        if (dsamples.length == 0) {
            return;
        }
        ArrayList<AggregatedData> aggregates = new ArrayList<>();
        for (Statistic stat : _unorderedStatistics) {
            Double value = stat.calculate(dsamples);
            AggregatedData data = new AggregatedData(stat, _serviceName, _hostName, _metric, value,
                    _periodStart, _period, dsamples);
            aggregates.add(data);
        }
        //only sort if there are ordered statistics
        if (_orderedStatistics.size() > 0) {
            Arrays.sort(dsamples);
            for (Statistic stat : _orderedStatistics) {
                Double value = stat.calculate(dsamples);
                AggregatedData data = new AggregatedData(stat, _serviceName, _hostName, _metric, value,
                        _periodStart, _period, dsamples);
                aggregates.add(data);
            }
        }
        LOGGER.debug("Writing " + aggregates.size() + " aggregation records");
        _listener.recordAggregation(aggregates.toArray(new AggregatedData[aggregates.size()]));
    }
}
