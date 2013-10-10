package com.arpnetworking.tsdaggregator;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * LogParser to parse query log files with json hash lines.
 *
 * @author barp
 */
public class QueryLogParser implements LogParser {
    private static final Logger LOGGER = Logger.getLogger(QueryLogParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LogLine parseLegacyLogLine(String line) throws ParseException {
        HashMap<String, CounterVariable> vals = new HashMap<String, CounterVariable>();
        line = line.trim();
        line = line.replace("[", "");
        line = line.replace("]", "");
        ArrayList<String> removalCandidates = new ArrayList<String>();

        String[] vars = line.split(",");
        for (String var : vars) {
            if (var.trim().length() > 0) {
                String[] splits = var.split("=");
                Double value;
                try {
                    value = Double.parseDouble(splits[1]);
                } catch (NumberFormatException e) {
                    LOGGER.warn("skipping value due to not being able to parse as double: " + splits[1], e);
                    continue;
                }
                String key = splits[0].trim();

                if (key.endsWith("-start")) {
                    key = key.replaceFirst("-start$", "");
                    removalCandidates.add(key);
                }
                ArrayList<Double> values = new ArrayList<Double>();
                values.add(value);
                CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, values);
                vals.put(key, cv);
            }
        }

        for (String remove : removalCandidates) {
            if (vals.get(remove).getValues().get(0).equals(0.0d)) {
                vals.remove(remove);
                LOGGER.warn("removing unfinished timer [" + remove + "] from timing set");
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
            throw new ParseException("no timestamp found in log line");
        }

        return new StandardLogLine(vals, timestamp);
    }

    public LogLine parseV2aLogLine(Map<String, Object> line) throws ParseException {
        TreeMap<String, CounterVariable> variables = new TreeMap<>();
        final Map<String, Object> counters;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedCounters = (Map<String, Object>) line.get("counters");
            counters = parsedCounters;
        } catch (Exception e) {
            throw new ParseException("could not cast element counters", e);
        }
        if (counters != null) {
            for (Map.Entry<String, Object> entry : counters.entrySet()) {
                ArrayList<Double> counter = new ArrayList<Double>();
                try {
                    counter.add(Double.valueOf(entry.getValue().toString()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("skipping value due to not being able to parse as double: " +
                            entry.getValue().toString(), e);
                }
                CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, counter);
                variables.put(entry.getKey().toString(), cv);
            }
        }

        final Map<String, ArrayList<Object>> timers;
        try {
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<Object>> parsedTimers = (Map<String, ArrayList<Object>>) line.get("timers");
            timers = parsedTimers;
        } catch (Exception e) {
            throw new ParseException("could not cast element timers", e);
        }
        if (timers != null) {
            for (Map.Entry<String, ArrayList<Object>> entryArray : timers.entrySet()) {
                ArrayList<Double> counter = new ArrayList<Double>();
                for (Object entry : entryArray.getValue()) {
                    try {
                        counter.add(Double.valueOf(entry.toString()));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("skipping value due to not being able to parse as double: " + entry.toString(), e);
                    }
                }
                CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Timer, counter);
                variables.put(entryArray.getKey().toString(), cv);
            }
        }

        DateTime timestamp;
        if (variables.containsKey("initTimestamp")) {
            Double time = variables.get("initTimestamp").getValues().get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
            variables.remove("initTimestamp");
        } else {
            throw new ParseException("no timestamp found in log line");
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
        if (counters != null) {
            for (Map.Entry<String, Object> entry : counters.entrySet()) {
                ArrayList<Double> counter = new ArrayList<Double>();
                try {
                    counter.add(Double.parseDouble(entry.getValue().toString()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("skipping value due to not being able to parse as double: " + entry.getValue(), e);
                }
                CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Counter, counter);
                variables.put(entry.getKey().toString(), cv);
            }
        }

        final Map<String, ArrayList<Object>> timers;
        try {
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<Object>> parsedTimers = (Map<String, ArrayList<Object>>) line.get("timers");
            timers = parsedTimers;
        } catch (Exception e) {
            throw new ParseException("could not cast element timers", e);
        }
        if (timers != null) {
            for (Map.Entry<String, ArrayList<Object>> entry : timers.entrySet()) {
                ArrayList<Object> vals = entry.getValue();
                ArrayList<Double> newVals = new ArrayList<Double>();
                for (Object val : vals) {
                    try {
                        newVals.add(Double.valueOf(val.toString()));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("skipping value due to not being able to parse as double: " + val, e);
                    }
                }
                CounterVariable cv = new CounterVariable(CounterVariable.MetricKind.Timer, newVals);
                variables.put(entry.getKey().toString(), cv);
            }
        }

        final Map<String, String> annotations;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> parsedAnnotations = (Map<String, String>) line.get("annotations");
            annotations = parsedAnnotations;
        } catch (Exception e) {
            throw new ParseException("could not cast element annotations");
        }
        if (annotations == null) {
            throw new ParseException("no timestamp found in log line");
        }
        DateTime timestamp = getTimestamp(annotations);

        return new StandardLogLine(variables, timestamp);
    }

    private DateTime getTimestamp(Map<String, String> annotations) throws ParseException {
        DateTime timestamp = null;
        if (annotations.containsKey("finalTimestamp")) {
            Double time;
            try {
                time = Double.parseDouble(annotations.get("finalTimestamp"));
            } catch (NumberFormatException e) {
                LOGGER.warn("finalTimestamp value is not parsable, falling back to initTimestamp, value: " +
                        annotations.get("finalTimestamp"), e);
                time = null;
            }
            if (time != null) {
                //double with whole number unix time, and fractional seconds
                Long ticks = Math.round(time * 1000);
                timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
            }
        }

        if (timestamp == null && annotations.containsKey("initTimestamp")) {
            Double time;
            try {
                time = Double.parseDouble(annotations.get("initTimestamp"));
            } catch (NumberFormatException e) {
                LOGGER.warn("initTimestamp value is not parsable, falling back to initTimestamp, value: " +
                        annotations.get("initTimestamp"), e);
                time = null;

            }
            if (time != null) {
                //double with whole number unix time, and fractional seconds
                Long ticks = Math.round(time * 1000);
                timestamp = new DateTime(ticks, ISOChronology.getInstanceUTC());
            }
        }

        if (timestamp == null) {
            throw new ParseException("no timestamp found in log line");
        }
        return timestamp;
    }

    @Override
    public Optional<LogLine> parseLogLine(String line) {
        LogLine logLine;

        final Map<String, Object> jsonLine;
        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> parsedJson = MAPPER.readValue(line, Map.class);
            jsonLine = parsedJson;
        } catch (IOException ex) {
            LOGGER.warn("Possible legacy, non-json tsd line found: ", ex);
            try {
                logLine = parseLegacyLogLine(line);
            } catch (Exception e) {
                LOGGER.warn("Discarding line: Unparsable.\nLine was:\n" + line, e);
                logLine = null;
            }
            return Optional.fromNullable(logLine);
        }
        try {
            String version = (String) jsonLine.get("version");
            if (version.equals("2a")) {
                logLine = parseV2aLogLine(jsonLine);
            } else if (version.equals("2b")) {
                logLine = parseV2bLogLine(jsonLine);
            } else if (version.equals("2c")) {
                logLine = parseV2cLogLine(jsonLine);
            } else {
                throw new ParseException("unknown line version");
            }
        } catch (ParseException e) {
            LOGGER.warn("Discarding line: Unparsable.\nLine was:\n" + line, e);
            logLine = null;
        }
        return Optional.fromNullable(logLine);
    }

    private LogLine parseV2cLogLine(Map<String, Object> line) throws ParseException {
        TreeMap<String, CounterVariable> variables = new TreeMap<>();
        parseChunk(line, "timers", CounterVariable.MetricKind.Timer, variables);
        parseChunk(line, "counters", CounterVariable.MetricKind.Counter, variables);
        parseChunk(line, "gauges", CounterVariable.MetricKind.Gauge, variables);

        final Map<String, String> annotations;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> parsedAnnotations = (Map<String, String>) line.get("annotations");
            annotations = parsedAnnotations;
        } catch (Exception e) {
            throw new ParseException("could not cast annotations to look for timestamp", e);
        }

        if (annotations == null) {
            throw new ParseException("no timestamp found in log line");
        }

        DateTime timestamp = getTimestamp(annotations);

        return new StandardLogLine(variables, timestamp);
    }

    private void parseChunk(Map<String, Object> lineData, String element, CounterVariable.MetricKind kind,
                            Map<String, CounterVariable> variables) throws ParseException {
        final Map<String, ArrayList<Object>> elements;
        try {
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<Object>> parsedElements = (Map<String, ArrayList<Object>>) lineData.get(element);
            elements = parsedElements;
        } catch (Exception e) {
            throw new ParseException("could not cast element " + element, e);
        }
        if (elements != null) {
            for (Map.Entry<String, ArrayList<Object>> entry : elements.entrySet()) {
                ArrayList<Object> lineValues = entry.getValue();
                ArrayList<Double> parsedValues = new ArrayList<Double>();
                for (Object val : lineValues) {
                    try {
                        parsedValues.add(Double.valueOf(val.toString()));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("skipping value due to not being able to parse as double: " + val, e);
                    }
                }
                CounterVariable cv = new CounterVariable(kind, parsedValues);
                variables.put(entry.getKey().toString(), cv);
            }
        }
    }
}
