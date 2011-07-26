/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tsdaggregator;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.joda.time.Period;

/**
 *
 * @author brandarp
 */
public class TsdAggregator {
	
	static final Logger _Logger = Logger.getLogger(TsdAggregator.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	
    	Options options = new Options();
    	options.addOption("f", "file", true, "file to be parsed");
    	options.addOption("s", "service", true, "service name");
    	options.addOption("h", "host", true, "host the metrics were generated on");
    	options.addOption("u", "uri", true, "metrics server uri");
    	CommandLineParser parser = new PosixParser();
    	CommandLine cl;
    	try {
			cl = parser.parse(options, args);
		} catch (ParseException e1) {
			printUsage(options);
			return;
		}
		
		if (!cl.hasOption("f")) {
			System.err.println("no file found, must specify file on the command line");
			printUsage(options);
			return;
		}
		
		if (!cl.hasOption("s")) {
			System.err.println("service name must be specified");
			printUsage(options);
			return;
		}
		
		if (!cl.hasOption("h")) {
			System.err.println("host name must be specified");
			printUsage(options);
			return;
		}
		
		if (!cl.hasOption("u")) {
			System.err.println("metrics server uri not specified");
			printUsage(options);
			return;
		}
		
		_Logger.info("using file " + cl.getOptionValue("f"));
		_Logger.info("using hostname " + cl.getOptionValue("h"));
		_Logger.info("using servicename " + cl.getOptionValue("s"));
		_Logger.info("using uri " + cl.getOptionValue("u"));
		
		Set<Period> defaultPeriods = new HashSet<Period>();
		defaultPeriods.add(Period.minutes(1));
		defaultPeriods.add(Period.minutes(5));
		defaultPeriods.add(Period.minutes(60));
		
    	AggregationListener httpListener = new HttpPostListener(cl.getOptionValue("u"));
    	AggregationListener listener = new BufferingListener(httpListener, 50);
    	
        HashMap<String, TSData> aggregations = new HashMap<String, TSData>();
        try {
			FileReader fileReader = new FileReader(cl.getOptionValue("f"));
			BufferedReader reader = new BufferedReader(fileReader);
			String line;
			while ((line = reader.readLine()) != null) {
				//System.out.println(line);
				LineData data = new LineData();
				data.parseLogLine(line);
				for (Map.Entry<String, ArrayList<Double>> entry : data.getVariables().entrySet()) {
					TSData tsdata = aggregations.get(entry.getKey());
					if (tsdata == null) {
						tsdata = new TSData(entry.getKey(), defaultPeriods, listener, cl.getOptionValue("h"), cl.getOptionValue("s"));
						aggregations.put(entry.getKey(), tsdata);
					}
					tsdata.addMetric(entry.getValue(), data.getTime());
				}
			}
			
			//close all aggregations
			for (Map.Entry<String, TSData> entry : aggregations.entrySet()) {
				entry.getValue().close();
			}
			listener.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void printUsage(Options options)
    {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("tsdaggregator", options, true);
    }
}
