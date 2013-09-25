package tsdaggregator;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import tsdaggregator.publishing.AggregationPublisher;
import tsdaggregator.statistics.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TSAggregation {

    Period _Period;
    Integer _NumberOfSamples = 0;
    ArrayList<Double> _Samples = new ArrayList<Double>();
    DateTime _PeriodStart = new DateTime(0);
    Set<Statistic> _OrderedStatistics = new HashSet<>();
    Set<Statistic> _UnorderedStatistics = new HashSet<>();
    String _Metric;
    String _HostName;
    String _ServiceName;
    AggregationPublisher _Listener;
    static Logger _Logger = Logger.getLogger(TSAggregation.class);

    public TSAggregation(String metric, Period period, AggregationPublisher listener, String hostName, String serviceName, Set<Statistic> statistics) {
        _Metric = metric;
        _Period = period;
        addStatistics(statistics, _OrderedStatistics, _UnorderedStatistics);
        _HostName = hostName;
        _ServiceName = serviceName;
        _Listener = listener;
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
        _Samples.add(value);
        _NumberOfSamples++;
        _Logger.debug("Added sample to aggregation: time = " + time.toString());
    }

    public void checkRotate() {
        rotateAggregation(new DateTime().minus(Duration.standardSeconds(60)));
    }

    public void checkRotate(long rotateMillis) {
        rotateAggregation(new DateTime().minus(new Duration(rotateMillis)));
    }

    private void rotateAggregation(DateTime time) {
        _Logger.debug("Checking roll. Period is " + _Period + ", Roll time is " + _PeriodStart.plus(_Period));
        if (time.isAfter(_PeriodStart.plus(_Period))) {
            //Calculate the start of the new aggregation
            _Logger.debug("We're rolling");
            DateTime hour = time.hourOfDay().roundFloorCopy();
            DateTime startPeriod = hour;
            while (!(startPeriod.isBefore(time) && startPeriod.plus(_Period).isAfter(time)) 
                    && (!startPeriod.equals(time))) {
                startPeriod = startPeriod.plus(_Period);
            }
            _Logger.debug("New start period is " + startPeriod);
            emitAggregations();
            _PeriodStart = startPeriod;
            _NumberOfSamples = 0;
            _Samples.clear();
        }
    }

    public void close() {
        emitAggregations();
    }

    public AggregationPublisher getListener() {
        return _Listener;
    }

    public void setListener(AggregationPublisher listener) {
        _Listener = listener;
    }

    private void emitAggregations() {
        _Logger.debug("Emitting aggregations; " + _Samples.size() + " samples");
        Double[] dsamples = _Samples.toArray(new Double[0]);
        if (dsamples.length == 0) {
            return;
        }
        ArrayList<AggregatedData> aggregates = new ArrayList<AggregatedData>();
        for (Statistic stat : _UnorderedStatistics) {
            Double value = stat.calculate(dsamples);
            if (_Listener != null) {
                AggregatedData data = new AggregatedData(stat, _ServiceName, _HostName, _Metric, value, _PeriodStart, _Period);
                aggregates.add(data);
            }
        }
        //only sort if there are ordered statistics
        if (_OrderedStatistics.size() > 0) {
            Arrays.sort(dsamples);
            for (Statistic stat : _OrderedStatistics) {
                Double value = stat.calculate(dsamples);
                if (_Listener != null) {
					AggregatedData data = new AggregatedData(stat, _ServiceName, _HostName, _Metric, value, _PeriodStart, _Period);
                    aggregates.add(data);
                }
            }
        }
        _Logger.debug("Writing " + aggregates.size() + " aggregation records");
        _Listener.recordAggregation(aggregates.toArray(new AggregatedData[0]));
    }
}
