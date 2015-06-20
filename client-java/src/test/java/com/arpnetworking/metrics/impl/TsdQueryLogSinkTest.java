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
package com.arpnetworking.metrics.impl;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import com.arpnetworking.logback.StenoEncoder;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Unit;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Tests for <code>TsdMetrics</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdQueryLogSinkTest {

    @Test
    public void testBuilderWithDefaults() {
        final String expectedPath = "./target/TsdQueryLogSinkTest/testBuilderWithDefaults/";
        final TsdQueryLogSink metricsFactory = (TsdQueryLogSink) new TsdQueryLogSink.Builder()
                .setPath(expectedPath)
                .build();

        final AsyncAppender asyncAppender = (AsyncAppender)
                metricsFactory.getQueryLogger().getAppender("query-log-async");
        final RollingFileAppender<ILoggingEvent> rollingAppender = (RollingFileAppender<ILoggingEvent>)
                asyncAppender.getAppender("query-log");
        @SuppressWarnings("unchecked")
        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = (TimeBasedRollingPolicy<ILoggingEvent>)
                rollingAppender.getRollingPolicy();
        final StenoEncoder encoder = (StenoEncoder) rollingAppender.getEncoder();

        Assert.assertTrue(encoder.isImmediateFlush());
        Assert.assertEquals(24, rollingPolicy.getMaxHistory());
        Assert.assertEquals(expectedPath + "query.log", rollingAppender.getFile());
        Assert.assertEquals(expectedPath + "query.%d{yyyy-MM-dd-HH}.log.gz", rollingPolicy.getFileNamePattern());
    }

    @Test
    public void testCustomBuilder() {
        final String expectedPath = "./target/TsdQueryLogSinkTest/testBuilderWithoutImmediateFlush/";
        final TsdQueryLogSink metricsFactory = (TsdQueryLogSink) new TsdQueryLogSink.Builder()
                .setPath(expectedPath)
                .setImmediateFlush(false)
                .setMaxHistory(48)
                .setName("foo")
                .setExtension(".bar")
                .build();

        final AsyncAppender asyncAppender = (AsyncAppender)
                metricsFactory.getQueryLogger().getAppender("query-log-async");
        final RollingFileAppender<ILoggingEvent> rollingAppender = (RollingFileAppender<ILoggingEvent>)
                asyncAppender.getAppender("query-log");
        @SuppressWarnings("unchecked")
        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = (TimeBasedRollingPolicy<ILoggingEvent>)
                rollingAppender.getRollingPolicy();
        final StenoEncoder encoder = (StenoEncoder) rollingAppender.getEncoder();

        Assert.assertFalse(encoder.isImmediateFlush());
        Assert.assertEquals(48, rollingPolicy.getMaxHistory());
        Assert.assertEquals(expectedPath + "foo.bar", rollingAppender.getFile());
        Assert.assertEquals(expectedPath + "foo.%d{yyyy-MM-dd-HH}.bar.gz", rollingPolicy.getFileNamePattern());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullPath() {
        new TsdQueryLogSink.Builder()
                .setPath(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullExtension() {
        new TsdQueryLogSink.Builder()
                .setExtension(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullImmediateFlush() {
        new TsdQueryLogSink.Builder()
                .setImmediateFlush(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullName() {
        new TsdQueryLogSink.Builder()
                .setName(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderEmptyName() {
        new TsdQueryLogSink.Builder()
                .setName("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullMaxHistory() {
        new TsdQueryLogSink.Builder()
                .setMaxHistory(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNegativeMaxHistory() {
        new TsdQueryLogSink.Builder()
                .setMaxHistory(-1)
                .build();
    }

    @Test
    public void testBuilderPathWithoutTrailingSeparator() {
        final String expectedPath = "./target/TsdQueryLogSinkTest/testBuilderPathWithoutTrailingSeparator";
        final TsdQueryLogSink metricsFactory = (TsdQueryLogSink) new TsdQueryLogSink.Builder()
                .setPath(expectedPath)
                .build();

        final AsyncAppender asyncAppender = (AsyncAppender)
                metricsFactory.getQueryLogger().getAppender("query-log-async");
        final RollingFileAppender<ILoggingEvent> rollingAppender = (RollingFileAppender<ILoggingEvent>)
                asyncAppender.getAppender("query-log");
        @SuppressWarnings("unchecked")
        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = (TimeBasedRollingPolicy<ILoggingEvent>)
                rollingAppender.getRollingPolicy();

        Assert.assertEquals(expectedPath + "/query.log", rollingAppender.getFile());
        Assert.assertEquals(expectedPath + "/query.%d{yyyy-MM-dd-HH}.log.gz", rollingPolicy.getFileNamePattern());
    }

    @Test
    public void testObjectMapperIOException() throws IOException {
        final org.slf4j.Logger logger = createSlf4jLoggerMock();
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        final Sink sink = new TsdQueryLogSink(
                new TsdQueryLogSink.Builder()
                        .setPath("./target/TsdQueryLogSinkTest")
                        .setName("testObjectMapperIOException-Query"),
                objectMapper,
                logger);

        Mockito.doThrow(new JsonMappingException("JsonMappingException")).when(objectMapper).writeValueAsString(Mockito.any());
        recordEmpty(sink);
        Mockito.verify(logger).warn(
                Mockito.argThat(Matchers.any(String.class)),
                Mockito.argThat(Matchers.any(Throwable.class)));
    }

    @Test
    public void testShutdownHookThread() throws InterruptedException {
        final LoggerContext context = Mockito.mock(LoggerContext.class);
        final Thread shutdownThread = new TsdQueryLogSink.ShutdownHookThread(context);
        shutdownThread.start();
        shutdownThread.join();
        Mockito.verify(context).stop();
    }

    @Test
    public void testEmptySerialization() throws IOException, InterruptedException {
        final File actualFile = new File("./target/TsdQueryLogSinkTest/testEmptySerialization-Query.log");
        Files.deleteIfExists(actualFile.toPath());
        final Sink sink = new TsdQueryLogSink.Builder()
                .setPath("./target/TsdQueryLogSinkTest")
                .setName("testEmptySerialization-Query")
                .setImmediateFlush(Boolean.TRUE)
                .build();

        sink.record(
                ANNOTATIONS,
                TEST_EMPTY_SERIALIZATION_TIMERS,
                TEST_EMPTY_SERIALIZATION_COUNTERS,
                TEST_EMPTY_SERIALIZATION_GAUGES);

        // TODO(vkoskela): Add protected option to disable async [MAI-181].
        Thread.sleep(100);

        final String actualOriginalJson = fileToString(actualFile);
        assertMatchesJsonSchema(actualOriginalJson);
        final String actualComparableJson = actualOriginalJson
                .replaceAll("\"time\":\"[^\"]*\"", "\"time\":\"<TIME>\"")
                .replaceAll("\"host\":\"[^\"]*\"", "\"host\":\"<HOST>\"")
                .replaceAll("\"processId\":\"[^\"]*\"", "\"processId\":\"<PROCESSID>\"")
                .replaceAll("\"threadId\":\"[^\"]*\"", "\"threadId\":\"<THREADID>\"")
                .replaceAll("\"id\":\"[^\"]*\"", "\"id\":\"<ID>\"");
        final JsonNode actual = OBJECT_MAPPER.readTree(actualComparableJson);
        final JsonNode expected = OBJECT_MAPPER.readTree(EXPECTED_EMPTY_METRICS_JSON);

        Assert.assertEquals(
                "expectedJson=" + OBJECT_MAPPER.writeValueAsString(expected)
                        + " vs actualJson=" + OBJECT_MAPPER.writeValueAsString(actual),
                expected,
                actual);
    }

    @Test
    public void testSerialization() throws IOException, InterruptedException {
        final File actualFile = new File("./target/TsdQueryLogSinkTest/testSerialization-Query.log");
        Files.deleteIfExists(actualFile.toPath());
        final Sink sink = new TsdQueryLogSink.Builder()
                .setPath("./target/TsdQueryLogSinkTest")
                .setName("testSerialization-Query")
                .setImmediateFlush(Boolean.TRUE)
                .build();

        final Map<String, String> annotations = new LinkedHashMap<>(ANNOTATIONS);
        annotations.put("foo", "bar");
        sink.record(
                annotations,
                TEST_SERIALIZATION_TIMERS,
                TEST_SERIALIZATION_COUNTERS,
                TEST_SERIALIZATION_GAUGES);

        // TODO(vkoskela): Add protected option to disable async [MAI-181].
        Thread.sleep(100);

        final String actualOriginalJson = fileToString(actualFile);
        assertMatchesJsonSchema(actualOriginalJson);
        final String actualComparableJson = actualOriginalJson
                .replaceAll("\"time\":\"[^\"]*\"", "\"time\":\"<TIME>\"")
                .replaceAll("\"host\":\"[^\"]*\"", "\"host\":\"<HOST>\"")
                .replaceAll("\"processId\":\"[^\"]*\"", "\"processId\":\"<PROCESSID>\"")
                .replaceAll("\"threadId\":\"[^\"]*\"", "\"threadId\":\"<THREADID>\"")
                .replaceAll("\"id\":\"[^\"]*\"", "\"id\":\"<ID>\"");
        final JsonNode actual = OBJECT_MAPPER.readTree(actualComparableJson);
        final JsonNode expected = OBJECT_MAPPER.readTree(EXPECTED_METRICS_JSON);

        Assert.assertEquals(
                "expectedJson=" + OBJECT_MAPPER.writeValueAsString(expected)
                        + " vs actualJson=" + OBJECT_MAPPER.writeValueAsString(actual),
                expected,
                actual);
    }

    private static Map<String, List<Quantity>> createQuantityMap(final Object... arguments) {
        // CHECKSTYLE.OFF: IllegalInstantiation - No Guava
        final Map<String, List<Quantity>> map = new HashMap<>();
        // CHECKSTYLE.ON: IllegalInstantiation
        List<Quantity> samples = null;
        for (final Object argument : arguments) {
            if (argument instanceof String) {
                samples = new ArrayList<>();
                map.put((String) argument, samples);
            } else if (argument instanceof Quantity) {
                assert samples != null : "first argument must be metric name";
                samples.add((Quantity) argument);
            } else {
                assert false : "unsupported argument type: " + argument.getClass();
            }
        }
        return map;
    }

    private void recordEmpty(final Sink sink) {
        sink.record(
                Collections.<String, String>emptyMap(),
                Collections.<String, List<Quantity>>emptyMap(),
                Collections.<String, List<Quantity>>emptyMap(),
                Collections.<String, List<Quantity>>emptyMap());
    }

    private org.slf4j.Logger createSlf4jLoggerMock() {
        return Mockito.mock(org.slf4j.Logger.class);
    }

    private void assertMatchesJsonSchema(final String json) {
        try {
            final JsonNode jsonNode = JsonLoader.fromString(json);
            final ProcessingReport report = VALIDATOR.validate(STENO_SCHEMA, jsonNode);
            Assert.assertTrue(report.toString(), report.isSuccess());
        } catch (final IOException | ProcessingException e) {
            Assert.fail("Failed with exception: " + e);
        }
    }

    private String fileToString(final File file) throws InterruptedException {
        // TODO(vkoskela): Need to work around async flushes to disk [MAI-458]
        Thread.sleep(500);
        try {
            return new Scanner(file, "UTF-8").useDelimiter("\\Z").next();
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final JsonValidator VALIDATOR = JsonSchemaFactory.byDefault().getValidator();
    private static final JsonNode STENO_SCHEMA;

    private static final Map<String, String> ANNOTATIONS = new LinkedHashMap<>();
    private static final Map<String, List<Quantity>> TEST_EMPTY_SERIALIZATION_TIMERS = createQuantityMap();
    private static final Map<String, List<Quantity>> TEST_EMPTY_SERIALIZATION_COUNTERS = createQuantityMap();
    private static final Map<String, List<Quantity>> TEST_EMPTY_SERIALIZATION_GAUGES = createQuantityMap();

    private static final String EXPECTED_EMPTY_METRICS_JSON = "{"
            + "  \"time\":\"<TIME>\","
            + "  \"name\":\"aint.metrics\","
            + "  \"level\":\"info\","
            + "  \"data\":{"
            + "    \"version\":\"2e\","
            + "    \"annotations\":{"
            + "      \"initTimestamp\":\"1997-07-16T19:20:30Z\","
            + "      \"finalTimestamp\":\"1997-07-16T19:20:31Z\""
            + "    }"
            + "  },"
            + "  \"context\":{"
            + "    \"host\":\"<HOST>\","
            + "    \"processId\":\"<PROCESSID>\","
            + "    \"threadId\":\"<THREADID>\""
            + "  },"
            + "  \"id\":\"<ID>\","
            + "  \"version\":\"0\""
            + "}";

    private static final Map<String, List<Quantity>> TEST_SERIALIZATION_TIMERS = createQuantityMap(
            "timerA",
            "timerB",
            TsdQuantity.newInstance(Long.valueOf(1L), null),
            "timerC",
            TsdQuantity.newInstance(Long.valueOf(2L), Unit.MILLISECOND),
            "timerD",
            TsdQuantity.newInstance(Long.valueOf(3L), Unit.SECOND),
            TsdQuantity.newInstance(Long.valueOf(4L), Unit.SECOND),
            "timerE",
            TsdQuantity.newInstance(Long.valueOf(5L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(6L), Unit.SECOND),
            "timerF",
            TsdQuantity.newInstance(Long.valueOf(7L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(8L), null),
            "timerG",
            TsdQuantity.newInstance(Long.valueOf(9L), null),
            TsdQuantity.newInstance(Long.valueOf(10L), null),
            "timerH",
            TsdQuantity.newInstance(Long.valueOf(11L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(12L), Unit.BYTE),
            "timerI",
            TsdQuantity.newInstance(Double.valueOf(1.12), null),
            "timerJ",
            TsdQuantity.newInstance(Double.valueOf(2.12), Unit.MILLISECOND),
            "timerK",
            TsdQuantity.newInstance(Double.valueOf(3.12), Unit.SECOND),
            TsdQuantity.newInstance(Double.valueOf(4.12), Unit.SECOND),
            "timerL",
            TsdQuantity.newInstance(Double.valueOf(5.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(6.12), Unit.SECOND),
            "timerM",
            TsdQuantity.newInstance(Double.valueOf(7.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(8.12), null),
            "timerN",
            TsdQuantity.newInstance(Double.valueOf(9.12), null),
            TsdQuantity.newInstance(Double.valueOf(10.12), null),
            "timerO",
            TsdQuantity.newInstance(Double.valueOf(11.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(12.12), Unit.BYTE));

    private static final Map<String, List<Quantity>> TEST_SERIALIZATION_COUNTERS = createQuantityMap(
            "counterA",
            "counterB",
            TsdQuantity.newInstance(Long.valueOf(11L), null),
            "counterC",
            TsdQuantity.newInstance(Long.valueOf(12L), Unit.MILLISECOND),
            "counterD",
            TsdQuantity.newInstance(Long.valueOf(13L), Unit.SECOND),
            TsdQuantity.newInstance(Long.valueOf(14L), Unit.SECOND),
            "counterE",
            TsdQuantity.newInstance(Long.valueOf(15L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(16L), Unit.SECOND),
            "counterF",
            TsdQuantity.newInstance(Long.valueOf(17L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(18L), null),
            "counterG",
            TsdQuantity.newInstance(Long.valueOf(19L), null),
            TsdQuantity.newInstance(Long.valueOf(110L), null),
            "counterH",
            TsdQuantity.newInstance(Long.valueOf(111L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(112L), Unit.BYTE),
            "counterI",
            TsdQuantity.newInstance(Double.valueOf(11.12), null),
            "counterJ",
            TsdQuantity.newInstance(Double.valueOf(12.12), Unit.MILLISECOND),
            "counterK",
            TsdQuantity.newInstance(Double.valueOf(13.12), Unit.SECOND),
            TsdQuantity.newInstance(Double.valueOf(14.12), Unit.SECOND),
            "counterL",
            TsdQuantity.newInstance(Double.valueOf(15.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(16.12), Unit.SECOND),
            "counterM",
            TsdQuantity.newInstance(Double.valueOf(17.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(18.12), null),
            "counterN",
            TsdQuantity.newInstance(Double.valueOf(19.12), null),
            TsdQuantity.newInstance(Double.valueOf(110.12), null),
            "counterO",
            TsdQuantity.newInstance(Double.valueOf(111.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(112.12), Unit.BYTE));

    private static final Map<String, List<Quantity>> TEST_SERIALIZATION_GAUGES = createQuantityMap(
            "gaugeA",
            "gaugeB",
            TsdQuantity.newInstance(Long.valueOf(21L), null),
            "gaugeC",
            TsdQuantity.newInstance(Long.valueOf(22L), Unit.MILLISECOND),
            "gaugeD",
            TsdQuantity.newInstance(Long.valueOf(23L), Unit.SECOND),
            TsdQuantity.newInstance(Long.valueOf(24L), Unit.SECOND),
            "gaugeE",
            TsdQuantity.newInstance(Long.valueOf(25L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(26L), Unit.SECOND),
            "gaugeF",
            TsdQuantity.newInstance(Long.valueOf(27L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(28L), null),
            "gaugeG",
            TsdQuantity.newInstance(Long.valueOf(29L), null),
            TsdQuantity.newInstance(Long.valueOf(210L), null),
            "gaugeH",
            TsdQuantity.newInstance(Long.valueOf(211L), Unit.DAY),
            TsdQuantity.newInstance(Long.valueOf(212L), Unit.BYTE),
            "gaugeI",
            TsdQuantity.newInstance(Double.valueOf(21.12), null),
            "gaugeJ",
            TsdQuantity.newInstance(Double.valueOf(22.12), Unit.MILLISECOND),
            "gaugeK",
            TsdQuantity.newInstance(Double.valueOf(23.12), Unit.SECOND),
            TsdQuantity.newInstance(Double.valueOf(24.12), Unit.SECOND),
            "gaugeL",
            TsdQuantity.newInstance(Double.valueOf(25.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(26.12), Unit.SECOND),
            "gaugeM",
            TsdQuantity.newInstance(Double.valueOf(27.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(28.12), null),
            "gaugeN",
            TsdQuantity.newInstance(Double.valueOf(29.12), null),
            TsdQuantity.newInstance(Double.valueOf(210.12), null),
            "gaugeO",
            TsdQuantity.newInstance(Double.valueOf(211.12), Unit.DAY),
            TsdQuantity.newInstance(Double.valueOf(212.12), Unit.BYTE));

    private static final String EXPECTED_METRICS_JSON = "{"
            + "  \"time\":\"<TIME>\","
            + "  \"name\":\"aint.metrics\","
            + "  \"level\":\"info\","
            + "  \"data\":{"
            + "    \"version\":\"2e\","
            + "    \"annotations\":{"
            + "      \"initTimestamp\":\"1997-07-16T19:20:30Z\","
            + "      \"finalTimestamp\":\"1997-07-16T19:20:31Z\","
            + "      \"foo\":\"bar\""
            + "    },"
            + "    \"counters\":{"
            + "      \"counterA\":{\"values\":[]},"
            + "      \"counterB\":{\"values\":[{\"value\":11}]},"
            + "      \"counterC\":{\"values\":[{\"value\":12,\"unit\":\"millisecond\"}]},"
            + "      \"counterD\":{\"values\":[{\"value\":13,\"unit\":\"second\"},{\"value\":14,\"unit\":\"second\"}]},"
            + "      \"counterE\":{\"values\":[{\"value\":15,\"unit\":\"day\"},{\"value\":16,\"unit\":\"second\"}]},"
            + "      \"counterF\":{\"values\":[{\"value\":17,\"unit\":\"day\"},{\"value\":18}]},"
            + "      \"counterG\":{\"values\":[{\"value\":19},{\"value\":110}]},"
            + "      \"counterH\":{\"values\":[{\"value\":111,\"unit\":\"day\"},{\"value\":112,\"unit\":\"byte\"}]},"
            + "      \"counterI\":{\"values\":[{\"value\":11.12}]},"
            + "      \"counterJ\":{\"values\":[{\"value\":12.12,\"unit\":\"millisecond\"}]},"
            + "      \"counterK\":{\"values\":[{\"value\":13.12,\"unit\":\"second\"},{\"value\":14.12,\"unit\":\"second\"}]},"
            + "      \"counterL\":{\"values\":[{\"value\":15.12,\"unit\":\"day\"},{\"value\":16.12,\"unit\":\"second\"}]},"
            + "      \"counterM\":{\"values\":[{\"value\":17.12,\"unit\":\"day\"},{\"value\":18.12}]},"
            + "      \"counterN\":{\"values\":[{\"value\":19.12},{\"value\":110.12}]},"
            + "      \"counterO\":{\"values\":[{\"value\":111.12,\"unit\":\"day\"},{\"value\":112.12,\"unit\":\"byte\"}]}"
            + "    },"
            + "    \"gauges\":{"
            + "      \"gaugeA\":{\"values\":[]},"
            + "      \"gaugeB\":{\"values\":[{\"value\":21}]},"
            + "      \"gaugeC\":{\"values\":[{\"value\":22,\"unit\":\"millisecond\"}]},"
            + "      \"gaugeD\":{\"values\":[{\"value\":23,\"unit\":\"second\"},{\"value\":24,\"unit\":\"second\"}]},"
            + "      \"gaugeE\":{\"values\":[{\"value\":25,\"unit\":\"day\"},{\"value\":26,\"unit\":\"second\"}]},"
            + "      \"gaugeF\":{\"values\":[{\"value\":27,\"unit\":\"day\"},{\"value\":28}]},"
            + "      \"gaugeG\":{\"values\":[{\"value\":29},{\"value\":210}]},"
            + "      \"gaugeH\":{\"values\":[{\"value\":211,\"unit\":\"day\"},{\"value\":212,\"unit\":\"byte\"}]},"
            + "      \"gaugeI\":{\"values\":[{\"value\":21.12}]},"
            + "      \"gaugeJ\":{\"values\":[{\"value\":22.12,\"unit\":\"millisecond\"}]},"
            + "      \"gaugeK\":{\"values\":[{\"value\":23.12,\"unit\":\"second\"},{\"value\":24.12,\"unit\":\"second\"}]},"
            + "      \"gaugeL\":{\"values\":[{\"value\":25.12,\"unit\":\"day\"},{\"value\":26.12,\"unit\":\"second\"}]},"
            + "      \"gaugeM\":{\"values\":[{\"value\":27.12,\"unit\":\"day\"},{\"value\":28.12}]},"
            + "      \"gaugeN\":{\"values\":[{\"value\":29.12},{\"value\":210.12}]},"
            + "      \"gaugeO\":{\"values\":[{\"value\":211.12,\"unit\":\"day\"},{\"value\":212.12,\"unit\":\"byte\"}]}"
            + "    },"
            + "    \"timers\":{"
            + "     \"timerA\":{\"values\":[]},"
            + "      \"timerB\":{\"values\":[{\"value\":1}]},"
            + "      \"timerC\":{\"values\":[{\"value\":2,\"unit\":\"millisecond\"}]},"
            + "      \"timerD\":{\"values\":[{\"value\":3,\"unit\":\"second\"},{\"value\":4,\"unit\":\"second\"}]},"
            + "      \"timerE\":{\"values\":[{\"value\":5,\"unit\":\"day\"},{\"value\":6,\"unit\":\"second\"}]},"
            + "      \"timerF\":{\"values\":[{\"value\":7,\"unit\":\"day\"},{\"value\":8}]},"
            + "      \"timerG\":{\"values\":[{\"value\":9},{\"value\":10}]},"
            + "      \"timerH\":{\"values\":[{\"value\":11,\"unit\":\"day\"},{\"value\":12,\"unit\":\"byte\"}]},"
            + "      \"timerI\":{\"values\":[{\"value\":1.12}]},"
            + "      \"timerJ\":{\"values\":[{\"value\":2.12,\"unit\":\"millisecond\"}]},"
            + "      \"timerK\":{\"values\":[{\"value\":3.12,\"unit\":\"second\"},{\"value\":4.12,\"unit\":\"second\"}]},"
            + "      \"timerL\":{\"values\":[{\"value\":5.12,\"unit\":\"day\"},{\"value\":6.12,\"unit\":\"second\"}]},"
            + "      \"timerM\":{\"values\":[{\"value\":7.12,\"unit\":\"day\"},{\"value\":8.12}]},"
            + "      \"timerN\":{\"values\":[{\"value\":9.12},{\"value\":10.12}]},"
            + "      \"timerO\":{\"values\":[{\"value\":11.12,\"unit\":\"day\"},{\"value\":12.12,\"unit\":\"byte\"}]}"
            + "    }"
            + "  },"
            + "  \"context\":{"
            + "    \"host\":\"<HOST>\","
            + "    \"processId\":\"<PROCESSID>\","
            + "    \"threadId\":\"<THREADID>\""
            + "  },"
            + "  \"id\":\"<ID>\","
            + "  \"version\":\"0\""
            + "}";

    static {
        JsonNode jsonNode = null;
        try {
            // Normally this is being executed from the project directory (e.g. root/client-java)
            jsonNode = JsonLoader.fromPath("../doc/query-log-schema-2e.json");
        } catch (final IOException e1) {
            try {
                // Under some IDE setups this may be executed from the workspace root (e.g. root)
                jsonNode = JsonLoader.fromPath("doc/query-log-schema-2e.json");
            } catch (final IOException e2) {
                throw new RuntimeException(e2);
            }
        }
        STENO_SCHEMA = jsonNode;

        ANNOTATIONS.put("initTimestamp", "1997-07-16T19:20:30Z");
        ANNOTATIONS.put("finalTimestamp", "1997-07-16T19:20:31Z");
    }
}
