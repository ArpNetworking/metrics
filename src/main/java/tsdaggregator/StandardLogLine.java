package tsdaggregator;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * A typical log line
 *
 * @author barp
 */
public class StandardLogLine implements LogLine {
	private final Map<String, CounterVariable> _Variables;
	private final DateTime _Time;

	public StandardLogLine(Map<String, CounterVariable> variables, DateTime time) {
		this._Variables = variables;
		this._Time = time;
	}

	@Override
	public DateTime getTime() {
		return _Time;
	}

	@Override
	public Map<String, CounterVariable> getVariables() {
		return _Variables;
	}
}
