package tsdaggregator;

import java.util.*;

import org.joda.time.*;

public class TSAggregation {
	Period _Period;
	Integer _NumberOfSamples = 0;
	ArrayList<Double> _Samples = new ArrayList<Double>();
	DateTime _PeriodStart = new DateTime(0);
	Set<Statistic> _OrderedStatistics = new HashSet<Statistic>();
        Set<Statistic> _UnorderedStatistics = new HashSet<Statistic>();
	String _Metric;
	String _HostName;
	String _ServiceName;
	AggregationListener _Listener;
	private static final Set<Statistic> DEFAULT_STATS = BuildDefaultStats();
	
	private static Set<Statistic> BuildDefaultStats() {
		Set<Statistic> stats = new HashSet<Statistic>();
		stats.add(new TP100());
		stats.add(new TP99());
		stats.add(new TP99p9());
		stats.add(new TP50());
		stats.add(new TP0());
		stats.add(new NStatistic());
		stats.add(new MeanStatistic());
		return stats;
	}
	
	public TSAggregation(String metric, Period period, AggregationListener listener, String hostName, String serviceName) {
		this(metric, period, listener, hostName, serviceName, DEFAULT_STATS);
	}
	public TSAggregation(String metric, Period period, AggregationListener listener, String hostName, String serviceName, Set<Statistic> stats) {
		_Metric = metric;
		_Period = period;
		addStatistics(stats);
		_HostName = hostName;
		_ServiceName = serviceName;
		_Listener = listener;
	}
        
        private void addStatistics(Set<Statistic> stats) {
            for (Statistic s : stats) {
                addStatistic(s);
            }
        }
        
        private void addStatistic(Statistic s) {
            if (s instanceof OrderedStatistic) {
                _OrderedStatistics.add(s);
            } else {
                _UnorderedStatistics.add(s);
            }
        }
        
	public void addSample(Double value, DateTime time) {
		rotateAggregation(time);
		_Samples.add(value);
		_NumberOfSamples++;
	}
	
	private void rotateAggregation(DateTime time){
		if (time.isAfter(_PeriodStart.plus(_Period))) {
			//Calculate the start of the new aggregation
			DateTime hour = time.hourOfDay().roundFloorCopy();
			DateTime startPeriod = hour;
			while (!(startPeriod.isBefore(time) && startPeriod.plus(_Period).isAfter(time))) {
				startPeriod = startPeriod.plus(_Period);
			}
			emitAggregations();
			_PeriodStart = startPeriod;			
			_NumberOfSamples = 0;
			_Samples.clear();
		}
	}
	
	public void close() {
		emitAggregations();
	}
	
	public AggregationListener getListener() {
		return _Listener;
	}

	public void setListener(AggregationListener listener) {
		_Listener = listener;
	}

	private void emitAggregations()
	{
		Double[] dsamples = _Samples.toArray(new Double[0]);

                if (dsamples.length == 0) {
			return;
		}
                ArrayList<AggregatedData> aggregates = new ArrayList<AggregatedData>();                
                for (Statistic stat : _UnorderedStatistics) {
			Double value = stat.calculate(dsamples);
			if (_Listener != null) {
				AggregatedData data = new AggregatedData();
				data.setPeriod(_Period);
				data.setHost(_HostName);
				data.setService(_ServiceName);
				data.setStatistic(stat);
				data.setValue(value);
				data.setPeriodStart(_PeriodStart);
				data.setMetric(_Metric);
				aggregates.add(data);
			}
		}
		Arrays.sort(dsamples);
		for (Statistic stat : _OrderedStatistics) {
			Double value = stat.calculate(dsamples);
			if (_Listener != null) {
				AggregatedData data = new AggregatedData();
				data.setPeriod(_Period);
				data.setHost(_HostName);
				data.setService(_ServiceName);
				data.setStatistic(stat);
				data.setValue(value);
				data.setPeriodStart(_PeriodStart);
				data.setMetric(_Metric);
				aggregates.add(data);
			}
		}
		_Listener.recordAggregation(aggregates.toArray(new AggregatedData[0]));
	}
}
