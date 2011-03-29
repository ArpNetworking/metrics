package tsdaggregator;

import java.util.*;

import org.joda.time.DateTime;

public class LineData {
	Map<String, Double> _Variables = new HashMap<String, Double>();
	DateTime _Time = new DateTime(0);
	
	public void parseLogLine(String line) {
		HashMap<String, Double> vals = new HashMap<String, Double>();
    	line.trim();
    	line = line.replace("[", "");
    	line = line.replace("]", "");
    	
    	String[] vars = line.split(",");
    	for (String var : vars) {
    		if (var.length() > 0) {
    			String[] splits = var.split("=");
    			Double value = Double.parseDouble(splits[1]);
    			vals.put(splits[0].trim(), value);
    		}
    	}
    	Double time = vals.get("timestamp");
    	//double with whole number unix time, and fractional seconds
    	Long ticks = Math.round(time * 1000);  
    	_Time = new DateTime(ticks);
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
