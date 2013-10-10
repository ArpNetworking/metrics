package com.arpnetworking.tsdaggregator;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * A typical log line.
 *
 * @author barp
 */
public class StandardLogLine implements LogLine {
    private final Map<String, CounterVariable> _variables;
    private final DateTime _time;

    public StandardLogLine(Map<String, CounterVariable> variables, DateTime time) {
        this._variables = variables;
        this._time = time;
    }

    @Override
    public DateTime getTime() {
        return _time;
    }

    @Override
    public Map<String, CounterVariable> getVariables() {
        return _variables;
    }
}
