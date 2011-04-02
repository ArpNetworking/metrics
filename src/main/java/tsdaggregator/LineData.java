package tsdaggregator;

import java.util.*;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

public class LineData {
	Map<String, Double> _Variables = new HashMap<String, Double>();
	DateTime _Time = new DateTime(0);
	static final Logger _Logger = Logger.getLogger(LineData.class);
	
	public void parseLogLine(String line) {
		HashMap<String, Double> vals = new HashMap<String, Double>();
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
    			}
    			else {
    				vals.put(key, value);
    			}
    		}
    	}
    	
    	for (String remove : removalCandidates) {
    		if (vals.containsKey(remove) && vals.get(remove) == 0.0d) {
    			vals.remove(remove);
    			_Logger.warn("removing unfinished timer [" + remove + "] from timing set");
    		}
    	}
    	if (vals.containsKey("initTimestamp")) {
	    	Double time = vals.get("initTimestamp");
	    	//double with whole number unix time, and fractional seconds
	    	Long ticks = Math.round(time * 1000);  
	    	_Time = new DateTime(ticks, ISOChronology.getInstanceUTC());
	    	vals.remove("initTimestamp");
		}
    	
    	_Variables = vals;
	}
	
	public DateTime getTime() {
		return _Time;
	}
	
	public Map<String, Double> getVariables()
	{
		return _Variables;
	}
}
