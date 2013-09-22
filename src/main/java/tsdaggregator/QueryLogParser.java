package tsdaggregator;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class QueryLogParser implements LogParser {
    static final Logger _Logger = Logger.getLogger(QueryLogParser.class);
	private  static final ObjectMapper MAPPER = new ObjectMapper();

    private LogLine parseLegacyLogLine(String line) {
        HashMap<String, CounterVariable> vals = new HashMap<String, CounterVariable>();
        line.trim();
        line = line.replace("[", "");
        line = line.replace("]", "");
        ArrayList<String> removalCandidates = new ArrayList<String>();

        String[] vars = line.split(",");
        for (String var : vars) {
            if (var.length() > 0) {
                String[] splits = var.split("=");
                Double value = Double.parseDouble(splits[1]);
                String key = splits[0].trim();

                if (key.endsWith("-start")) {
                    removalCandidates.add(key.replaceFirst("-start$", ""));
                } else {
                    ArrayList<Double> values = new ArrayList<Double>();
                    values.add(value);
                    CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, values);
                    vals.put(key, cv);
                }
            }
        }

        for (String remove : removalCandidates) {
            if (vals.containsKey(remove) && vals.get(remove).getValues().get(0) == 0.0d) {
                vals.remove(remove);
                _Logger.warn("removing unfinished timer [" + remove + "] from timing set");
            }
        }
		DateTime timestamp;
        if (vals.containsKey("initTimestamp")) {
            Double time = vals.get("initTimestamp").getValues().get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
            vals.remove("initTimestamp");
        } else {
			timestamp = new DateTime(0);
		}

		return new StandardLogLine(vals, timestamp);
    }

    public LogLine parseV2aLogLine(Map<String, Object> line) throws ParseException {
		TreeMap<String, CounterVariable> variables = new TreeMap<>();
        final Map<String, Double> counters;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Double> parsedCounters = (Map<String, Double>) line.get("counters");
			counters = parsedCounters;
		} catch (Exception e) {
			throw new ParseException("could not cast element counters", e);
		}
        for (Map.Entry<String, Double> entry : counters.entrySet()) {
            ArrayList<Double> counter = new ArrayList<Double>();
            counter.add(Double.parseDouble(entry.getValue().toString()));
            CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, counter);
            variables.put(entry.getKey().toString(), cv);
        }

        final Map<String, ArrayList<Double>> timers;
		try {
			@SuppressWarnings("unchecked")
			Map<String, ArrayList<Double>> parsedTimers = (Map<String, ArrayList<Double>>) line.get("timers");
			timers = parsedTimers;
		} catch (Exception e) {
			throw new ParseException("could not cast element timers", e);
		}
        for (Map.Entry<String, ArrayList<Double>> entry : timers.entrySet()) {
            CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Timer, entry.getValue());
            variables.put(entry.getKey().toString(), cv);
        }

		DateTime timestamp;
        if (variables.containsKey("initTimestamp")) {
            Double time = variables.get("initTimestamp").getValues().get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
            variables.remove("initTimestamp");
        } else {
			timestamp = new DateTime(0);
		}
		return new StandardLogLine(variables, timestamp);
    }

    public LogLine parseV2bLogLine(Map<String, Object> line) throws ParseException {
		TreeMap<String, CounterVariable> variables = new TreeMap<>();

		final Map<String, Object> counters;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> parsedCounters = (Map<String, Object>) line.get("counters");
			counters = parsedCounters;
		} catch (Exception e) {
			throw new ParseException("could not cast element counters", e);
		}
        for (Map.Entry<String, Object> entry : counters.entrySet()) {
            ArrayList<Double> counter = new ArrayList<Double>();
            counter.add(Double.parseDouble(entry.getValue().toString()));
            CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, counter);
            variables.put(entry.getKey().toString(), cv);
        }

        final Map<String, ArrayList<Object>> timers;
		try {
			@SuppressWarnings("unchecked")
			Map<String, ArrayList<Object>> parsedTimers = (Map<String, ArrayList<Object>>) line.get("timers");
			timers = parsedTimers;
		} catch (Exception e) {
			throw new ParseException("could not cast element timers", e);
		}
        for (Map.Entry<String, ArrayList<Object>> entry : timers.entrySet()) {
            ArrayList<Object> vals = entry.getValue();
            ArrayList<Double> newVals = new ArrayList<Double>();
            for (Object val : vals) {
                newVals.add(Double.valueOf(val.toString()));
            }
            CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Timer, newVals);
            variables.put(entry.getKey().toString(), cv);
        }

		DateTime timestamp;
        final Map<String, String> annotations;
		try {
			@SuppressWarnings("unchecked")
			Map<String, String> parsedAnnotations = (Map<String, String>)line.get("annotations");
			annotations = parsedAnnotations;
		} catch (Exception e) {
			throw new ParseException("could not cast element annotations");
		}
        if (annotations.containsKey("finalTimestamp")) {
            Double time = Double.parseDouble(annotations.get("finalTimestamp"));
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
        }
        else if (annotations.containsKey("initTimestamp")) {
            Double time = Double.parseDouble(annotations.get("initTimestamp"));
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
        } else {
			timestamp = new DateTime(0);
		}
		return new StandardLogLine(variables, timestamp);
    }

    @Override
    public LogLine parseLogLine(String line) {
		LogLine logLine;

		final Map<String, Object> jsonLine;
        try {
			@SuppressWarnings("unchecked")
			final Map<String, Object> parsedJson = MAPPER.readValue(line, Map.class);
            jsonLine = parsedJson;
        } catch (IOException ex) {
            _Logger.warn("Possible legacy, non-json tsd line found: ", ex);
            try {
                logLine = parseLegacyLogLine(line);
            }
            catch (Exception e) {
                _Logger.warn("Discarding line: Unparsable.\nLine was:\n" + line, e);
				logLine = null;
            }
			return logLine;
		}
		try {
			String version = (String) jsonLine.get("version");
			if (version.equals("2a")) {
				logLine = parseV2aLogLine(jsonLine);
			}
			else if (version.equals("2b")) {
				logLine = parseV2bLogLine(jsonLine);
			}
			else if (version.equals("2c")) {
				logLine = parseV2cLogLine(jsonLine);
			}
		    else {
				throw new ParseException("unknown line version");
			}
		}
		catch (ParseException e) {
			_Logger.warn("Discarding line: Unparsable.\nLine was:\n" + line, e);
			logLine = null;
		}
		return logLine;
    }

	private LogLine parseV2cLogLine(Map<String,Object> line) throws ParseException {
		_Logger.info("Trying to parse V2c log line");
		TreeMap <String, CounterVariable> variables = new TreeMap<>();
		parseChunk(line, "timers", CounterVariable.MetricKind.Timer, variables);
		_Logger.info("Timers parsed");
		parseChunk(line, "counters", CounterVariable.MetricKind.Counter, variables);
		_Logger.info("Counters parsed");
		parseChunk(line, "gauges", CounterVariable.MetricKind.Gauge, variables);
		_Logger.info("All chunks parsed");

		DateTime timestamp;
		final Map<String, String> annotations;
		try {
			@SuppressWarnings("unchecked")
			Map<String, String> parsedAnnotations = (Map<String, String>)line.get("annotations");
			annotations = parsedAnnotations;
		} catch (Exception e) {
			throw new ParseException("could not cast annotations to look for timestamp", e);
		}

		if (annotations.containsKey("finalTimestamp")) {
			Double time = Double.parseDouble(annotations.get("finalTimestamp"));
			//double with whole number unix time, and fractional seconds
			Long ticks = Math.round(time * 1000);
			timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
		}
		else if (annotations.containsKey("initTimestamp")) {
			Double time = Double.parseDouble(annotations.get("initTimestamp"));
			//double with whole number unix time, and fractional seconds
			Long ticks = Math.round(time * 1000);
			timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
		} else {
			throw new ParseException("no timestamp found in log line");
		}
		return new StandardLogLine(variables, timestamp);
	}

	private void parseChunk(Map<String, Object> lineData, String element, CounterVariable.MetricKind kind, Map<String, CounterVariable> variables) throws ParseException {
		final Map<String, ArrayList<Object>> elements;
		try {
			@SuppressWarnings("unchecked")
			Map<String, ArrayList<Object>> parsedElements = (Map<String, ArrayList<Object>>) lineData.get(element);
			elements = parsedElements;
		}
		catch (Exception e) {
			throw new ParseException("could not cast element " + element, e);
		}
		for (Map.Entry<String, ArrayList<Object>> entry : elements.entrySet()) {
			ArrayList<Object> lineValues = entry.getValue();
			ArrayList<Double> parsedValues = new ArrayList<Double>();
			for (Object val : lineValues) {
				parsedValues.add(Double.valueOf(val.toString()));
			}
			CounterVariable cv = new CounterVariable(kind, parsedValues);
			variables.put(entry.getKey().toString(), cv);
		}
	}
}
