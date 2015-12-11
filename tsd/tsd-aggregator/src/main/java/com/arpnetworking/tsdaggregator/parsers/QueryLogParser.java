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
package com.arpnetworking.tsdaggregator.parsers;

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.EnumerationDeserializer;
import com.arpnetworking.jackson.EnumerationDeserializerStrategyUsingToUpperCase;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.steno.RateLimitLogBuilder;
import com.arpnetworking.tsdaggregator.model.DefaultMetric;
import com.arpnetworking.tsdaggregator.model.DefaultRecord;
import com.arpnetworking.tsdaggregator.model.Metric;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdaggregator.model.querylog.Version2c;
import com.arpnetworking.tsdaggregator.model.querylog.Version2d;
import com.arpnetworking.tsdaggregator.model.querylog.Version2e;
import com.arpnetworking.tsdcore.model.MetricType;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.parsers.exceptions.ParsingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.exception.ConstraintsViolatedException;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of <code>RecordParser</code> for the TSD query log format. The
 * query log format represents each <code>Record</code> instance with one json
 * object per line.
 *
 * Specification for version 2e:
 * {@code
 *{
 *    "$schema": "http://json-schema.org/draft-04/schema#",
 *    "definitions": {
 *        "sampleObject" : {
 *            "type": "object",
 *            "properties" : {
 *                "unit": {
 *                    "type": "string",
 *                    "enum": ["nanosecond","microsecond", "millisecond", "second", "minute", "hour", "day", "week",
 *                    "bit", "byte", "kilobit", "kilobyte", "megabit", "megabyte", "gigabit", "gigabyte", "terabyte", "petabyte"]
 *                },
 *                "value": {
 *                    "type": "number"
 *                }
 *            },
 *            "required": [ "value" ]
 *        },
 *        "dataElement": {
 *            "type": "object",
 *            "properties": {
 *                "values": {
 *                    "type": "array",
 *                    "items": {
 *                        "$ref": "#/definitions/sampleObject"
 *                    }
 *                }
 *            },
 *            "required": [ "values" ]
 *        },
 *        "metricsList": {
 *            "type": "object",
 *            "additionalProperties": {
 *                "$ref": "#/definitions/dataElement"
 *            }
 *        },
 *        "data": {
 *            "type": "object",
 *            "properties":{
 *                "annotations": {
 *                    "type":"object",
 *                    "properties": {
 *                        "finalTimestamp": {
 *                            "type":"string"
 *                        },
 *                        "initTimestamp": {
 *                            "type":"string"
 *                        }
 *                    },
 *                    "required": ["initTimestamp", "finalTimestamp"],
 *                    "additionalProperties": {
 *                        "type": "string"
 *                    }
 *                },
 *                "counters":  {
 *                    "$ref": "#/definitions/metricsList"
 *                },
 *                "gauges": {
 *                    "$ref": "#/definitions/metricsList"
 *                },
 *                "timers": {
 *                    "$ref": "#/definitions/metricsList"
 *                },
 *                "version": {
 *                    "type":"string",
 *                    "pattern": "^2e$"
 *                }
 *            },
 *            "required": ["annotations", "version"]
 *        }
 *    },
 *
 *    "title": "Query Log 2e",
 *    "description": "log file entry for ingestion by tsd aggregator",
 *    "type":"object",
 *
 *    "properties":{
 *        "time": {
 *            "type":"string",
 *            "format": "date-time"
 *        },
 *        "name":  {
 *            "type":"string",
 *            "pattern": "^aint-metrics$"
 *        },
 *        "level":  {
 *            "type":"string",
 *            "pattern": "^info$"
 *        },
 *        "data": {
 *            "$ref": "#/definitions/data"
 *        },
 *        "id":  {
 *            "type":"string"
 *        },
 *        "context": {
 *            "type":"object",
 *            "properties": {
 *            }
 *        }
 *    },
 *    "required": ["time", "name", "level", "data"]
 *}
 * }
 *
 * Specification for version 2d:
 * {@code
 *{
 *    "$schema": "http://json-schema.org/draft-04/schema#",
 *    "definitions": {
 *        "sampleObject" : {
 *            "type": "object",
 *            "properties" : {
 *                "unit": {
 *                    "type": "string",
 *                    "enum": ["nanosecond","microsecond", "millisecond", "second", "minute", "hour", "day", "week",
 *                    "bit", "byte", "kilobit", "kilobyte", "megabit", "megabyte", "gigabit", "gigabyte", "terabyte", "petabyte"]
 *                },
 *                "value": {
 *                    "type": "number",
 *                }
 *            }
 *        },
 *        "dataElement": {
 *            "type": "object",
 *            "properties": {
 *                "values": {
 *                    "type": "array",
 *                    "items": {
 *
 *                        "$ref": "#/definitions/sampleObject"
 *                    }
 *                }
 *            }
 *        },
 *        "metricsList": {
 *            "type": "object",
 *            "additionalProperties": {
 *                "$ref": "#/definitions/dataElement"
 *            }
 *        }
 *    },
 *
 *    "title": "Query Log 2d",
 *    "description": "log file entry for ingestion by tsd aggregator",
 *    "type":"object",
 *
 *    "properties":{
 *        "annotations": {
 *            "type":"object",
 *            "properties": {
 *                "finalTimestamp": {
 *                    "type":"string"
 *                },
 *                "initTimestamp": {
 *                    "type":"string"
 *                }
 *            },
 *            "required": ["initTimestamp", "finalTimestamp"]
 *        },
 *        "counters": {
 *            "$ref": "#/definitions/metricsList"
 *        },
 *        "gauges": {
 *            "$ref": "#/definitions/metricsList"
 *        },
 *        "timers": {
 *            "$ref": "#/definitions/metricsList"
 *        },
 *        "version": {
 *            "type":"string",
 *            "pattern": "^2d$"
 *        }
 *    },
 *    "required": ["annotations", "version"]
 *}
 * }
 *
 * Specification for version 2c:
 * {@code
 *{
 *    "title": "Query Log 2c",
 *    "description": "log file entry for ingestion by tsd aggregator",
 *    "type":"object",
 *    "$schema": "http://json-schema.org/draft-04/schema#",
 *    "properties":{
 *        "annotations": {
 *            "type":"object",
 *            "properties":{
 *            "finalTimestamp": {
 *                "type":"string"
 *            },
 *            "initTimestamp": {
 *                "type":"string"
 *            }
 *            },
 *            "required": ["initTimestamp", "finalTimestamp"]
 *        },
 *        "counters": {
 *            "type":"object",
 *            "properties":{ },
 *            "additionalProperties": {
 *                "type": "array",
 *                "items": {
 *                    "type": "number"
 *                }
 *            }
 *        },
 *        "gauges": {
 *            "type":"object",
 *            "properties":{ },
 *            "additionalProperties": {
 *                "type": "array",
 *                "items": {
 *                    "type": "number"
 *                }
 *            }
 *        },
 *        "timers": {
 *            "type":"object",
 *            "properties":{ },
 *            "additionalProperties": {
 *                "type": "array",
 *                "items": {
 *                    "type": "number"
 *                }
 *            }
 *        },
 *        "version": {
 *            "type":"string",
 *            "pattern": "^2c$"
 *        }
 *    },
 *    "required": ["annotations", "version"]
 *}
 * }
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class QueryLogParser implements Parser<Record> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Record parse(final byte[] data) throws ParsingException {
        // Attempt to parse the data as JSON to distinguish between the legacy
        // format and the current JSON format
        final JsonNode jsonNode;
        try {
            jsonNode = OBJECT_MAPPER.readTree(data);
        } catch (final IOException ex) {
            // CHECKSTYLE.OFF: IllegalInstantiation - Approved for byte[] to String
            throw new ParsingException(String.format(
                    "Unsupported non-json format; data=%s",
                    new String(data, Charsets.UTF_8)));
            // CHECKSTYLE.ON: IllegalInstantiation
        }

        // If it's JSON extract the version and parse accordingly
        final JsonNode dataNode = jsonNode.get(DATA_KEY);
        JsonNode versionNode = jsonNode.get(VERSION_KEY);
        if (dataNode != null) {
            final JsonNode dataVersionNode = dataNode.get(VERSION_KEY);
            if (dataVersionNode != null) {
                versionNode = dataVersionNode;
            }
        }
        if (versionNode == null) {
            throw new ParsingException(String.format("Unable to determine version; jsonNode=%s", jsonNode));
        }
        final String version = versionNode.textValue().toLowerCase(Locale.getDefault());
        switch (version) {
            case "2c":
                return parseV2cLogLine(jsonNode);
            case "2d":
                return parseV2dLogLine(jsonNode);
            case "2e":
                return parseV2eLogLine(jsonNode);
            default:
                throw new ParsingException(String.format("Unsupported version; version=%s", version));
        }
    }

    // NOTE: Package private for testing
    /* package private */com.arpnetworking.tsdaggregator.model.Record parseV2cLogLine(final JsonNode jsonNode) throws ParsingException {
        final Version2c model;
        try {
            model = OBJECT_MAPPER.treeToValue(jsonNode, Version2c.class);
        } catch (final IOException | IllegalArgumentException | ConstraintsViolatedException e) {
            throw new ParsingException("Failed to deserialize version 2c", e);
        }

        final Version2c.Annotations annotations = model.getAnnotations();
        final DateTime timestamp = getTimestampFor2c(annotations);

        final Map<String, Metric> variables = Maps.newHashMap();
        putVariablesVersion2c(model.getTimers(), MetricType.TIMER, variables);
        putVariablesVersion2c(model.getCounters(), MetricType.COUNTER, variables);
        putVariablesVersion2c(model.getGauges(), MetricType.GAUGE, variables);

        return new DefaultRecord.Builder()
                .setMetrics(variables)
                .setTime(timestamp)
                .setAnnotations(annotations.getOtherAnnotations())
                .build();
    }

    // NOTE: Package private for testing
    /* package private */com.arpnetworking.tsdaggregator.model.Record parseV2dLogLine(final JsonNode jsonNode) throws ParsingException {
        final Version2d model;
        try {
            model = OBJECT_MAPPER.treeToValue(jsonNode, Version2d.class);
        } catch (final IOException | IllegalArgumentException | ConstraintsViolatedException e) {
            throw new ParsingException("Failed to deserialize version 2d", e);
        }

        final Version2d.Annotations annotations = model.getAnnotations();
        final DateTime timestamp = annotations.getFinalTimestamp();

        final Map<String, Metric> variables = Maps.newHashMap();
        putVariablesVersion2d(model.getTimers(), MetricType.TIMER, variables);
        putVariablesVersion2d(model.getCounters(), MetricType.COUNTER, variables);
        putVariablesVersion2d(model.getGauges(), MetricType.GAUGE, variables);

        return new DefaultRecord.Builder()
                .setMetrics(variables)
                .setTime(timestamp)
                .setAnnotations(annotations.getOtherAnnotations())
                .build();
    }

    // NOTE: Package private for testing
    /* package private */com.arpnetworking.tsdaggregator.model.Record parseV2eLogLine(final JsonNode jsonNode) throws ParsingException {
        final Version2e model;
        try {
            model = OBJECT_MAPPER.treeToValue(jsonNode, Version2e.class);
        } catch (final IOException | IllegalArgumentException | ConstraintsViolatedException e) {
            throw new ParsingException("Failed to deserialize version 2e", e);
        }

        final Version2e.Data data = model.getData();
        final Version2e.Annotations annotations = data.getAnnotations();
        final DateTime timestamp = annotations.getFinalTimestamp();

        final Map<String, Metric> variables = Maps.newHashMap();
        putVariablesVersion2e(data.getTimers(), MetricType.TIMER, variables);
        putVariablesVersion2e(data.getCounters(), MetricType.COUNTER, variables);
        putVariablesVersion2e(data.getGauges(), MetricType.GAUGE, variables);

        return new DefaultRecord.Builder()
                .setMetrics(variables)
                .setTime(timestamp)
                .setAnnotations(annotations.getOtherAnnotations())
                .build();
    }

    private static void putVariablesVersion2c(
            final Map<String, List<String>> elements,
            final MetricType metricKind,
            final Map<String, Metric> variables) {

        for (final Map.Entry<String, List<String>> entry : elements.entrySet()) {
            final List<String> element = entry.getValue();
            final List<Quantity> quantities = Lists.newArrayList(Iterables.filter(
                    Lists.transform(element, VERSION_2C_SAMPLE_TO_QUANTITY),
                    Predicates.notNull()));
            variables.put(
                    entry.getKey(),
                    new DefaultMetric.Builder()
                            .setType(metricKind)
                            .setValues(quantities)
                            .build());
        }
    }

    private static void putVariablesVersion2d(
            final Map<String, Version2d.Element> elements,
            final MetricType metricKind,
            final Map<String, Metric> variables) {

        for (final Map.Entry<String, Version2d.Element> entry : elements.entrySet()) {
            final Version2d.Element element = entry.getValue();
            final List<Quantity> quantities = Lists.newArrayList(Iterables.filter(
                    Lists.transform(element.getValues(), VERSION_2D_SAMPLE_TO_QUANTITY),
                    Predicates.notNull()));
            variables.put(
                    entry.getKey(),
                    new DefaultMetric.Builder()
                            .setType(metricKind)
                            .setValues(quantities)
                            .build());
        }
    }

    private static void putVariablesVersion2e(
            final Map<String, Version2e.Element> elements,
            final MetricType metricKind,
            final Map<String, Metric> variables) {

        for (final Map.Entry<String, Version2e.Element> entry : elements.entrySet()) {
            final Version2e.Element element = entry.getValue();
            final List<Quantity> quantities = Lists.newArrayList(Iterables.filter(
                    Lists.transform(element.getValues(), VERSION_2E_SAMPLE_TO_QUANTITY),
                    Predicates.notNull()));
            variables.put(
                    entry.getKey(),
                    new DefaultMetric.Builder()
                            .setType(metricKind)
                            .setValues(quantities)
                            .build());
        }
    }

    private DateTime getTimestampFor2c(final Version2c.Annotations annotations) throws ParsingException {
        if (annotations.getFinalTimestamp().isPresent()) {
            try {
                return timestampToDateTime(Double.parseDouble(annotations.getFinalTimestamp().get()));
                // CHECKSTYLE.OFF: EmptyBlock - Exception triggers fallback.
            } catch (final NumberFormatException nfe) {
                // CHECKSTYLE.ON: EmptyBlock
                // Ignore.
            }
        }
        if (annotations.getInitTimestamp().isPresent()) {
            try {
                return timestampToDateTime(Double.parseDouble(annotations.getInitTimestamp().get()));
                // CHECKSTYLE.OFF: EmptyBlock - Exception triggers fallback.
            } catch (final NumberFormatException nfe) {
                // CHECKSTYLE.ON: EmptyBlock
                // Ignore.
            }
        }
        throw new ParsingException(String.format("No timestamp found in annotations; annotations=%s", annotations));
    }

    private static DateTime timestampToDateTime(final double seconds) {
        return new DateTime(Math.round(seconds * 1000.0), ISOChronology.getInstanceUTC());
    }

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final String DATA_KEY = "data";
    private static final String VERSION_KEY = "version";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryLogParser.class);
    private static final RateLimitLogBuilder INVALID_SAMPLE_LOG = new RateLimitLogBuilder(
            LOGGER.warn().setMessage("Invalid sample for metric"),
            Duration.ofMinutes(1));

    private static final Function<String, Quantity> VERSION_2C_SAMPLE_TO_QUANTITY = new Function<String, Quantity>() {
        @Override
        public Quantity apply(final String sample) {
            if (sample != null) {
                try {
                    final double value = Double.parseDouble(sample);
                    if (Double.isFinite(value)) {
                        return new Quantity.Builder().setValue(value).build();
                    } else {
                        // TODO(barp): Create a counter for invalid metrics [AINT-680]
                        INVALID_SAMPLE_LOG.addData("value", sample).log();
                        return null;
                    }
                } catch (final NumberFormatException nfe) {
                    return null;
                }
            } else {
                return null;
            }
        }
    };

    private static final Function<Version2d.Sample, Quantity> VERSION_2D_SAMPLE_TO_QUANTITY = new Function<Version2d.Sample, Quantity>() {
        @Override
        public Quantity apply(final Version2d.Sample sample) {
            if (sample != null) {
                if (Double.isFinite(sample.getValue())) {
                    return new Quantity.Builder().setValue(sample.getValue()).setUnit(sample.getUnit().orNull()).build();
                } else {
                    // TODO(barp): Create a counter for invalid metrics [AINT-680]
                    INVALID_SAMPLE_LOG.addData("value", sample.getValue()).log();
                    return null;
                }
            } else {
                return null;
            }
        }
    };

    private static final Function<Version2e.Sample, Quantity> VERSION_2E_SAMPLE_TO_QUANTITY = new Function<Version2e.Sample, Quantity>() {
        @Override
        public Quantity apply(final Version2e.Sample sample) {
            if (sample != null) {
                if (Double.isFinite(sample.getValue())) {
                    return new Quantity.Builder().setValue(sample.getValue()).setUnit(sample.getUnit().orNull()).build();
                } else {
                    // TODO(barp): Create a counter for invalid metrics [AINT-680]
                    INVALID_SAMPLE_LOG.addData("value", sample.getValue()).log();
                    return null;
                }
            } else {
                return null;
            }
        }
    };

    static {
        final SimpleModule queryLogParserModule = new SimpleModule("QuerLogParser");
        BuilderDeserializer.addTo(queryLogParserModule, Version2c.class);
        BuilderDeserializer.addTo(queryLogParserModule, Version2d.class);
        BuilderDeserializer.addTo(queryLogParserModule, Version2e.class);
        queryLogParserModule.addDeserializer(
                Unit.class,
                EnumerationDeserializer.newInstance(
                        Unit.class,
                        EnumerationDeserializerStrategyUsingToUpperCase.<Unit>newInstance()));
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        OBJECT_MAPPER.registerModule(queryLogParserModule);
    }
}
