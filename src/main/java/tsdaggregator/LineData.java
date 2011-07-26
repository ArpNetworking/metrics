package tsdaggregator;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.codehaus.jackson.map.*;

public class LineData {

    Map<String, ArrayList<Double>> _Variables = new HashMap<String, ArrayList<Double>>();
    DateTime _Time = new DateTime(0);
    static final Logger _Logger = Logger.getLogger(LineData.class);

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
                    ArrayList values = new ArrayList();
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

    public void parseV2aLogLine(Map<String, Object> line) {
        Map<?, ?> counters = (Map) line.get("counters");
        for (Map.Entry entry : counters.entrySet()) {
            ArrayList<Double> counter = new ArrayList<Double>();
            counter.add(Double.parseDouble(entry.getValue().toString()));


            _Variables.put(entry.getKey().toString(), counter);
        }

        Map<?, ?> timers = (Map) line.get("timers");
        for (Map.Entry entry : timers.entrySet()) {

            _Variables.put(entry.getKey().toString(), (ArrayList<Double>) entry.getValue());
        }

        if (_Variables.containsKey("initTimestamp")) {
            Double time = _Variables.get("initTimestamp").get(0);
            //double with whole number unix time, and fractional seconds
            Long ticks = Math.round(time * 1000);
            _Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
            _Variables.remove("initTimestamp");
        }
    }

    public void parseLogLine(String line) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> jsonLine = mapper.readValue(line, Map.class);
            String version = (String) jsonLine.get("version");
            if (version.equals("2a")) {
                parseV2aLogLine(jsonLine);
            }
        } catch (IOException e) {
            _Logger.warn("Legacy, non-json tsd line found: ", e);
            parseLegacyLogLine(line);
        }
    }

    public DateTime getTime() {
        return _Time;
    }

    public Map<String, ArrayList<Double>> getVariables() {
        return _Variables;
    }
}
