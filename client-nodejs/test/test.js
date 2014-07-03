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

// To enable verbosity, execute '$ VERBOSE=true mocha'
var hasVerboseArg = false;
process.argv.forEach(function(item, index) {
        if(item.toLowerCase().indexOf("--verbose")==0) {
            hasVerboseArg = true;
        }
    });

var verbose = hasVerboseArg ? true
         : typeof(process.env.VERBOSE) != undefined ? process.env.VERBOSE === "true"
         : false;

tsd = require("../lib/tsd-metrics-client") ({
   LOG_MAX_SIZE : 1000,
   LOG_BACKUPS : 2,
   LOG_CONSOLE_ECHO : verbose,
   LOG_FILE_NAME : "test-tsd-query.log"});
colors = require("colors");
assert = require("chai").assert;
fs = require("fs");

var log_schema = require('./querylog_schema.json');
var JaySchema = require('jayschema');

if(fs.existsSync(tsd.TsdMetrics.LOG_FILE_NAME)) {
    fs.unlinkSync(tsd.TsdMetrics.LOG_FILE_NAME);
}

var schemaValidator = new JaySchema();

function toSerializedLog(log) {
    return JSON.parse(JSON.stringify(log));
}

function createMetrics(validateSchema) {
    if(typeof(validateSchema) === "undefined") {
        validateSchema = true;
    }

    var m = new tsd.TsdMetrics();
    // add versbose error logging
    m.events().addListener("error", function (err) {
       print("Error:" + err);
    });

    if(validateSchema) {
        //add schema validator
        m.events().addListener("logEvent", function(log) {
            // the reason for emitting the log blob and parsing it back is the fact that the decision of
            // not emitting non stopped samples happens during the serialization
            var json = JSON.stringify(log);
            var schemaValidation = schemaValidator.validate(JSON.parse(json), log_schema);
            assert.lengthOf(schemaValidation, 0, "Schema Validation failed: " + JSON.stringify(schemaValidation));
        });
    }
    return m;
}

function aggregateErrors(metrics, errorArr) {
    metrics.events().addListener("error", function (err) {
        errorArr.push(err);
    });
}

function print(message) {
    if(verbose){
        if (Object.keys(arguments).length === 1) {
            console.log(message.yellow);
        } else {
            var args = arguments;
            delete args[0];
            console.log(message.yellow, args);
        }
    }
}

assert.timer = function assertDuration(durationObject, expectedMilliseconds, message) {
    assert.operator(Math.ceil(durationObject.getValue() / 1000), '>=', expectedMilliseconds, message);
    assert.equal(durationObject.getUnit().name, "nanosecond", "Non explicit timer unit is not 'nanosecond'");
}

assert.explicitTimer = function assertExplicitDuration(durationObject, expectedValue, expectedUnit, message) {
    assert.equal(durationObject.getValue(), expectedValue, message);
    assert.equal(durationObject.getUnit().name, expectedUnit, message);
}

assert.counter = function assertCounter(counterObject, expectedValue, message) {
    assert.equal(counterObject.getValue(), expectedValue, message);
    assert.equal(counterObject.getUnit(), undefined, "counter has a unit, while it shouldn't");
}

assert.gauge = function assertGauge(gaugeObject, expectedValue, expectedUnit, message){
    assert.equal(gaugeObject.getValue(), expectedValue, message);
    assert.equal(gaugeObject.getUnit().name, expectedUnit, message);
}

assert.gaugeDefault = function assertGaugeDefault(gaugeObject, expectedValue, message){
    assertGauge(gaugeObject, expectedValue, undefined, message)
}

describe('TsdMetrics', function () {
    describe('Verify Exports', function() {
      it('should export correctly with or without parameters passed to require() ', function (done) {
        tsdTestNoParams = require("../lib/tsd-metrics-client") ;
        assert.isDefined(tsdTestNoParams);
        assert.isDefined(tsdTestNoParams.TsdMetrics);

        tsdTestOptionalParams = require("../lib/tsd-metrics-client") ();
        assert.isDefined(tsdTestOptionalParams);
        assert.isDefined(tsdTestOptionalParams.TsdMetrics);

        done();
      });
    });

    describe('Base Case', function () {
        var helloCounter = Math.floor(Math.random() * 50.0);
        var worldCounter = Math.floor(Math.random() * 50.0);
        var customAnnotation ="HelloWorld";
        var gg0 = Math.floor(Math.random() * 50.0);
        var gg1 = Math.floor(Math.random() * 50.0);
        var ct0 = Date.now();

        it('should log counters, timers and gauges correctly', function (done) {
            var m = createMetrics();

            print("adding custom annotation " + customAnnotation);
            m.annotate(customAnnotation, customAnnotation);

            print("start timer1");
            m.startTimer("timer1");

            print("increment counter 'hello' by " + helloCounter);
            m.incrementCounter("hello", helloCounter);

            print("increment counter 'hello' by 1");
            m.incrementCounter("hello");

            print("reset a non incremented counter");
            m.resetCounter("brandNew");

            print("decrement counter 'world'");
            m.decrementCounter("world");

            print("reset counter 'world'");
            m.resetCounter("world");

            print("decrement counter 'world' by " + worldCounter);
            m.decrementCounter("world", worldCounter);

            print("set gauge 'gg' to " + gg0);
            m.setGauge("gg", gg0);

            print("set gauge 'gg' to " + gg1);
            m.setGauge("gg", gg1);

            print("set timer 'customTimer' to " + gg1);
            m.setTimer("customTimer", ct0);

            setTimeout(function () {
                print("stop timer1 after ~750ms");
                m.stopTimer("timer1");
                m.close();
                done();
            }, 750);

            m.events().addListener("logEvent", function (log) {
                assert.property(log.annotations, "initTimestamp");
                assert.property(log.annotations, "finalTimestamp");
                assert.property(log.annotations, customAnnotation);

                assert.counter(log.counters.brandNew.samples[0], 0, "resetCounter didn't creat counter with value 0");
                assert.counter(log.counters.hello.samples[0], helloCounter + 1, "increment counter happy case failed");
                assert.counter(log.counters.world.samples[0], -1, "decrement counter happy case failed");
                assert.counter(log.counters.world.samples[1], -worldCounter, "decrement additional sample failed");
                assert.counter(log.gauges.gg[0], gg0, "setting gauge failed");
                assert.counter(log.gauges.gg[1], gg1, "setting additional guage failed");
                assert.timer(log.timers.timer1.samples[0], 750, "log.timers.timer1[0] >= 750");
                assert.explicitTimer(log.timers.customTimer.samples[0], ct0, "millisecond", "setTimer failed to set the correct time duration");
            });
        });

        it("should not output counters, metrics or gauges if they are empty", function (done) {
            var m = createMetrics();

            print("Do nothing");
            m.events().addListener("logEvent", function (log) {
                var serializedLog = toSerializedLog(log);
                assert.isUndefined(serializedLog.counters, "empty counters list was emitted");
                assert.isUndefined(serializedLog.gauges, "empty gauges list was emitted");
                assert.isUndefined(serializedLog.timers, "empty timers list was emitted");
            });
            m.close();
            done();
        });

        it("should not output counters or gauges that are empty, but ouput empty timers with non stopped samples", function (done) {
            var m = createMetrics();

            m.startTimer("TIMER1");
            m.createTimer("Timer2");
            m.events().addListener("logEvent", function (log) {
                var serializedLog = toSerializedLog(log);
                assert.isUndefined(serializedLog.counters, "empty counters list was emitted");
                assert.isUndefined(serializedLog.gauges, "empty gauges list was emitted");
                assert.isDefined(serializedLog.timers, "timers list with empty samples was not emitted");
            });
            m.close();
            done();
        });
    });

    describe('Timers', function () {
        var t1 = 750;
        var t2 = 150;
        var t3 = 450;
        var t1_plust_t2 = t1 + t2;
        var t2_plust_t3 = t2 + t3;
        var t1_plust_t2_plus_t3 = t1 + t2 + t3;
        it('should measure multi samples and not auto stop timers on close', function (done) {
            var m = createMetrics();

            print("start timer1");
            m.startTimer("timer1");

            print("start timer2");
            m.startTimer("timer2");

            print("start timer3");
            m.startTimer("timer3");

            setTimeout(function () {
                print("stop timer1 after ~750ms");
                m.stopTimer("timer1");
                print("start timer1");
                m.startTimer("timer1");
                setTimeout(function () {
                    print("stop timer2 after ~" + t1_plust_t2 + "ms");
                    m.stopTimer("timer2");
                    setTimeout(function () {
                        print("stop timer1 after ~" + t2_plust_t3 + "ms");
                        m.stopTimer("timer1");
                        print("close (timer 3 is not auto stopped)");
                        m.close();
                        done();
                    }, t3);
                }, t2);
            }, t1);

            m.events().addListener("logEvent", function (log) {
                assert.timer(log.timers.timer1.samples[0], t1, "log.timers.timer1[0] >= " + t1);
                assert.timer(log.timers.timer1.samples[1], t2_plust_t3, "log.timers.timer1[0] >= " + t2_plust_t3);
                assert.timer(log.timers.timer2.samples[0], t1_plust_t2, "log.timers.timer2[0] >= " + t1_plust_t2);
                assert.isTrue(JSON.stringify(log).indexOf("\"timer3\":{\"values\":[]}") > -1, "timer 3 was auto stoped when not expected to")
                //assert.isUndefined(log.timers.timer3.samples[0].duration, "timer 3 was auto stoped when not expected to");
            });
        }),
        it('should correctly set multiple samples to timers using setTimer', function (done) {
            var m = createMetrics();

            print("set timer1 first sample = " + t1);
            m.setTimer("timer1", t1);

            print("set timer1 second sample = " + t2);
            m.setTimer("timer1", t2);

            m.events().addListener("logEvent", function (log) {
                assert.explicitTimer(log.timers.timer1.samples[0], t1, "millisecond", "log.timers.timer1[0] == " + t1);
                assert.explicitTimer(log.timers.timer1.samples[1], t2, "millisecond", "log.timers.timer1[1] == " + t2);
            });
            print("close metrics");
            m.close();
            done();
        }),

        it('should not emit timer sample for non closed samples', function (done) {
            var m = createMetrics();

            print("start timer1");
            m.createTimer("timer1");
            print("start timer1 second sample and stop it");
            m.createTimer("timer1").stop();

            print("start timer2");
            m.createTimer("timer2");
            print("start timer2 second sample");
            m.createTimer("timer2");

            setTimeout(function () {
                print("close (timer 3 is not auto stopped)");
                m.close();
                done();
            }, t3);


            m.events().addListener("logEvent", function (log) {
                // the reason for emitting the log blob and parsing it back is the fact that the decision of
                // not emitting non stopped samples happens during the serialization
                var serializedLog = toSerializedLog(log);

                assert.lengthOf(serializedLog.timers.timer1.values, 1, "non stopped timer sample was emitted");
                assert.lengthOf(serializedLog.timers.timer2.values, 0, "timer 2 was auto stoped when not expected to");
            });
        });
    });

    describe("Errors", function () {
        it('should report error on closing twice', function (done) {
            var m = createMetrics();
            var nonExistingTimer =  "not there";
            var errArr =[];
            aggregateErrors(m, errArr);

            var tmp = Math.floor(Math.random() * 50.0);
            print("increment counter 'hello' by " + tmp);
            m.incrementCounter("hello", tmp);
            tmp = Math.random() * 1000;
            print("set gauge 'gg' to " + tmp);
            m.setGauge("gg", tmp);
            print("stopping timer '" + nonExistingTimer + "'");
            m.stopTimer(nonExistingTimer);

            print("closing");
            m.close();
            print("closing again");
            m.close();

            assert.lengthOf(errArr, 2, "unexpected count of errors");

            assert.equal(errArr[0].toString(),
                new Error("Cannot stop timer '" + nonExistingTimer + "'. No samples have currently started for the timer").toString());

            assert.equal(errArr[1].toString(),
                new Error("Metrics object was not opened or it's already closed").toString());

            done();
        }),

        it('should report error on stopping timer twice', function (done) {
            var m = createMetrics();
            var errArr =[];
            aggregateErrors(m, errArr);

            print("start timer T1");
            m.startTimer("T1");
            print("stop timer once");
            m.stopTimer("T1");
            print("stop timer twice");
            m.stopTimer("T1");
            m.close();

            assert.lengthOf(errArr, 1, "unexpected count of errors");
            done();
        }),

        it('should report error when calling metrics methods after closing the metrics object', function (done) {
            var m = createMetrics();
            var errArr =[];
            aggregateErrors(m, errArr);

            print("close the metrics object");
            m.close();

            m.incrementCounter();
            m.resetCounter();
            m.startTimer();
            m.setGauge();
            m.setTimer();
            m.annotate("bla")

            assert.lengthOf(errArr, 6, "unexpected count of errors");
            done();
        }),

        it('should report error if LOGGER failed', function (done) {
            var m = createMetrics();
            var errMsg = "LOGGER ERROR";
            var getValue = tsd.TsdMetrics.LOGGER.getValue;
            tsd.TsdMetrics.LOGGER.getValue = function(){
                throw new Error(errMsg)
            }
            var errArr =[];
            aggregateErrors(m, errArr);

            print("close the metrics object");
            m.createCounter("TEST");
            m.close();

            tsd.TsdMetrics.LOGGER.getValue = getValue;
            assert.lengthOf(errArr, 1, "unexpected count of errors");
            assert.equal(errArr[0].toString(),
                 new Error(errMsg).toString());

            done();
        });

        it('should report error if OOP create* called after close', function (done) {
            var m = createMetrics();
            var getValue = tsd.TsdMetrics.LOGGER.getValue;
            var errArr =[];
            aggregateErrors(m, errArr);

            print("close the metrics object");
            m.close();
            m.createTimer("TEST");
            m.createCounter("TEST");

            assert.lengthOf(errArr, 2, "unexpected count of errors");
            done();
        });

        it('should report error if OOP counters/timers incremented/stopped after close', function (done) {
            var m = createMetrics(false);
            var getValue = tsd.TsdMetrics.LOGGER.getValue;
            var errArr =[];
            aggregateErrors(m, errArr);

            print("close the metrics object");
            var timer = m.createTimer("TEST_TIMER");
            var counter = m.createCounter("TEST_COUNTER");
            m.close(); //error 1: skipping TEST_TIMER serialization
            timer.stop(); //error 2
            counter.increment(); //error 3

            assert.lengthOf(errArr, 3, "unexpected count of errors");
            done();
        });

        it('should report error setTimer was called without unit', function (done) {
            var m = createMetrics(false);
            var errArr =[];
            aggregateErrors(m, errArr);

            m.setTimer("TEST_TIMER", 123);

            m.events().addListener("logEvent", function (log) {
                assert.explicitTimer(
                    log.timers.TEST_TIMER.samples[0], 123,
                    "millisecond",
                    "Unexpected value while setting explicit timer");
            });
            m.close();

            assert.lengthOf(errArr,1, "unexpected count of errors");
            done();
        });
    });

    describe('OOP', function () {
        var helloCounterValue = Math.floor(Math.random() * 50.0);
        var worldCounterValue = Math.floor(Math.random() * 50.0);
        var customAnnotation ="HelloWorld";
        var gg0 = Math.floor(Math.random() * 50.0);
        var gg1 = Math.floor(Math.random() * 50.0);
        var ct0 = Date.now();

        it('should log counters, timers and gauges correctly using OOP', function (done) {
            var m = createMetrics();

            print("start timer1");
            var timer1 = m.createTimer("timer1");

            print("increment counter 'hello' by " + helloCounterValue);
            var helloCounter = m.createCounter("hello");
            helloCounter.increment(helloCounterValue);

            print("increment counter 'hello' by 1");
            helloCounter.increment();

            print("creat new 'hello' counter sample");
            var helloCounter = m.createCounter("hello");

            print("increment counter 'hello' by 1");
            helloCounter.increment();

            print("decrement counter 'world'");
            var worldCounter = m.createCounter("world");
            worldCounter.decrement();

            print("create new sample for counter 'world'");
            worldCounter = m.createCounter("world");;

            print("decrement counter 'world' by " + worldCounterValue);
            worldCounter.decrement(worldCounterValue);

            setTimeout(function () {
                print("stop timer1 after ~750ms");
                timer1.stop();
                m.close();
                done();
            }, 750);

            m.events().addListener("logEvent", function (log) {
                assert.property(log.annotations, "initTimestamp");
                assert.property(log.annotations, "finalTimestamp");

                assert.counter(log.counters.hello.samples[0], helloCounterValue + 1, "increment counter happy case failed");
                assert.counter(log.counters.hello.samples[1], 1, "increment counter happy case failed");
                assert.counter(log.counters.world.samples[0], -1, "decrement counter happy case failed");
                assert.counter(log.counters.world.samples[1], -worldCounterValue, "decrement additional sample failed");
                assert.timer(log.timers.timer1.samples[0], 750, "log.timers.timer1[0] >= 750");
            });
        }),

        it('should play nice when using OOP and functional', function (done) {
            var m = createMetrics();

            print("start timer1 OOP");
            var timer1 = m.createTimer("timer1");

            print("start timer1 Func");
            m.startTimer("timer1");

            print("start timer2 Func");
            m.startTimer("timer2");

            print("increment counter 'hello' by " + helloCounterValue);
            var helloCounter = m.createCounter("hello");
            helloCounter.increment(helloCounterValue);

            print("increment counter 'hello' by 1");
            helloCounter.increment();

            print("creat new 'hello' counter sample using functional and increment by 1");
            m.incrementCounter("hello");

            print("creat new 'hello' counter sample using OOP and increment by " + helloCounterValue);
            helloCounter = m.createCounter("hello");
            helloCounter.increment(helloCounterValue);

            print("increment counter 'hello' by 1");
            helloCounter.increment();

            m.events().addListener("logEvent", function (log) {
                assert.counter(log.counters.hello.samples[0], helloCounterValue + 1, "increment counter happy case failed");
                assert.counter(log.counters.hello.samples[1], 1, "increment counter happy case failed");

                assert.timer(log.timers.timer1.samples[0], 750, "log.timers.timer1[0] >= 750");
                assert.timer(log.timers.timer1.samples[1], 850, "log.timers.timer1[1] >= 850");
                assert.timer(log.timers.timer2.samples[0], 950, "log.timers.timer1[0] >= 950");
            });

            setTimeout(function () {
                print("stop timer1 after ~750ms");
                timer1.stop();
                setTimeout(function() {
                    m.stopTimer("timer1");
                    setTimeout(function() {
                        m.stopTimer("timer2");
                        m.close();
                        done();
                    }, 100)
                }, 100)
            }, 750);
        }),

        it('should work as expected in real case', function (done) {
            var m = createMetrics();
            var opsCounter = 10;
            var iterations = opsCounter;
            var durations = [];
            var counts =[];

            for(var i = 0; i < iterations; ++i){
                var timer = m.createTimer("timeit");
                durations.push(Math.random() * 1000);
                setTimeout(function(t){
                    t.stop();;
                    --opsCounter;
                    if(opsCounter == 0){
                        m.close();
                    }
                }, durations[i], timer);

                var counter = m.createCounter("countit");
                counts.push(Math.floor(Math.random() * 10));
                for(var j = 0; j < counts[i]; ++j){
                    counter.increment();
                }
            }
            m.events().addListener("logEvent", function (log) {
                for(var k = 0; k < iterations; ++k){
                    assert.counter(log.counters.countit.samples[k], counts[k], "increment counter happy case failed");
                    assert.timer(log.timers.timeit.samples[k], durations[k],
                        "log.timers.timeit.samples['"+ k +"'] >= " + durations[k]);
                }
                done();
            });
        });
    });

    describe('Metrics with units', function () {
        it('should serialize gauges and explcit timers with correct units', function (done) {
            var m = createMetrics();
            m.setGauge("g1",1, tsd.Units.GIGABYTE);
            m.setTimer("t1",1123, tsd.Units.WEEK);
            m.events().addListener("logEvent", function (log) {
                assert.gauge(log.gauges.g1[0], 1, "gigabyte", "gauge g1's unit or value incorrect");
                assert.explicitTimer(log.timers.t1.samples[0], 1123, "week", "gauge t1's unit or value incorrect");
                done();
            });
            m.close();
        });
    });
});
