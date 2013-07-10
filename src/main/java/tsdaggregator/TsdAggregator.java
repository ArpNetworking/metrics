/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator;

import org.apache.commons.cli.*;
import org.apache.commons.io.input.Tailer;
import org.apache.log4j.Logger;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author brandarp
 */
public class TsdAggregator {

    static final Logger _Logger = Logger.getLogger(TsdAggregator.class);
    private static final String REMET_DEFAULT_URI = "http://localhost:7090/report";
    private static final String MONITORD_DEFAULT_URI = "http://monitord:8080/results";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                _Logger.error("Unhandled exception!", throwable);
            }
        });


        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("input_file").withLongOpt("file").hasArg().withDescription("file to be parsed").create("f"));
        options.addOption(OptionBuilder.withArgName("service").withLongOpt("service").hasArg().withDescription("service name").create("s"));
        options.addOption(OptionBuilder.withArgName("host").withLongOpt("host").hasArg().withDescription("host the metrics were generated on").create("h"));
        options.addOption(OptionBuilder.withArgName("cluster").withLongOpt("cluster").hasArg().withDescription("name of the cluster the host is in").create("c"));
        options.addOption(OptionBuilder.withArgName("uri").withLongOpt("uri").hasArg().withDescription("metrics server uri").create("u"));
        options.addOption(OptionBuilder.withArgName("output_file").withLongOpt("output").hasArg().withDescription("output file").create("o"));
        options.addOption(OptionBuilder.withArgName("parser").withLongOpt("parser").hasArg().withDescription("parser to use to parse log lines").create("p"));
        options.addOption(OptionBuilder.withArgName("period").withLongOpt("period").hasArgs().withDescription("aggregation time period in ISO 8601 standard notation (multiple allowed)").create("d"));
        options.addOption(OptionBuilder.withArgName("stat").withLongOpt("statistic").hasArgs().withDescription("statistic of aggregation to record (multiple allowed)").create("t"));
        options.addOption(OptionBuilder.withArgName("extension").withLongOpt("extension").hasArgs().withDescription("extension of files to parse - uses a union of arguments as a regex (multiple allowed)").create("e"));
        options.addOption(OptionBuilder.withLongOpt("tail").hasArg(false).withDescription("\"tail\" or follow the file and do not terminate").create("l"));
        options.addOption(OptionBuilder.withLongOpt("rrd").hasArg(false).withDescription("create or write to rrd databases").create());
        options.addOption(OptionBuilder.withLongOpt("remet").hasArg(false).withDescription("send data to a local remet server").create());
        options.addOption(OptionBuilder.withLongOpt("monitord").hasArg(false).withDescription("send data to a monitord server").create());
        CommandLineParser parser = new PosixParser();
        CommandLine cl;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println("error parsing options: " + e1.getMessage());
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

        if (!cl.hasOption("u") && !cl.hasOption("o") && !cl.hasOption("remet") && !cl.hasOption("monitord")) {
            System.err.println("metrics server uri or output file not specified");
            printUsage(options);
            return;
        }

        Class parserClass = QueryLogLineData.class;
        if (cl.hasOption("p")) {
            String lineParser = cl.getOptionValue("p");
            try {
                parserClass = Class.forName(lineParser);
                if (!LogLine.class.isAssignableFrom(parserClass)) {
                    _Logger.error("parser class [" + lineParser + "] does not implement required LogLine interface");
                    return;
                }
            } catch (ClassNotFoundException ex) {
                _Logger.error("could not find parser class [" + lineParser + "] on classpath");
                return;
            }
        }

        String[] periodOptions = {"PT1M", "PT5M", "PT1H"};
        if (cl.hasOption("remet")) {
            periodOptions = new String[] {"PT1S"};
        }

        if (cl.hasOption("d")) {
            periodOptions = cl.getOptionValues("d");
        }


        Pattern filter = Pattern.compile(".*");
        if (cl.hasOption("e")) {
            String[] filters = cl.getOptionValues("e");
            StringBuilder builder = new StringBuilder();
            int x = 0;
            for (String f : filters) {
                if (x > 0) {
                    builder.append(" || ");
                }
                builder.append("(").append(f).append(")");
            }
            filter = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
        }

        Boolean tailFile = cl.hasOption("tail") || cl.hasOption("remet");

        Set<Statistic> statisticsClasses = new HashSet<Statistic>();
        if (cl.hasOption("t")) {
            String[] statisticsStrings = cl.getOptionValues("t");
            for (String statString : statisticsStrings) {
                try {
                    _Logger.info("Looking up statistic " + statString);
                    Class statClass = ClassLoader.getSystemClassLoader().loadClass(statString);
                    Statistic stat;
                    try {
                        stat = (Statistic)statClass.newInstance();
                        statisticsClasses.add(stat);
                    } catch (InstantiationException ex) {
                        _Logger.error("Could not instantiate statistic [" + statString + "]", ex);
                        return;
                    } catch (IllegalAccessException ex) {
                        _Logger.error("Could not instantiate statistic [" + statString + "]", ex);
                        return;
                    }
                    if (!Statistic.class.isAssignableFrom(statClass)) {
                        _Logger.error("Statistic class [" + statString + "] does not implement required Statistic interface");
                        return;
                    }
                } catch (ClassNotFoundException ex) {
                    _Logger.error("could not find statistic class [" + statString + "] on classpath");
                    return;
                }
            }
        } else if (cl.hasOption("remet")) {
            statisticsClasses.add(new NStatistic());
            statisticsClasses.add(new TP100());
            statisticsClasses.add(new TP99());
            statisticsClasses.add(new TP90());
            statisticsClasses.add(new MeanStatistic());
        } else {
            statisticsClasses.add(new TP0());
            statisticsClasses.add(new TP50());
            statisticsClasses.add(new TP100());
            statisticsClasses.add(new TP90());
            statisticsClasses.add(new TP99());
            statisticsClasses.add(new TP99p9());
            statisticsClasses.add(new MeanStatistic());
            statisticsClasses.add(new NStatistic());
        }

        String fileName = cl.getOptionValue("f");
        String hostName = cl.getOptionValue("h");
        if (!cl.hasOption("h")) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                _Logger.error("could not lookup hostname of local machine", e);
                System.err.println("host name not specified and could not determine hostname automatically, please specify explicitly");
                printUsage(options);
                return;
            }
        }

        String cluster = cl.getOptionValue("c");
        String serviceName = cl.getOptionValue("s");
        String metricsUri = "";
        String outputFile = "";
        Boolean outputRRD = false;
        if (cl.hasOption("u")) {
            metricsUri = cl.getOptionValue("u");
        } else if (cl.hasOption("remet")) {
            metricsUri = REMET_DEFAULT_URI;
        } else if (cl.hasOption("monitord")) {
            metricsUri = MONITORD_DEFAULT_URI;
        }

        if (cl.hasOption("o")) {
            outputFile = cl.getOptionValue("o");
        }

        if (cl.hasOption("rrd")) {
            outputRRD = true;
        }

        _Logger.info("using file " + fileName);
        _Logger.info("using cluster " + cluster);
        _Logger.info("using hostname " + hostName);
        _Logger.info("using servicename " + serviceName);
        _Logger.info("using uri " + metricsUri);
        _Logger.info("using output file " + outputFile);
        _Logger.info("using filter (" + filter.pattern() + ")");
        if (outputRRD) {
            _Logger.info("outputting rrd files");
        }

        Set<Period> periods = new HashSet<Period>();
        PeriodFormatter periodParser = ISOPeriodFormat.standard();
        for (String p : periodOptions) {
            periods.add(periodParser.parsePeriod(p));
        }

        MultiListener listener = new MultiListener();
        if (!metricsUri.equals("") && (!options.hasOption("remet") && !options.hasOption("monitord"))) {
            AggregationListener httpListener = new HttpPostListener(metricsUri);
            listener.addListener(new BufferingListener(httpListener, 50));
        }

        if (!metricsUri.equals("") && options.hasOption("remet")) {
            AggregationListener httpListener = new HttpPostListener(metricsUri);
            //we don't want to buffer remet responses
            listener.addListener(httpListener);
        }

        if (!metricsUri.equals("") && options.hasOption("monitord")) {
            AggregationListener monitordListener = new MonitordListener(metricsUri, cluster, hostName);
            listener.addListener(monitordListener);
        }

        if (!outputFile.equals("")) {
            AggregationListener fileListener = new FileListener(outputFile);
            //listener = new BufferingListener(fileListener, 500);
            listener.addListener(fileListener);
        }

        if (outputRRD) {
            listener.addListener(new RRDClusterListener());
        }

        Map<String, TSData> aggregations = new ConcurrentHashMap<>();

        ArrayList<String> files = new ArrayList<>();
        File file = new File(fileName);
        if (file.isFile()) {
            files.add(fileName);
        } else if (file.isDirectory()) {
            _Logger.info("File given is a directory, will recursively process");
            findFilesRecursive(file, files, filter);
        }
        for (String f : files) {
            try {
                _Logger.info("Reading file " + f);

                LineProcessor processor = new LineProcessor(parserClass, statisticsClasses, hostName, serviceName, periods, listener, aggregations);
                if (tailFile) {
                    File fileHandle = new File(f);
                    LogTailerListener tailListener = new LogTailerListener(processor);
                    Tailer t = Tailer.create(fileHandle, tailListener, 500l, false);
                }
                else {
                    //check the first 4 bytes of the file for utf markers
                    FileInputStream fis = new FileInputStream(f);
                    byte[] header = new byte[4];
                    if (fis.read(header) < 4) {
                        //If there are less than 4 bytes, we should move on
                        continue;
                    }
                    String encoding = "UTF-8";
                    if (header[0] == -1 && header[1] == -2) {
                        _Logger.info("Detected UTF-16 encoding");
                        encoding = "UTF-16";
                    }

                    InputStreamReader fileReader = new InputStreamReader(new FileInputStream(f), Charset.forName(encoding));
                    BufferedReader reader = new BufferedReader(fileReader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (processor.invoke(line))
                            return;
                    }
                }

                //close all aggregations
                for (Map.Entry<String, TSData> entry : aggregations.entrySet()) {
                    entry.getValue().close();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long rotationCheck = 30000;
        long rotateOn = 60000;
        if (options.hasOption("remet")) {
            rotationCheck = 500;
            rotateOn = 1000;
        }


        if (tailFile) {
            while (true) {
                try {
                    Thread.sleep(rotationCheck);
                    //_Logger.info("Checking rotations on " + aggregations.size() + " TSData objects");
                    for (Map.Entry<String, TSData> entry : aggregations.entrySet()) {
                        //_Logger.info("Check rotate on " + entry.getKey());
                        entry.getValue().checkRotate(rotateOn);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    _Logger.error("Interrupted!", e);
                }
            }
        }
        listener.close();
    }

    private static void findFilesRecursive(File dir, ArrayList<String> files, Pattern filter) {
        String[] list = dir.list();
        Arrays.sort(list);
        for (String f : list) {
            File entry = new File(dir, f);
            if (entry.isFile()) {
                Matcher m = filter.matcher(entry.getPath());
                if (m.find()) {
                    files.add(entry.getAbsolutePath());
                } else {
                }
            } else if (entry.isDirectory()) {
                findFilesRecursive(entry, files, filter);
            }
        }
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tsdaggregator", options, true);
    }
}
