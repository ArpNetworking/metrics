/**
 * Copyright 2014 Groupon.com
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

package com.arpnetworking.test.junitbenchmarks;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A class to filter hprof results to be somewhat useful.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class HProfFilter {
    /**
     * Entry point.
     *
     * @param args Command line args.
     */
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.out.println("First argument must be the report to filter");
        }
        final Path report = Paths.get(args[0]);
        final HProfFilter filter = new HProfFilter(report);
        try {
            filter.run();
        } catch (final IOException e) {
            System.err.println("IO Exception: " + e);
        }
    }

    /* package private for testing */ HProfFilter(final Path report) {
        _report = report;
    }

    /* package private for testing */ void run() throws IOException {
        final String reportNoExt = com.google.common.io.Files.getNameWithoutExtension(_report.toString());
        final String reportExt = com.google.common.io.Files.getFileExtension(_report.toString());

        final String filteredName = reportNoExt + ".filtered." + reportExt;
        final Path filtered = _report.toAbsolutePath().normalize().getParent().resolve(filteredName);
        System.out.printf("Filtering file %s%n", _report);
        System.out.printf("Output file is %s%n", filtered);

        try (
                final BufferedReader reader = Files.newBufferedReader(_report, Charsets.UTF_8);
                final BufferedWriter writer = Files.newBufferedWriter(filtered, Charsets.UTF_8)) {
            readHeader(reader, writer);

            // The next thing in the file will be the threads declarations
            readThreadDefs(reader, writer);

            // The next thing in the file is the trace definitions
            final Map<Integer, Trace> traces = Maps.newHashMap();
            readTraces(reader, writer, traces);

            readSamples(reader, writer, traces);
        }
    }

    private void readSamples(
            final BufferedReader reader,
            final BufferedWriter writer,
            final Map<Integer, Trace> traces)
            throws IOException {
        String line;
        final Samples samples = new Samples();
        int totalSamples = 0;
        String date = "";
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("CPU SAMPLES BEGIN")) {
                // Trim the "CPU SAMPLES BEGIN (total = " off the front
                String totals = line.substring(line.indexOf('(') + 9);
                totals = totals.substring(0, totals.indexOf(')'));
                totalSamples = Integer.parseInt(totals);
                final int dateStartIndex = line.indexOf(')') + 2;
                date = line.substring(dateStartIndex);
            } else if (line.trim().startsWith("rank")) {
                // Do nothing
                continue;
            } else if (line.startsWith("CPU SAMPLES END")) {
                // Do nothing
                continue;
            } else {
                samples.addLine(line);
            }
        }

        samples.emit(writer, traces, date);
    }

    private void readTraces(
            final BufferedReader reader,
            final BufferedWriter writer,
            final Map<Integer, Trace> traces) throws IOException {

        String line;
        Trace trace = null;
        while ((line = reader.readLine()) != null) {
            // The first line we read from here will always start with TRACE
            if (line.startsWith("TRACE")) {
                if (trace != null) {
                    trace.emit(writer);
                }

                trace = createTrace(line);
                traces.put(trace.getId(), trace);
                reader.mark(READ_AHEAD_LIMIT);
            } else if (line.startsWith("CPU SAMPLES BEGIN")) {
                // We hit the CPU Samples section, reset the read
                reader.reset();
                trace.emit(writer);
                break;
            } else {
                trace.addStackLine(line);
                reader.mark(READ_AHEAD_LIMIT);
            }
        }
    }

    private void readThreadDefs(final BufferedReader reader, final BufferedWriter writer) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("THREAD")) {
                reader.mark(READ_AHEAD_LIMIT);
                writer.write(line);
                writer.newLine();
            } else {
                // Move us back to before we read the non-thread line
                reader.reset();
                break;
            }
        }
    }

    private void readHeader(final BufferedReader reader, final BufferedWriter writer) throws IOException {
        String line;
        int linesRead = 0;
        boolean passedHeader = false;
        // Skip until we see a ------ line
        while ((line = reader.readLine()) != null) {
            linesRead++;
            writer.write(line);
            writer.newLine();

            if (line.startsWith("-----")) {
                passedHeader = true;
            } else if (passedHeader && line.trim().length() == 0) {
                // Empty line after the header
                reader.mark(READ_AHEAD_LIMIT);
            } else if (passedHeader && line.startsWith("THREAD")) {
                break;
            }
        }
    }

    private static Trace createTrace(final String traceLine) {
        final String[] split = traceLine.split(" ");
        if (split.length == 1) {
            throw new IllegalArgumentException(String.format("Trace line does not appear to be valid: %s", traceLine));
        }

        final int traceNumber = Integer.parseInt(split[1].replace(":", ""));
        return new Trace(traceNumber);
    }

    private final Path _report;

    private static final int READ_AHEAD_LIMIT = 256 * 1024;

    private static class Trace {
        public Trace(final int id) {
            _id = id;
        }

        public void addStackLine(final String line) {
            _stackLines.add(line);
        }

        public int getId() {
            return _id;
        }

        public boolean shouldFilter() {
            final String topLine = _stackLines.get(0).trim();
            if (topLine.startsWith("sun.nio")) {
                return true;
            } else if (topLine.startsWith("sun.misc.Unsafe")) {
                return true;
            }
            return false;
        }

        public void emit(final BufferedWriter writer) throws IOException {
            if (!shouldFilter()) {
                writer.write(String.format("TRACE %d:", _id));
                writer.newLine();
                for (final String stackLine : _stackLines) {
                    writer.write(stackLine);
                    writer.newLine();
                }
            }
        }

        private final int _id;
        private final List<String> _stackLines = Lists.newArrayList();
    }

    private static class Samples {

        public void addLine(final String line) {
            final List<String> strings = _splitter.splitToList(line);
            if (strings.size() != 6) {
                throw new IllegalArgumentException(String.format("Samples entry does not appear to be valid: %s", line));
            }
            final int count = Integer.parseInt(strings.get(3));
            final int trace = Integer.parseInt(strings.get(4));
            final String method = strings.get(5);
            _samples.add(new Sample(count, trace, method));
        }

        public void emit(final BufferedWriter writer,
                         final Map<Integer, Trace> traces,
                         final String date) throws IOException {

            if (_samples.size() == 0) {
                return;
            }

            final List<Sample> filteredSamples = FluentIterable.from(_samples)
                    .filter(new Predicate<Sample>() {
                        @Override
                        public boolean apply(final Sample input) {
                            final int traceId = input.getTrace();
                            final Trace trace = traces.get(traceId);
                            if (trace == null || trace.shouldFilter()) {
                                return false;
                            }
                            return true;
                        }
                    })
                    .toSortedList(new Comparator<Sample>() {
                        @Override
                        public int compare(final Sample o1, final Sample o2) {
                            return Integer.compare(o2.getCount(), o1.getCount());
                        }
                    });

            long filteredSamplesCount = 0;
            for (final Sample filteredSample : filteredSamples) {
                filteredSamplesCount += filteredSample._count;
            }

            writer.write(String.format("CPU SAMPLES BEGIN (total = %d) %s", filteredSamplesCount, date));
            writer.newLine();

            writer.write("rank   self  accum   count trace method");
            writer.newLine();

            final String sampleFormat = "%4d %5.2f%% %5.2f%% %7d %5d %s";

            int rank = 1;
            double accum = 0;
            for (final Sample sample : filteredSamples) {
                final double perc = (double) sample.getCount() / filteredSamplesCount * 100;
                accum += perc;
                writer.write(String.format(sampleFormat, rank, perc, accum, sample.getCount(), sample.getTrace(), sample.getMethod()));
                writer.newLine();
                ++rank;
            }

            writer.write("CPU SAMPLES END");
            writer.newLine();
        }

        private final Splitter _splitter = Splitter.on(" ").omitEmptyStrings().trimResults().limit(6);
        private final List<Sample> _samples = Lists.newArrayList();

        private static class Sample {
            public Sample(final int count, final int trace, final String method) {
                _count = count;
                _trace = trace;
                _method = method;
            }

            public int getCount() {
                return _count;
            }

            public int getTrace() {
                return _trace;
            }

            public String getMethod() {
                return _method;
            }

            private final int _count;
            private final int _trace;
            private final String _method;
        }
    }
}
