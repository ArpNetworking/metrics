/*
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

var util = require("util");
var tsd = require("../lib/tsd-metrics-client");
var assert = require("chai").assert;
var testCommon = require("./test-common");
var log_schema = require('../../doc/query-log-schema-2e.json');
var JaySchema = require('jayschema');
var fs = require("fs");
var TEST_LOGFILE_NAME = "test-tsd-query.log";
if (fs.existsSync(TEST_LOGFILE_NAME)) {
    fs.unlinkSync(TEST_LOGFILE_NAME);
}

var schemaValidator = new JaySchema();

var errorArr = [];
var skipErrorValidation = false;

tsd.onError(function (err) {
    testCommon.printError("Error:" + err);
    errorArr.push(err);
});

function clearErrors() {
    errorArr.length = 0;
    skipValidateSchema = true;
    skipErrorValidation = false;
}


var testSinks = [];
if (testCommon.verbose) {
    testSinks.push(tsd.Sinks.createConsoleSink());
}

function createMetrics() {
    return new tsd.TsdMetrics();
}

function validateSchema(jsonObject) {
    var schemaValidation = schemaValidator.validate(jsonObject, log_schema);
    assert.lengthOf(schemaValidation, 0, "Schema Validation failed: " + JSON.stringify(schemaValidation));
}

describe('Query log sink', function () {
    var queryLogSink = tsd.Sinks.createQueryLogSink(TEST_LOGFILE_NAME, 1000, 2);

    //let's sniff the wire
    var originalInfo = queryLogSink.logger.info;

    function sinkSniffer(replacement) {
        queryLogSink.logger.info = function (serializedEvent) {
            testCommon.print(JSON.stringify(JSON.parse(serializedEvent), null, 2));
            replacement(serializedEvent);
        }
    }

    beforeEach(function () {
        clearErrors();
        tsd.init(testSinks.concat([queryLogSink]));
    });

    afterEach(function () {
        if (!skipErrorValidation) {
            if (errorArr.length > 0) {
                this.test.error(new Error("Errors reported when non is expected.\n" + errorArr.toString()));
            }
        }
    });

    it('should output counters, timers and gauges correctly according to schema', function (done) {
        var helloCounter = Math.floor(Math.random() * 50.0);
        var worldCounter = Math.floor(Math.random() * 50.0);
        var customAnnotation = "HelloWorld";
        var gg0 = Math.floor(Math.random() * 50.0);
        var gg1 = Math.floor(Math.random() * 50.0);
        var ct0 = Date.now();

        var m = createMetrics();

        m.annotate(customAnnotation, customAnnotation);
        m.startTimer("timer1");
        m.incrementCounter("hello", helloCounter);
        m.incrementCounter("hello");
        m.resetCounter("brandNew");
        m.decrementCounter("world");
        m.resetCounter("world");
        m.decrementCounter("world", worldCounter);
        m.setGauge("gg", gg0);
        m.setGauge("gg", gg1);
        m.setTimer("customTimer", ct0, tsd.Units.MILLISECOND);

        var deserializedEvent = undefined;
        //don't run assertions within the sniffer, because the sniffer runs within the try catch block of the
        //metrics objects and within the test error listener, so assertion error would interfere with these and
        //cause global catastrophe that makes knowing what happen not so easy
        sinkSniffer(function (serializedEvent) {
            deserializedEvent = JSON.parse(serializedEvent);
        });

        setTimeout(function () {
            testCommon.print("stop timer1 after ~750ms");
            m.stopTimer("timer1");
            m.close();
            validateSchema(deserializedEvent);

            assert.property(deserializedEvent.data.annotations, "initTimestamp");
            assert.property(deserializedEvent.data.annotations, "finalTimestamp");
            assert.property(deserializedEvent.data.annotations, customAnnotation);

            assert.equal(deserializedEvent.data.counters.brandNew.values[0].value, 0,
                "resetCounter didn't create counter with value 0");
            assert.equal(deserializedEvent.data.counters.hello.values[0].value, helloCounter + 1,
                "increment counter happy case failed");
            assert.equal(deserializedEvent.data.counters.world.values[0].value, -1,
                "decrement counter happy case failed");
            assert.equal(deserializedEvent.data.counters.world.values[1].value, -worldCounter,
                "decrement additional sample failed");
            assert.equal(deserializedEvent.data.gauges.gg.values[0].value, gg0, "setting gauge failed");
            assert.equal(deserializedEvent.data.gauges.gg.values[1].value, gg1, "setting additional guage failed");
            assert.timerValue(deserializedEvent.data.timers.timer1.values[0].value, 750,
                "log.timers.timer1.values[0] >= 750");
            assert.equal(deserializedEvent.data.timers.customTimer.values[0].value, ct0,
                "setTimer failed to set the correct time duration");
            assert.equal(deserializedEvent.data.timers.customTimer.values[0].unit, "millisecond",
                "setTimer failed to set the correct time unit");
            done();
        }, 750);
    });

    it("should not output counters or gauges that are empty, but output empty timers with non stopped samples", function (done) {
        skipErrorValidation = true;
        var m = createMetrics();

        m.startTimer("TIMER1");
        m.createTimer("Timer2");
        var deserializedEvent = undefined;
        sinkSniffer(function (serializedEvent) {
            deserializedEvent = JSON.parse(serializedEvent);
        });
        m.close(); //serialization reports two errors

        validateSchema(deserializedEvent);
        assert.property(deserializedEvent.data.annotations, "initTimestamp");
        assert.property(deserializedEvent.data.annotations, "finalTimestamp");
        assert.notProperty(deserializedEvent.data, "counters");
        assert.notProperty(deserializedEvent.data, "gauges");
        assert.property(deserializedEvent.data, "timers");
        assert.lengthOf(deserializedEvent.data.timers.TIMER1.values, 0);
        assert.lengthOf(deserializedEvent.data.timers.Timer2.values, 0);

        assert.lengthOf(errorArr, 2, "unexpected count of errors");

        assert.include(errorArr[0].toString(), "TIMER1");
        assert.include(errorArr[1].toString(), "Timer2");

        done();
    });

    //TODO: Move to sink tests
    it('should not emit timer sample for non closed samples', function (done) {
        skipErrorValidation = true;
        var m = createMetrics();

        testCommon.print("start timer1");
        m.createTimer("timer1");
        testCommon.print("start timer1 second sample and stop it");
        m.createTimer("timer1").stop();

        testCommon.print("start timer2");
        m.createTimer("timer2");
        testCommon.print("start timer2 second sample");
        m.createTimer("timer2");

        var deserializedEvent = undefined;
        sinkSniffer(function (serializedEvent) {
            deserializedEvent = JSON.parse(serializedEvent);
        });

        setTimeout(function () {
            testCommon.print("close (timer 3 is not auto stopped)");
            m.close();
            assert.lengthOf(deserializedEvent.data.timers.timer1.values, 1, "non stopped timer sample was emitted");
            assert.lengthOf(deserializedEvent.data.timers.timer2.values, 0, "timer 2 was auto stopped when not expected to");

            assert.lengthOf(errorArr, 3, "unexpected count of errors. Errors dump: " + errorArr.toString());
            done();
        }, 100);
    });

    it("should not output counters, metrics or gauges if they are empty", function (done) {
        var m = createMetrics();

        testCommon.print("Do nothing");
        var deserializedEvent = undefined;
        sinkSniffer(function (serializedEvent) {
            deserializedEvent = JSON.parse(serializedEvent);
        });
        m.close();

        validateSchema(deserializedEvent);
        assert.property(deserializedEvent.data.annotations, "initTimestamp");
        assert.property(deserializedEvent.data.annotations, "finalTimestamp");
        assert.notProperty(deserializedEvent.data, "counters");
        assert.notProperty(deserializedEvent.data, "gauges");
        assert.notProperty(deserializedEvent.data, "timers");
        done();
    });

    it("should test correctly for metrics list and sample", function (done) {
        function TestSink() {}

        util.inherits(TestSink, tsd.Sink);

        var emittedMetricEvent;
        TestSink.prototype.record = function (metricsEvent) {
            emittedMetricEvent = metricsEvent;
        };
        tsd.init(testSinks.concat([new TestSink()]));
        var m = createMetrics();
        m.incrementCounter("test");
        m.close();

        assert.isTrue(tsd.Sink.isMetricsList(emittedMetricEvent.counters.test));
        assert.isFalse(tsd.Sink.isMetricsList(emittedMetricEvent.counters.test[0]));

        assert.isTrue(tsd.Sink.isMetricSample(emittedMetricEvent.counters.test.getValues()[0]));
        assert.isFalse(tsd.Sink.isMetricSample(emittedMetricEvent.counters.test.getValues()));
        done();
    });
});

describe('Console log sink', function () {
    var consoleLogSink = tsd.Sinks.createConsoleSink();

    function sinkSniffer(replacement) {
        consoleLogSink._console = {};
        consoleLogSink._console.log = function (serializedEvent) {
            replacement(serializedEvent);
        }
    }

    function TestSink() {
    }

    util.inherits(TestSink, tsd.Sink);

    var emittedMetricEvent;
    TestSink.prototype.record = function (metricsEvent) {
        emittedMetricEvent = metricsEvent;
    };


    beforeEach(function () {
        clearErrors();
        tsd.init(testSinks.concat([new TestSink(), consoleLogSink]));
    });

    afterEach(function () {
        if (!skipErrorValidation) {
            if (errorArr.length > 0) {
                this.test.error(new Error("Errors reported when non is expected.\n" + errorArr.toString()));
            }
        }
    });

    it('should work', function (done) {
        var helloCounter = Math.floor(Math.random() * 50.0);
        var worldCounter = Math.floor(Math.random() * 50.0);
        var customAnnotation = "HelloWorld";
        var gg0 = Math.floor(Math.random() * 50.0);
        var gg1 = Math.floor(Math.random() * 50.0);
        var ct0 = Date.now();

        var m = createMetrics();

        m.annotate(customAnnotation, customAnnotation);
        m.startTimer("timer1");
        m.incrementCounter("hello", helloCounter);
        m.incrementCounter("hello");
        m.resetCounter("brandNew");
        m.decrementCounter("world");
        m.resetCounter("world");
        m.decrementCounter("world", worldCounter);
        m.setGauge("gg", gg0);
        m.setGauge("gg", gg1);
        m.setTimer("customTimer", ct0, tsd.Units.MILLISECOND);

        var serializedEvent = undefined;

        sinkSniffer(function (data) {
            serializedEvent = data;
        });

        setTimeout(function () {
            testCommon.print("stop timer1 after ~750ms");
            m.stopTimer("timer1");
            m.close();

            assert.equal(JSON.stringify(emittedMetricEvent, function (key, value) {
                if (tsd.Sink.isMetricSample(value)) {
                    return {
                        value: value.getValue(),
                        unit: value.getUnit()
                    };
                }
                if (tsd.Sink.isMetricsList(value)) {
                    var listValue = {};
                    listValue[key] = {
                        values: value.getValues()
                    };
                    return {
                        values: value.getValues()
                    };
                }
                if (key[0] !== "_") {
                    return value;
                }
            }, 2), serializedEvent);
            done();
        }, 750);
    });
});