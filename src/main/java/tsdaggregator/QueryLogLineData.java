package tsdaggregator;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QueryLogLineData implements LogLine {

    Map<String, ArrayList<Double>> _Variables = new HashMap<String, ArrayList<Double>>();
    DateTime _Time = new DateTime(0);
    static final Logger _Logger = Logger.getLogger(QueryLogLineData.class);

    private void parseLegacyLogLine(String line) {
        HashMap<String, ArrayList<Double>> vals = new HashMap<String, ArrayList<Double>>();
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
                    vals.put(key, values);
                }
            }
        }

        for (String remove : removalCandidates) {
            if (vals.containsKey(remove) && vals.get(remove).get(0) == 0.0d) {
                vals.remove(remove);
                _Logger.warn("removing unfinished timer [" + remove + "] from timing set");
            }
        }
        if (vals.containsKey("initTimestamp")) {
            Double time = vals.get("initTimestamp").get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            _Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
            vals.remove("initTimestamp");
        }

        _Variables = vals;
    }

    @SuppressWarnings("unchecked")
    public void parseV2aLogLine(Map<String, Object> line) {
        Map<String, Double> counters = (Map<String, Double>) line.get("counters");
        for (Map.Entry<String, Double> entry : counters.entrySet()) {
            ArrayList<Double> counter = new ArrayList<Double>();
            counter.add(Double.parseDouble(entry.getValue().toString()));

            _Variables.put(entry.getKey().toString(), counter);
        }

        Map<String, ArrayList<Double>> timers = (Map<String, ArrayList<Double>>) line.get("timers");
        for (Map.Entry<String, ArrayList<Double>> entry : timers.entrySet()) {

            _Variables.put(entry.getKey().toString(), entry.getValue());
        }

        if (_Variables.containsKey("initTimestamp")) {
            Double time = _Variables.get("initTimestamp").get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            _Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
            _Variables.remove("initTimestamp");
        }
    }

    @SuppressWarnings("unchecked")
    public void parseV2bLogLine(Map<String, Object> line) {
        Map<String, Double> counters = (Map<String, Double>) line.get("counters");
        for (Map.Entry<String, Double> entry : counters.entrySet()) {
            ArrayList<Double> counter = new ArrayList<Double>();
            counter.add(Double.parseDouble(entry.getValue().toString()));

            _Variables.put(entry.getKey().toString(), counter);
        }

        Map<String, ArrayList<Object>> timers = (Map<String, ArrayList<Object>>) line.get("timers");
        for (Map.Entry<String, ArrayList<Object>> entry : timers.entrySet()) {
            ArrayList<Object> vals = entry.getValue();
            ArrayList<Double> newVals = new ArrayList<Double>();
            for (Object val : vals) {
                newVals.add(Double.valueOf(val.toString()));
            }
            _Variables.put(entry.getKey().toString(), newVals);
        }

        Map<String, String> annotations = (Map<String, String>)line.get("annotations");
        if (annotations.containsKey("finalTimestamp")) {
            Double time = Double.parseDouble(annotations.get("finalTimestamp"));
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            _Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
        }
        else if (annotations.containsKey("initTimestamp")) {
            Double time = Double.parseDouble(annotations.get("initTimestamp"));
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            _Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parseLogLine(String line) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> jsonLine = mapper.readValue(line, Map.class);
            String version = (String) jsonLine.get("version");
            if (version.equals("2a")) {
                parseV2aLogLine(jsonLine);
            }
            else if (version.equals("2b")) {
                parseV2bLogLine(jsonLine);
            }
       
        } catch (IOException ex) {
            _Logger.warn("Possible legacy, non-json tsd line found: ", ex);
            try {
                parseLegacyLogLine(line);
            }
            catch (Exception e) {
                _Logger.warn("Discarding line: Unparsable.\nLine was:\n" + line, e);
            }
        }
    }

    @Override
    public DateTime getTime() {
        return _Time;
    }

    @Override
    public Map<String, ArrayList<Double>> getVariables() {
        return _Variables;
    }
}
