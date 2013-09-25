/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator.publishing;

import com.google.common.base.Charsets;
import org.apache.log4j.Logger;
import tsdaggregator.AggregatedData;

import java.io.*;
import java.text.DecimalFormat;
import java.text.Format;


/**
 *
 * @author brandarp
 */
public class FilePublisher implements AggregationPublisher {

    String _FileName = "";
    Writer _Writer;
    static final Logger _Logger = Logger.getLogger(FilePublisher.class);
    long _LinesWritten = 0;
    Format doubleFormat = new DecimalFormat("#.##");

    public FilePublisher(String file) {
        _FileName = file;
        try {
            _Writer = new OutputStreamWriter(new FileOutputStream(_FileName, true), Charsets.UTF_8);
        } catch (IOException ex) {
            _Logger.error("Could not open output file for writing: " + _FileName, ex);
            return;
        }
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        _Logger.debug("Writing aggregation data to FilePublisher, " + data.length + " records to file " + _FileName);
        
        StringBuilder sb = new StringBuilder();
        if (data.length > 0) {
            for (AggregatedData d : data) {

                sb.append("{\"value\":\"").append(doubleFormat.format(d.getValue()))
                        .append("\",\"counter\":\"").append(d.getMetric())
                        .append("\",\"service\":\"").append(d.getService())
                        .append("\",\"host\":\"").append(d.getHost())
                        .append("\",\"period\":\"").append(d.getPeriod())
                        .append("\",\"periodStart\":\"").append(d.getPeriodStart())
                        .append("\",\"statistic\":\"").append(d.getStatistic().getName())
                        .append("\"}\n");
                _LinesWritten++;
            }
        }
        try {
            _Writer.append(sb.toString());
            _Writer.flush();
        } catch (IOException ex) {
            _Logger.error("Error writing to output file", ex);
        }
    }

    @Override
    public void close() {
        try {
            _Logger.info("Closing after " + _LinesWritten + " lines written.");
            _Writer.flush();
            _Writer.close();
        } catch (IOException ex) {
            _Logger.error("Error writing to output file", ex);
        }
    }
}
