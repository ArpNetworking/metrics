package tsdaggregator;

import org.apache.log4j.Logger;
import org.joda.time.Period;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/15/12
 * Time: 2:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class RRDSingleListener {
    static final Logger _Logger = Logger.getLogger(RRDSingleListener.class);
    private String _FileName;
    private DecimalFormat doubleFormat = new DecimalFormat("#.####");

    public RRDSingleListener(AggregatedData data) {
        String rrdName = data.getHost() + "." + data.getMetric() + "." + data.getPeriod().toString() + data.getStatistic().getName() + ".rrd";
        rrdName = rrdName.replace("/", "-");
        String before = rrdName;
        rrdName = rrdName.replaceAll("-\\d+", "");
        if (!before.equals(rrdName)) {
            _Logger.info("replaced a numeric chunk: " + before + " became " + rrdName);
        }
        _FileName = rrdName;
        Long startTime = data.getPeriodStart().getMillis() / 1000;
        createRRDFile(rrdName, data.getPeriod(), data.getMetric(), startTime);
    }

    private void createRRDFile(String rrdName, Period period, String dsName, Long startTime) {
        if (new File(rrdName).exists()) {
            return;
        }
        _Logger.info("Creating rrd file " + rrdName);
        String[] argsList = new String [] {"rrdtool", "create", rrdName, "-b", startTime.toString(), "-s", Integer.toString(period.toStandardSeconds().getSeconds()),
                "DS:" + "metric" + ":GAUGE:" + Integer.toString(period.toStandardSeconds().getSeconds() * 3) + ":U:U",
                "RRA:AVERAGE:0.5:1:1000"};
        executeProcess(argsList);
    }

    private void executeProcess(String[] args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            StringBuilder procOutput = new StringBuilder();
            while ((line = stdOut.readLine()) != null) {
                procOutput.append(line + "\n");
            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                _Logger.error("Interrupted waiting for process to exit", e);
            }
            if (p.exitValue() != 0) {
                StringBuilder builder = new StringBuilder();
                for (String arg : args) {
                    builder.append(arg).append(" ");
                }
                _Logger.error("executed: " + builder.toString());
                _Logger.error("Process exit code " + p.exitValue());
                _Logger.error("Process output:\n" + procOutput.toString());

            }
        } catch (IOException e) {
            _Logger.error("IOException while trying to create RRD file", e);
        }
    }

    public void storeData(AggregatedData data) {
        Long unixTime = data.getPeriodStart().getMillis() / 1000;
        String value = unixTime.toString() + ":" + doubleFormat.format(data.getValue());
        String[] argsList = new String [] {"rrdtool", "update", _FileName, value};
        executeProcess(argsList);
    }
}
