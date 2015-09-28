/**
 * Copyright 2014 Brandon Arp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * RRD publisher that maintains all the rrd databases for a cluster. This class
 * is not thread safe.
 *
 * TODO(vkoskela): Make this class thread safe [MAI-100]
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class RrdSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final PeriodicData periodicData) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", periodicData.getData().size())
                .addData("conditionsSize", periodicData.getConditions().size())
                .log();

        for (final AggregatedData datum : periodicData.getData()) {
            if (!datum.isSpecified()) {
                continue;
            }
            final String name = (datum.getHost() + "."
                    + datum.getFQDSN().getMetric() + "."
                    + datum.getPeriod().toString()
                    + datum.getFQDSN().getStatistic().getName()
                    + ".rrd").replace("/", "-");

            RrdNode listener = _listeners.get(name);
            if (listener == null) {
                listener = new RrdNode(name);
                _listeners.put(name, listener);
            }
            listener.storeData(datum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {}

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("super", super.toLogValue())
                .put("path", _path)
                .put("rrdTool", _rrdTool)
                .build();
    }

    private final HashMap<String, RrdNode> _listeners = Maps.newHashMap();

    private RrdSink(final Builder builder) {
        super(builder);
        _path = builder._path;
        _rrdTool = builder._rrdTool;
    }

    private final String _path;
    private final String _rrdTool;

    private static final Logger LOGGER = LoggerFactory.getLogger(RrdSink.class);

    /**
     * Implementation of builder pattern for <code>RrdClusterSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, RrdSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(RrdSink.class);
        }

        /**
         * The path to the RRD root. Cannot be null or empty.
         *
         * @param value The path to the RRD root.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPath(final String value) {
            _path = value;
            return self();
        }

        /**
         * The RRD tool to use. Cannot be null or empty. Default is "rrdtool".
         *
         * @param value The RRD tool to use.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setRrdTool(final String value) {
            _rrdTool = value;
            return self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        @NotEmpty
        private String _path;
        @NotNull
        @NotEmpty
        private String _rrdTool = "rrdtool";
    }

    private final class RrdNode {

        public RrdNode(final String name) {
            _fileName = _path + File.separator + name;
        }

        public void storeData(final AggregatedData data) {
            final long startTimeEpochInSeconds = data.getPeriodStart().getMillis() / 1000;
            createRRDFile(data.getPeriod(), startTimeEpochInSeconds);
            final String value = startTimeEpochInSeconds + ":" + String.format("%f", data.getValue().getValue());
            final String[] arguments = new String[] {
                _rrdTool,
                "update",
                _fileName,
                value };
            executeProcess(arguments);
        }

        private void createRRDFile(final Period period, final long startTime) {
            if (new File(_fileName).exists()) {
                return;
            }
            LOGGER.info()
                    .setMessage("Creating rrd file")
                    .addData("sink", getName())
                    .addData("fileName", _fileName)
                    .log();
            // TODO(barp): Address assumptions on type and timing [MAI-101]
            // Also add more assertions to the unit tests for each command
            // execution.
            final String[] arguments = new String[] {
                _rrdTool,
                "create",
                _fileName,
                "-b",
                Long.toString(startTime),
                "-s",
                Integer.toString(period.toStandardSeconds().getSeconds()),
                "DS:metric:GAUGE:" + Integer.toString(period.toStandardSeconds().getSeconds() * 3) + ":U:U",
                "RRA:AVERAGE:0.5:1:1000" };
            executeProcess(arguments);
        }

        private void executeProcess(final String[] args) {
            try {
                final ProcessBuilder proecssBuilder = new ProcessBuilder(args);
                proecssBuilder.redirectErrorStream(true);
                final Process process = proecssBuilder.start();
                try (final BufferedReader processStandardOut = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charsets.UTF_8))) {
                    String line;
                    final StringBuilder processOutput = new StringBuilder();
                    while ((line = processStandardOut.readLine()) != null) {
                        processOutput.append(line).append("\n");
                    }
                    try {
                        process.waitFor();
                    } catch (final InterruptedException e) {
                        LOGGER.error()
                                .setMessage("Interrupted waiting for process to exit")
                                .addData("sink", getName())
                                .setThrowable(e)
                                .log();
                    }
                    if (process.exitValue() != 0) {
                        LOGGER.error()
                                .setMessage("Execution result in an error")
                                .addData("sink", getName())
                                .addData("command",  Joiner.on(" ").join(args))
                                .addData("exitValue", process.exitValue())
                                .addData("output", processOutput.toString())
                                .log();
                    }
                }
            } catch (final IOException e) {
                LOGGER.error()
                        .setMessage("Error executing rrd")
                        .addData("sink", getName())
                        .setThrowable(e)
                        .log();
            }
        }

        private final String _fileName;
    }
}
