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
var expect = require("chai").expect;
var fs = require("fs");
var testCommon = require("./test-common");

function TestSink() {}

util.inherits(TestSink, tsd.Sink);

var emittedMetricEvent;
TestSink.prototype.record = function(metricsEvent) {
  emittedMetricEvent = metricsEvent;
};

var testSink = new TestSink();
var metricsTestSinks = [testSink];
if (testCommon.verbose) {
  metricsTestSinks.push(tsd.Sinks.createConsoleSink());
}

function createMetrics() {
  return new tsd.TsdMetrics();
}

var errorArr = [];
var skipErrorValidation = false;

tsd.onError(function(err) {
  testCommon.printError("Error:" + err);
  errorArr.push(err);
});

function clearErrors() {
  errorArr.length = 0;
  skipErrorValidation = false;
}

describe('TsdMetrics', function() {
  beforeEach(function() {
    clearErrors();
    tsd.init(metricsTestSinks);
  });
  afterEach(function() {
    if (!skipErrorValidation) {
      if (errorArr.length > 0) {
        this.test.error(new Error("Errors reported when non is expected.\n" + errorArr.toString()));
      }
    }
  });
  describe('Verify Exports', function() {
    it('should export correctly with or without parameters passed to require() ', function(done) {
      var tsdTestNoParams = require("../lib/tsd-metrics-client");
      assert.isDefined(tsdTestNoParams);
      assert.isDefined(tsdTestNoParams.TsdMetrics);

      var tsdTestOptionalParams = require("../lib/tsd-metrics-client")();
      assert.isDefined(tsdTestOptionalParams);
      assert.isDefined(tsdTestOptionalParams.TsdMetrics);

      done();
    });

    it('should export correctly using old require options', function(done) {
      var tsdTestWithParams = require("../lib/tsd-metrics-client")({
        LOG_MAX_SIZE: 1000,
        LOG_BACKUPS: 2,
        LOG_CONSOLE_ECHO: true,
        LOG_FILE_NAME: "test-tsd-query.log"
      });

      assert.instanceOf(tsd.init._sinks[0], tsd.Sinks.createQueryLogSink().constructor);
      assert.instanceOf(tsd.init._sinks[1], tsd.Sinks.createConsoleSink().constructor);

      assert.isDefined(tsdTestWithParams);
      assert.isDefined(tsdTestWithParams.TsdMetrics);

      var tsdTestOptionalParams = require("../lib/tsd-metrics-client")();
      assert.isDefined(tsdTestOptionalParams);
      assert.isDefined(tsdTestOptionalParams.TsdMetrics);

      done();
    });
  });

  describe('Base Case', function() {
    var helloCounter = Math.floor(Math.random() * 50.0);
    var worldCounter = Math.floor(Math.random() * 50.0);
    var customAnnotation = "HelloWorld";
    var gg0 = Math.floor(Math.random() * 50.0);
    var gg1 = Math.floor(Math.random() * 50.0);
    var ct0 = Date.now();

    it('should log counters, timers and gauges correctly', function(done) {
      var m = createMetrics();

      testCommon.print("adding custom annotation " + customAnnotation);
      m.annotate(customAnnotation, customAnnotation);

      testCommon.print("start timer1");
      m.startTimer("timer1");

      testCommon.print("increment counter 'hello' by " + helloCounter);
      m.incrementCounter("hello", helloCounter);

      testCommon.print("increment counter 'hello' by 1");
      m.incrementCounter("hello");

      testCommon.print("reset a non incremented counter");
      m.resetCounter("brandNew");

      testCommon.print("decrement counter 'world'");
      m.decrementCounter("world");

      testCommon.print("reset counter 'world'");
      m.resetCounter("world");

      testCommon.print("decrement counter 'world' by " + worldCounter);
      m.decrementCounter("world", worldCounter);

      testCommon.print("set gauge 'gg' to " + gg0);
      m.setGauge("gg", gg0);

      testCommon.print("set gauge 'gg' to " + gg1);
      m.setGauge("gg", gg1);

      testCommon.print("set timer 'customTimer' to " + gg1);
      m.setTimer("customTimer", ct0, tsd.Units.MILLISECOND);

      setTimeout(function() {
        testCommon.print("stop timer1 after ~750ms");
        m.stopTimer("timer1");
        m.close();
        assert.property(emittedMetricEvent.annotations, "initTimestamp");
        assert.property(emittedMetricEvent.annotations, "finalTimestamp");
        assert.property(emittedMetricEvent.annotations, customAnnotation);

        assert.counter(emittedMetricEvent.counters.brandNew.getValues()[0], 0,
          "resetCounter didn't creat counter with value 0");
        assert.counter(emittedMetricEvent.counters.hello.getValues()[0], helloCounter + 1,
          "increment counter happy case failed");
        assert.counter(emittedMetricEvent.counters.world.getValues()[0], -1,
          "decrement counter happy case failed");
        assert.counter(emittedMetricEvent.counters.world.getValues()[1], -worldCounter,
          "decrement additional sample failed");
        assert.counter(emittedMetricEvent.gauges.gg.getValues()[0], gg0, "setting gauge failed");
        assert.counter(emittedMetricEvent.gauges.gg.getValues()[1], gg1,
          "setting additional guage failed");
        assert.timer(emittedMetricEvent.timers.timer1.getValues()[0], 750,
          "log.timers.timer1.getValues()[0] >= 750");
        assert.explicitTimer(emittedMetricEvent.timers.customTimer.getValues()[0], ct0, "millisecond",
          "setTimer failed to set the correct time duration");
        done();
      }, 750);
    });

    it("should return true when isOpen when metrics object not close, and return false otherwise ", function(
      done) {
      var m = createMetrics();

      m.incrementCounter("c");
      assert.isTrue(m.isOpen(), "metrics.isOpen return false while the object is not closed");
      m.close();
      assert.isFalse(m.isOpen(), "metrics.isOpen return true while the object is closed");
      done();
    });
  });

  describe('Timers', function() {
    var t1 = 750;
    var t2 = 150;
    var t3 = 450;
    var t1_plus_t2 = t1 + t2;
    var t2_plus_t3 = t2 + t3;

    it('should measure multi samples and not auto stop timers on close', function(done) {
      var m = createMetrics();

      testCommon.print("start timer1");
      m.startTimer("timer1");

      testCommon.print("start timer2");
      m.startTimer("timer2");

      testCommon.print("start timer3");
      m.startTimer("timer3");

      setTimeout(function() {
        testCommon.print("stop timer1 after ~750ms");
        m.stopTimer("timer1");
        testCommon.print("start timer1");
        m.startTimer("timer1");
        setTimeout(function() {
          testCommon.print("stop timer2 after ~" + t1_plus_t2 + "ms");
          m.stopTimer("timer2");
          setTimeout(function() {
            testCommon.print("stop timer1 after ~" + t2_plus_t3 + "ms");
            m.stopTimer("timer1");
            testCommon.print("close (timer 3 is not auto stopped)");
            m.close();
            assert.timer(emittedMetricEvent.timers.timer1.getValues()[0], t1,
              "log.timers.timer1.getValues()[0] >= " + t1);
            assert.timer(emittedMetricEvent.timers.timer1.getValues()[1], t2_plus_t3,
              "log.timers.timer1.getValues()[0] >= " + t2_plus_t3);
            assert.timer(emittedMetricEvent.timers.timer2.getValues()[0], t1_plus_t2,
              "log.timers.timer2.getValues()[0] >= " + t1_plus_t2);
            assert.isUndefined(emittedMetricEvent.timers.timer3.getValues()[0],
              "timer 3 was auto stopped when not expected to");
            done();
          }, t3);
        }, t2);
      }, t1);

      skipErrorValidation = true;
    });

    it('should correctly set multiple samples to timers using setTimer', function(done) {
      var m = createMetrics();

      testCommon.print("set timer1 first sample = " + t1);
      m.setTimer("timer1", t1, tsd.Units.MILLISECOND);

      testCommon.print("set timer1 second sample = " + t2);
      m.setTimer("timer1", t2, tsd.Units.MILLISECOND);

      testCommon.print("close metrics");
      m.close();

      assert.explicitTimer(emittedMetricEvent.timers.timer1.getValues()[0], t1, "millisecond",
        "log.timers.timer1.getValues()[0] == " + t1);
      assert.explicitTimer(emittedMetricEvent.timers.timer1.getValues()[1], t2, "millisecond",
        "log.timers.timer1.getValues()[1] == " + t2);
      done();
    });

    it('should return false when timer.isStopped while timer is not stopped, and return true when it is',
      function(done) {
        var m = createMetrics();

        testCommon.print("start timer1");
        var timer1 = m.createTimer("timer1");

        assert.isFalse(timer1.isStopped(), "Timer.isStopped() return true while the timer is not stopped");

        timer1.stop();

        assert.isTrue(timer1.isStopped(), "Timer.isStopped() return false while the timer is stopped");
        m.close();
        done();
      });
  });

  describe("Errors", function() {
    beforeEach(function() {
      clearErrors();
      tsd.init(metricsTestSinks);
      skipErrorValidation = true;
    });
    it('should report error on closing twice', function(done) {
      var m = createMetrics();
      var nonExistingTimer = "not there";

      var tmp = Math.floor(Math.random() * 50.0);
      testCommon.print("increment counter 'hello' by " + tmp);
      m.incrementCounter("hello", tmp);
      tmp = Math.random() * 1000;
      testCommon.print("set gauge 'gg' to " + tmp);
      m.setGauge("gg", tmp);
      testCommon.print("stopping timer '" + nonExistingTimer + "'");
      m.stopTimer(nonExistingTimer);

      testCommon.print("closing");
      m.close();
      testCommon.print("closing again");
      m.close();

      assert.lengthOf(errorArr, 2, "unexpected count of errors");

      assert.equal(errorArr[0].toString(),
        new Error("Cannot stop timer '" + nonExistingTimer +
          "'. No samples have currently started for the timer").toString());

      assert.equal(errorArr[1].toString(),
        new Error("Metrics object was not opened or it's already closed").toString());

      done();
    });

    it('should report error on stopping timer twice', function(done) {
      var m = createMetrics();

      testCommon.print("start timer T1");
      m.startTimer("T1");
      testCommon.print("stop timer once");
      m.stopTimer("T1");
      testCommon.print("stop timer twice");
      m.stopTimer("T1");
      m.close();

      assert.lengthOf(errorArr, 1, "unexpected count of errors");
      done();
    });

    it('should report error when calling metrics methods after closing the metrics object', function(done) {
      var m = createMetrics();

      testCommon.print("close the metrics object");
      m.close();

      m.incrementCounter();
      m.resetCounter();
      m.startTimer();
      m.setGauge();
      m.setTimer();
      m.annotate("bla")

      assert.lengthOf(errorArr, 6, "unexpected count of errors");
      done();
    });

    it('should report error if a sink failed', function(done) {
      var m = createMetrics();
      var errMsg = "LOGGER ERROR";

      function FaultySink() {}

      util.inherits(FaultySink, tsd.Sink);

      FaultySink.prototype.record = function(metricsEvent) {
        throw new Error(errMsg);
      };

      tsd.init([new FaultySink()]);

      testCommon.print("close the metrics object");
      m.createCounter("TEST");
      m.close();

      tsd.init(metricsTestSinks);
      assert.lengthOf(errorArr, 1, "unexpected count of errors");
      assert.include(errorArr[0].toString(), "FaultySink");
      assert.include(errorArr[0].toString(), errMsg);

      done();
    });

    it('should report error correctly from sinks', function(done) {
      var m = createMetrics();
      var errMsg = "LOGGER ERROR";

      function ErrorReportingSink() {}

      util.inherits(ErrorReportingSink, tsd.Sink);

      ErrorReportingSink.prototype.record = function(metricsEvent) {
        throw new Error(errMsg);
      };

      tsd.init([new ErrorReportingSink()]);

      testCommon.print("close the metrics object");
      m.createCounter("TEST");
      m.close();

      tsd.init(metricsTestSinks);
      assert.lengthOf(errorArr, 1, "unexpected count of errors");
      assert.include(errorArr[0].toString(), "ErrorReportingSink");
      assert.include(errorArr[0].toString(), errMsg);
      done();
    });

    it('should report error if OOP create* called after close', function(done) {
      var m = createMetrics();

      testCommon.print("close the metrics object");
      m.close();
      m.createTimer("TEST");
      m.createCounter("TEST");

      assert.lengthOf(errorArr, 2, "unexpected count of errors");
      done();
    });

    it('should report error if OOP counters/timers incremented/stopped after close', function(done) {
      var m = createMetrics(false);

      tsd.init(metricsTestSinks);
      testCommon.print("close the metrics object");
      var timer = m.createTimer("TEST_TIMER");
      var counter = m.createCounter("TEST_COUNTER");
      m.close(); //error 1: unstopped timers
      timer.stop(); //error 2: stop after close
      counter.increment(); //error 3: increment after close

      assert.lengthOf(errorArr, 3, "unexpected count of errors" + errorArr);
      done();
    });

    it('should report error setTimer was called without unit', function(done) {
      var m = createMetrics(false);

      m.setTimer("TEST_TIMER", 123);
      m.close();

      assert.explicitTimer(
        emittedMetricEvent.timers.TEST_TIMER.getValues()[0], 123,
        "millisecond",
        "Unexpected value while setting explicit timer");

      assert.lengthOf(errorArr, 1, "unexpected count of errors");
      done();
    });

    it('should throw if non function is passed to onError', function(done) {
      //wrapping the call in an anonymous function seems to be the way to test function's throw without
      //propagating the exception to the test framework and fail the test case
      //(http://stackoverflow.com/a/19150023/736518)
      expect(function() {
        tsd.onError("NOT A FUNC")
      }).to.throw('errorCallback is not a function');
      done();
    });
  });

  describe('OOP', function() {
    var helloCounterValue = Math.floor(Math.random() * 50.0);
    var worldCounterValue = Math.floor(Math.random() * 50.0);

    it('should log counters, timers and gauges correctly using OOP', function(done) {
      var m = createMetrics();

      testCommon.print("start timer1");
      var timer1 = m.createTimer("timer1");

      testCommon.print("increment counter 'hello' by " + helloCounterValue);
      var helloCounter = m.createCounter("hello");
      helloCounter.increment(helloCounterValue);

      testCommon.print("increment counter 'hello' by 1");
      helloCounter.increment();

      testCommon.print("creat new 'hello' counter sample");
      helloCounter = m.createCounter("hello");

      testCommon.print("increment counter 'hello' by 1");
      helloCounter.increment();

      testCommon.print("decrement counter 'world'");
      var worldCounter = m.createCounter("world");
      worldCounter.decrement();

      testCommon.print("create new sample for counter 'world'");
      worldCounter = m.createCounter("world");

      testCommon.print("decrement counter 'world' by " + worldCounterValue);
      worldCounter.decrement(worldCounterValue);

      setTimeout(function() {
        testCommon.print("stop timer1 after ~750ms");
        timer1.stop();
        m.close();

        assert.property(emittedMetricEvent.annotations, "initTimestamp");
        assert.property(emittedMetricEvent.annotations, "finalTimestamp");

        assert.counter(emittedMetricEvent.counters.hello.getValues()[0], helloCounterValue + 1,
          "increment counter happy case failed");
        assert.counter(emittedMetricEvent.counters.hello.getValues()[1], 1,
          "increment counter happy case failed");
        assert.counter(emittedMetricEvent.counters.world.getValues()[0], -1,
          "decrement counter happy case failed");
        assert.counter(emittedMetricEvent.counters.world.getValues()[1], -worldCounterValue,
          "decrement additional sample failed");
        assert.timer(emittedMetricEvent.timers.timer1.getValues()[0], 750,
          "log.timers.timer1[0] >= 750");
        done();
      }, 750);
    });

    it('should play nice when using OOP and functional', function(done) {
      var m = createMetrics();

      testCommon.print("start timer1 OOP");
      var timer1 = m.createTimer("timer1");

      testCommon.print("start timer1 Func");
      m.startTimer("timer1");

      testCommon.print("start timer2 Func");
      m.startTimer("timer2");

      testCommon.print("increment counter 'hello' by " + helloCounterValue);
      var helloCounter = m.createCounter("hello");
      helloCounter.increment(helloCounterValue);

      testCommon.print("increment counter 'hello' by 1");
      helloCounter.increment();

      testCommon.print("create new 'hello' counter sample using functional and increment by 1");
      m.incrementCounter("hello");

      testCommon.print("create new 'hello' counter sample using OOP and increment by " + helloCounterValue);
      helloCounter = m.createCounter("hello");
      helloCounter.increment(helloCounterValue);

      testCommon.print("increment counter 'hello' by 1");
      helloCounter.increment();

      setTimeout(function() {
        testCommon.print("stop timer1 after ~750ms");
        timer1.stop();
        setTimeout(function() {
          m.stopTimer("timer1");
          setTimeout(function() {
            m.stopTimer("timer2");
            m.close();
            assert.counter(emittedMetricEvent.counters.hello.getValues()[0], helloCounterValue +
              1, "increment counter happy case failed");
            assert.counter(emittedMetricEvent.counters.hello.getValues()[1], 1,
              "increment counter happy case failed");

            assert.timer(emittedMetricEvent.timers.timer1.getValues()[0], 750,
              "log.timers.timer1.getValues()[0] >= 750");
            assert.timer(emittedMetricEvent.timers.timer1.getValues()[1], 850,
              "log.timers.timer1.getValues()[1] >= 850");
            assert.timer(emittedMetricEvent.timers.timer2.getValues()[0], 950,
              "log.timers.timer1.getValues()[0] >= 950");
            done();
          }, 100)
        }, 100)
      }, 750);
    });

    it('should work as expected in real case', function(done) {
      var m = createMetrics();
      var opsCounter = 10;
      var iterations = opsCounter;
      var durations = [];
      var counts = [];

      for (var i = 0; i < iterations; ++i) {
        var timer = m.createTimer("timeit");
        durations.push(Math.random() * 1000);
        setTimeout(function(t) {
          t.stop();;
          --opsCounter;
          if (opsCounter == 0) {
            m.close();
            for (var k = 0; k < iterations; ++k) {
              assert.counter(emittedMetricEvent.counters.countit.getValues()[k], counts[k],
                "increment counter happy case failed");
              assert.timer(emittedMetricEvent.timers.timeit.getValues()[k], durations[k],
                "log.timers.timeit.getValues()['" + k + "'] >= " + durations[k]);
            }
            done();

          }
        }, durations[i], timer);

        var counter = m.createCounter("countit");
        counts.push(Math.floor(Math.random() * 10));
        for (var j = 0; j < counts[i]; ++j) {
          counter.increment();
        }
      }
    });
  });

  describe('Metrics with units', function() {
    it('should serialize gauges and explicit timers with correct units', function(done) {
      var m = createMetrics();
      m.setGauge("g1", 1, tsd.Units.GIGABYTE);
      m.setTimer("t1", 1123, tsd.Units.WEEK);

      m.close();
      assert.gauge(emittedMetricEvent.gauges.g1.getValues()[0], 1, "gigabyte",
        "gauge g1's unit or value incorrect");
      assert.explicitTimer(emittedMetricEvent.timers.t1.getValues()[0], 1123, "week",
        "gauge t1's unit or value incorrect");
      done();
    });
  });
});
