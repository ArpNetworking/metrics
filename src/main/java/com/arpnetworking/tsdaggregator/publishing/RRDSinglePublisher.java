package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.google.common.base.Charsets;
import org.apache.log4j.Logger;
import org.joda.time.Period;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A publisher that stores the data in rrdtool.
 *
 * @author barp
 */
public class RRDSinglePublisher {
    private static final Logger LOGGER = Logger.getLogger(RRDSinglePublisher.class);
    private final String _fileName;
    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("#.####");

    public RRDSinglePublisher(@Nonnull AggregatedData data) {
        String rrdName = data.getHost() + "." + data.getMetric() + "." + data.getPeriod().toString() +
                data.getStatistic().getName() + ".rrd";
        rrdName = rrdName.replace("/", "-");
        String before = rrdName;
        rrdName = rrdName.replaceAll("-\\d+", "");
        if (!before.equals(rrdName)) {
            LOGGER.info("replaced a numeric chunk: " + before + " became " + rrdName);
        }
        _fileName = rrdName;
        @Nonnull Long startTime = data.getPeriodStart().getMillis() / 1000;
        createRRDFile(rrdName, data.getPeriod(), startTime);
    }

    private void createRRDFile(String rrdName, @Nonnull Period period, @Nonnull Long startTime) {
        if (new File(rrdName).exists()) {
            return;
        }
        LOGGER.info("Creating rrd file " + rrdName);
        @Nonnull String[] argsList = new String[]{"rrdtool", "create", rrdName, "-b", startTime.toString(), "-s",
                Integer.toString(period.toStandardSeconds().getSeconds()),
                "DS:" + "metric" + ":GAUGE:" + Integer.toString(period.toStandardSeconds().getSeconds() * 3) + ":U:U",
                "RRA:AVERAGE:0.5:1:1000"};
        executeProcess(argsList);
    }

    private void executeProcess(@Nonnull String[] args) {
        @Nullable BufferedReader stdOut = null;
        try {
            @Nonnull ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            stdOut = new BufferedReader(new InputStreamReader(p.getInputStream(), Charsets.UTF_8));

            String line;
            @Nonnull StringBuilder procOutput = new StringBuilder();
            while ((line = stdOut.readLine()) != null) {
                procOutput.append(line).append("\n");
            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted waiting for process to exit", e);
            }
            if (p.exitValue() != 0) {
                @Nonnull StringBuilder builder = new StringBuilder();
                for (String arg : args) {
                    builder.append(arg).append(" ");
                }
                LOGGER.error("executed: " + builder.toString());
                LOGGER.error("Process exit code " + p.exitValue());
                LOGGER.error("Process output:\n" + procOutput.toString());

            }
        } catch (IOException e) {
            LOGGER.error("IOException while trying to create RRD file", e);
        } finally {
            if (stdOut != null) {
                try {
                    stdOut.close();
                } catch (IOException ignored) {
                }
            }

        }
    }

    public void storeData(@Nonnull AggregatedData data) {
        @Nonnull Long unixTime = data.getPeriodStart().getMillis() / 1000;
        @Nonnull String value = unixTime.toString() + ":" + DOUBLE_FORMAT.format(data.getValue());
        @Nonnull String[] argsList = new String[]{"rrdtool", "update", _fileName, value};
        executeProcess(argsList);
    }
}
