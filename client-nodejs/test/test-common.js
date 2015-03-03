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

var colors = require("colors");

// To enable verbosity, execute '$ VERBOSE=true mocha'
var hasVerboseArg = false;
process.argv.forEach(function (item, index) {
    if (item.toLowerCase().indexOf("--verbose") == 0) {
        hasVerboseArg = true;
    }
});

var verbose = hasVerboseArg ? true
    : typeof(process.env.VERBOSE) != undefined ? process.env.VERBOSE === "true"
    : false;


var tsd = require("../lib/tsd-metrics-client");
var fs = require("fs");

if (fs.existsSync(tsd.TsdMetrics.LOG_FILE_NAME)) {
    fs.unlinkSync(tsd.TsdMetrics.LOG_FILE_NAME);
}

var assert = require("chai").assert;

//inject metrics assertions
assert.timerValue = function assertTimerValue(timerValue, expectedMilliseconds, message) {
    assert.operator(Math.ceil(timerValue / 1000000), '>=', expectedMilliseconds, message);
    assert.operator(Math.ceil(timerValue / 1000000), '<=', expectedMilliseconds * 2, message);
};

assert.timer = function assertTimer(timerObject, expectedMilliseconds, message) {
    assert.timerValue(timerObject.getValue(), expectedMilliseconds, message);
    assert.equal(timerObject.getUnit().name, "nanosecond", "Non explicit timer unit is not 'nanosecond'");
};

assert.explicitTimer = function assertExplicitDuration(durationObject, expectedValue, expectedUnit, message) {
    assert.equal(durationObject.getValue(), expectedValue, message);
    assert.equal(durationObject.getUnit().name, expectedUnit, message);
};

assert.counter = function assertCounter(counterObject, expectedValue, message) {
    assert.equal(counterObject.getValue(), expectedValue, message);
    assert.equal(counterObject.getUnit(), undefined, "counter has a unit, while it shouldn't");
};

assert.gauge = function assertGauge(gaugeObject, expectedValue, expectedUnit, message) {
    assert.equal(gaugeObject.getValue(), expectedValue, message);
    assert.equal(gaugeObject.getUnit().name, expectedUnit, message);
};

assert.gaugeDefault = function assertGaugeDefault(gaugeObject, expectedValue, message) {
    assertGauge(gaugeObject, expectedValue, undefined, message)
};

function print(message) {
    if (verbose) {
        if (Object.keys(arguments).length === 1) {
            console.log(message.yellow);
        } else {
            var args = arguments;
            delete args[0];
            console.log(message.yellow, args);
        }
    }
}

function printError(message) {
    if (verbose) {
        if (Object.keys(arguments).length === 1) {
            console.log(message.red);
        } else {
            var args = arguments;
            delete args[0];
            console.log(message.red, args);
        }
    }
}

module.exports.print = print;
module.exports.printError = printError;
module.exports.verbose = verbose;
