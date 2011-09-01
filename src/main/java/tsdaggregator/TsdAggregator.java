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
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

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
        options.addOption("p", "parser", true, "parser to use to parse log lines");
        options.addOption("d", "period", true, "aggregation time period in ISO 8601 standard notation (multiple allowed)");
        options.addOption("t", "statistic", true, "statistic of aggregation to record (multiple allowed)");
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
        
        Class parserClass = QueryLogLineData.class;
        if (cl.hasOption("p")) {
            String lineParser = cl.getOptionValue("p");
            try {
                parserClass = Class.forName(lineParser);
                if (!LogLine.class.isAssignableFrom(parserClass)) {
                    _Logger.error("parser class [" + lineParser + "] does not implement requried LogLine interface");
                    return;
                }
            } catch (ClassNotFoundException ex) {
                _Logger.error("could not find parser class [" + lineParser + "] on classpath");
                return;
            }
        }
        
        String[] periodOptions = {"PT1M", "PT5M", "PT1H"};
        if (cl.hasOption("d")) {
            periodOptions = cl.getOptionValues("d");
        }

        String fileName = cl.getOptionValue("f");
        String hostName = cl.getOptionValue("h");
        String serviceName = cl.getOptionValue("s");
        String metricsUri = cl.getOptionValue("u");
        _Logger.info("using file " + fileName);
        _Logger.info("using hostname " + hostName);
        _Logger.info("using servicename " + serviceName);
        _Logger.info("using uri " + metricsUri);

        Set<Period> periods = new HashSet<Period>();
        PeriodFormatter periodParser = ISOPeriodFormat.standard();
        for (String p : periodOptions) {
            periods.add(periodParser.parsePeriod(p));
        }

        AggregationListener httpListener = new HttpPostListener(metricsUri);
        AggregationListener listener = new BufferingListener(httpListener, 50);

        HashMap<String, TSData> aggregations = new HashMap<String, TSData>();
        
        ArrayList<String> files = new ArrayList<String>();
        File file = new File(fileName);
        if (file.isFile()) {
            files.add(fileName);
        } else if (file.isDirectory()) {
            _Logger.info("File given is a directory, will recursively process");
            findFilesRecursive(file, files);
        }
        for (String f : files) {
            try {
                _Logger.info("Reading file " + f);
                FileReader fileReader = new FileReader(f);
                BufferedReader reader = new BufferedReader(fileReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    //System.out.println(line);
                    LogLine data = null;
                    try {
                        data = (LogLine)parserClass.newInstance();
                    } catch (InstantiationException ex) {
                        _Logger.error("Could not instantiate LogLine parser", ex);
                        return;
                    } catch (IllegalAccessException ex) {
                        _Logger.error("Could not instantiate LogLine parser", ex);
                        return;
                    }

                    data.parseLogLine(line);
                    for (Map.Entry<String, ArrayList<Double>> entry : data.getVariables().entrySet()) {
                        TSData tsdata = aggregations.get(entry.getKey());
                        if (tsdata == null) {
                            tsdata = new TSData(entry.getKey(), periods, listener, hostName, serviceName);
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
    }
    
    private static void findFilesRecursive(File dir, ArrayList<String> files) {
        String[] list = dir.list();
        Arrays.sort(list);
        for (String f : list) {
            File entry = new File(dir, f);
            if (entry.isFile()) {
                files.add(entry.getAbsolutePath());
            } else if (entry.isDirectory()) {
                findFilesRecursive(entry, files);
            }
        }
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tsdaggregator", options, true);
    }
}
