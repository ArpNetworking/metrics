/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.google.common.base.Charsets;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.Format;


/**
 * Publisher to write data to a file.
 *
 * @author barp
 */
public class FilePublisher implements AggregationPublisher {

    private String _fileName = "";
    private Writer _writer;
    private static final Logger LOGGER = Logger.getLogger(FilePublisher.class);
    private long _linesWritten = 0;
    private static final Format DOUBLE_FORMAT = new DecimalFormat("#.##");

    public FilePublisher(String file) {
        _fileName = file;
        try {
            _writer = new OutputStreamWriter(new FileOutputStream(_fileName, true), Charsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("Could not open output file for writing: " + _fileName, ex);
            return;
        }
    }

    @Override
    public void recordAggregation(AggregatedData[] data) {
        LOGGER.debug("Writing aggregation data to FilePublisher, " + data.length + " records to file " + _fileName);

        StringBuilder sb = new StringBuilder();
        if (data.length > 0) {
            for (AggregatedData d : data) {

                sb.append("{\"value\":\"").append(DOUBLE_FORMAT.format(d.getValue()))
                        .append("\",\"counter\":\"").append(d.getMetric())
                        .append("\",\"service\":\"").append(d.getService())
                        .append("\",\"host\":\"").append(d.getHost())
                        .append("\",\"period\":\"").append(d.getPeriod())
                        .append("\",\"periodStart\":\"").append(d.getPeriodStart())
                        .append("\",\"statistic\":\"").append(d.getStatistic().getName())
                        .append("\"}\n");
                _linesWritten++;
            }
        }
        try {
            _writer.append(sb.toString());
            _writer.flush();
        } catch (IOException ex) {
            LOGGER.error("Error writing to output file", ex);
        }
    }

    @Override
    public void close() {
        try {
            LOGGER.info("Closing after " + _linesWritten + " lines written.");
            _writer.flush();
            _writer.close();
        } catch (IOException ex) {
            LOGGER.error("Error writing to output file", ex);
        }
    }
}
