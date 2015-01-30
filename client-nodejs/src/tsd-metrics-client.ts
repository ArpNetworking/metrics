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

import tsdDef = require("tsdDef");
import log4js = require("log4js");
import events = require("events");

import timers = require("./tsd-timer");
import counters = require("./tsd-counter");
import utils = require("./utils");
import timerSamples = require("./timer-samples");
import counterSamples = require("./counter-samples");
import units = require("./tsd-units");
import sample = require("./tsd-metric-sample");
import metricsList = require("./tsd-metrics-list");
import logEntry = require("./metrics-event");

//aliases
import TsdTimer = timers.TsdTimer;
import TsdCounter = counters.TsdCounter;
import Lazy = utils.Lazy;
import CounterSamples = counterSamples.CounterSamples;
import TimerSamples = timerSamples.TimerSamples;
import Units = units.Units;
import TsdMetricSample = sample.TsdMetricSample;
import TsdMetricsList = metricsList.TsdMetricsList;
import MetricsEvent = logEntry.MetricsEvent;

/**
 * The tsd module configuration parameters.
 * @ignore
 */
class Options {
    /* istanbul ignore next */
    constructor() {
    }

    /**
     * Sets the maximums size of log in bytes before rolling a new file.
     * Default: 32 MB
     * @type {number}
     */
    public static LOG_MAX_SIZE:number = 32 * 1024 * 1024;

    /**
     * Sets the maximum number of log files backup to retain.
     * Default: 10
     * @type {number}
     */
    public static LOG_BACKUPS:number = 10;

    /**
     * The name of the query log file
     * Default: "tsd-query.log"
     * @type {string}
     */
    public static LOG_FILE_NAME:string = "tsd-query.log";

    /**
     * Sets a flag to output the metrics to console in addition to the query file (mainly for testing).
     * Default: false
     * @type {boolean}
     */
    public static LOG_CONSOLE_ECHO:boolean = false;
}

/**
 * This class allows holds the open state of the metrics object and the event emitter used for logging error and test
 * event.
 * The motive for this class is to avoid passing TsdMetrics instance to timers and counters, and create a
 * circular reference that could be expensive to collect by the GC.
 * The timers and counter needs to check the open state of the TsdMetrics instance and report error if an operation
 * is executed while the TsdMetrics instance is in closed state.
 *
 * @class
 * @ignore
 **/
export class MetricsStateObject {
    private eventEmitter:events.EventEmitter = new events.EventEmitter();

    constructor(public isOpen:boolean) {
        //according to http://nodejs.org/docs/v0.3.5/api/events.html#events.EventEmitter
        //if an error event is emitted and nothing was listening, the process will exist, so we are adding this
        //do-nothing listener in order not to exit the process
        this.eventEmitter.addListener("error", (error)=> {
        });
    }

    /**
     * Get an instance if the event emitter to be used to emit the metrics events.
     */
    public getEventEmitter():events.EventEmitter {
        return this.eventEmitter;
    }

    /**
     * Assert that the metrics object is not closed.
     *
     * @method
     * @param message An optional message to prefix to error message if object is closed.
     * @returns {boolean} True if the metric object is open, otherwise false.
     */
    public assertIsOpen(failMessage:string = ""):boolean {
        return this.assert(this.isOpen,
                (failMessage == "" ? failMessage : failMessage + ": ") +
                "Metrics object was not opened or it's already closed");
    }

    /**
     * Assert that a condition is true and emit an error event if not.
     *
     * @method
     * @param condition The condition to assert.
     * @param message The message to set to the error if condition not met.
     * @returns {boolean}
     */
    public assert(condition:boolean, message:string):boolean {
        if (!condition) {
            this.eventEmitter.emit("error", new Error(message));
        }
        return condition;
    }

    /**
     * Assert that an object is defined and its value is also defined.
     *
     * @method
     * @param object The object to test.
     * @param message The message to set to the error if condition not met.
     * @returns {boolean} True of the object is defined, otherwise false.
     */
    public assertDefined(object:any, message:string):boolean {
        return this.assert(typeof object !== "undefined", message);
    }

    /**
     * Assert that an object is not defined or its value is undefined.
     *
     * @method
     * @param object The object to test.
     * @param message The message to set to the error if condition not met.
     * @returns {boolean} True of the object is undefined, otherwise false.
     */
    public assertUndefined(object:any, message:string):boolean {
        return this.assert(typeof object === "undefined", message);
    }
}

/**
 * 'error' event. Emitted on errors or improper usage of API's
 *
 * @event TsdMetrics#error
 * @type {Error}
 */
/**
 * 'logEvent' event. Emitted after successful writing of the metric event to the query log file
 *
 * @event TsdMetrics.events()#logEvent
 * @type {object}
 * @property {Array.<{Object.<string, string>}>} annotations Array of hash annotations.
 * @property {Object.<string, number[]>} counters Array of {"counter" : [samples]} hashes.
 * @property {Object.<string, number[]>} timers Array of {"timer" : [samples]} hashes.
 * @property {Object.<string, number[]>} gauges Array of {"gauge" : [samples]} hashes.
 */
/**
 * Node JS class for publishing metrics as time series data (TSD).
 *
 * @class
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */
export class TsdMetrics implements tsdDef.Metrics {

    /**
     * Singleton instance of the logger.
     */
    private static LOGGER:Lazy<log4js.Logger> =
        new Lazy<log4js.Logger>(() => {
            var appendersArray:any[] = [
                {
                    type: "file",
                    filename: Options.LOG_FILE_NAME,
                    maxLogSize: Options.LOG_MAX_SIZE,
                    backups: Options.LOG_BACKUPS,
                    layout: {
                        type: "pattern",
                        pattern: "%m"
                    },
                    category: "tsd-client"
                }
            ];
            /* istanbul ignore next */
            if (Options.LOG_CONSOLE_ECHO) {
                appendersArray.push(
                    {
                        type: "console",
                        layout: {
                            type: "pattern",
                            pattern: "%m"
                        },
                        category: "tsd-client"
                    }
                );
            }
            var config = {
                appenders: appendersArray
            };

            log4js.configure(config, {});

            return log4js.getLogger("tsd-client");
        });

    /**
     * Metrics state object that holds flag to the metrics open state along with common event emitter.
     */
    private _metricsStateObject:MetricsStateObject = new MetricsStateObject(true);
    private _timers:{[name:string]: tsdDef.Timer} = {};
    private _counters:{[name:string]: tsdDef.Counter} = {};

    /**
     * MetricsEvent field that will be used to serialize the final metric event.
     */
    private _metricsEvent:MetricsEvent = new MetricsEvent();

    private static getOrCreateMetricsCollection<T>(name:string, collectionOfT:{[name:string]:T}, factory:() => T) {
        if (collectionOfT[name] === undefined) {
            collectionOfT[name] = factory();
        }
        return collectionOfT[name];
    }

    private getOrCreateCounterSamples(name:string):CounterSamples {
        return <CounterSamples>TsdMetrics.getOrCreateMetricsCollection(
            name, this._metricsEvent.counters, () => new CounterSamples(this._metricsStateObject));
    }

    private getOrCreateTimerSamples(name:string):TimerSamples {
        return <TimerSamples>TsdMetrics.getOrCreateMetricsCollection(
            name, this._metricsEvent.timers, () => new TimerSamples(name, this._metricsStateObject));
    }

    private getOrCreateGaugesSamples(name:string):TsdMetricsList < tsdDef.MetricSample > {
        return TsdMetrics.getOrCreateMetricsCollection(
            name, this._metricsEvent.gauges, () => new TsdMetricsList < tsdDef.MetricSample >());
    }

    /**
     * Constructor.
     */
    constructor() {
        this.annotate("initTimestamp", new Date().toISOString());
    }

    /**
     * Get the event emitter to be used for emitting events and errors.
     *
     * @method
     * @returns {EventEmitter} Event emitter instance
     */
    public events():events.EventEmitter {
        return this._metricsStateObject.getEventEmitter();
    }

    /**
     * Create and initialize a counter sample. It is valid to create multiple
     * <code>Counter</code> instances with the same name, even concurrently,
     * each will record a unique sample for the counter of the specified name.
     *
     * @method
     * @param {string} name The name of the counter.
     * @returns {Counter} TsdCounter instance for recording a counter sample.
     */
    public createCounter(name:string):tsdDef.Counter {
        if (this._metricsStateObject.assertIsOpen()) {
            return this.getOrCreateCounterSamples(name).addCounter();
        }
        return new TsdCounter(this._metricsStateObject);
    }

    /**
     * Increment the specified counter by the specified amount. All counters are
     * initialized to zero.
     *
     * @method
     * @param {string} name The name of the counter.
     * @param {number} value The amount to increment by.
     * @emits 'error' if the metrics object is closed
     */
    public incrementCounter(name:string, value:number = 1):void {
        if (this._metricsStateObject.assertIsOpen()) {
            if (this._counters[name] === undefined) {
                this._counters[name] = this.createCounter(name);
            }
            this._counters[name].increment(value);
        }
    }

    /**
     * Decrement the specified counter by the specified amount. All counters are
     * initialized to zero.
     *
     * @method
     * @param {string} name The name of the counter.
     * @param {number} value The amount to decrement by.
     * @emits 'error' if the metrics object is closed
     */
    public decrementCounter(name:string, value:number = 1) {
        this.incrementCounter(name, -value);
    }

    /**
     * Reset the counter to zero. This most commonly used to record a zero-count
     * for a particular counter. If clients wish to record set count metrics
     * then all counters should be reset before conditionally invoking increment
     * and/or decrement.
     *
     * @method
     * @param {string} name The name of the counter.
     */
    public resetCounter(name:string):void {
        if (this._metricsStateObject.assertIsOpen()) {
            this._counters[name] = this.createCounter(name);
        }
    }

    /**
     * Create and start a timer. It is valid to create multiple <code>Timer</code>
     * instances with the same name, even concurrently, each will record a
     * unique sample for the timer of the specified name.
     *
     * @method
     * @param name The name of the timer.
     * @returns {Timer} TsdTimer instance for recording a timing sample.
     */
    public createTimer(name:string):tsdDef.Timer {
        if (this._metricsStateObject.assertIsOpen()) {
            return this.getOrCreateTimerSamples(name).addTimer();
        }
        return new TsdTimer(name, this._metricsStateObject);
    }

    /**
     * Start the specified timer measurement.
     *
     * @method
     * @param {string} name The name of the timer.
     * @emits 'error' if the metrics object is closed or if the timer already started
     */
    public startTimer(name:string):void {
        if (this._metricsStateObject.assertIsOpen() &&
            this._metricsStateObject.assertUndefined(this._timers[name],
                    "Timer '" + name + "' already started; to time the same event concurrently use createTimer")) {
            this._timers[name] = this.createTimer(name);
        }
    }

    /**
     * Stop the specified timer measurement.
     *
     * @method
     * @param {string} name The name of the timer.
     * @emits 'error' if the metrics object is closed or if the timer already stopped.
     */
    public stopTimer(name:string):void {
        if (this._metricsStateObject.assertIsOpen() &&
            this._metricsStateObject.assertDefined(this._timers[name],
                    "Cannot stop timer '" + name + "'. No samples have currently started for the timer")) {
            this._timers[name].stop();
            this._timers[name] = undefined;
        }
    }

    /**
     * Set the timer to the specified value. This is most commonly used to
     * record timers from external sources that are not integrated with metrics.
     *
     * @method
     * @param {string} name The name of the timer.
     * @param {number} duration The duration of the timer.
     * @param {Units} unit The unit of time of the duration.
     * @emits 'error' if the metrics object is closed or if not unit specified
     */
    public setTimer(name:string, duration:number, unit:tsdDef.Unit) {
        if (this._metricsStateObject.assertIsOpen()) {
            if (!this._metricsStateObject.assertDefined(unit,
                "No unit specified for explicit timer. Assuming millisecond.")) {
                unit = Units.MILLISECOND;
            }
            this.getOrCreateTimerSamples(name).addExplicitTimer(duration, unit);
        }
    }

    /**
     * Set the specified gauge reading.
     *
     * @method
     * @param {string} name  The name of the gauge.
     * @param {number} value The reading on the gauge
     * @param {Units} unit The unit of the reading is recorded into. Default = undefined (no unit).
     * @emits 'error' if the metrics object is closed
     */
    public setGauge(name:string, value:number, unit:tsdDef.Unit = undefined) {
        if (this._metricsStateObject.assertIsOpen()) {
            this.getOrCreateGaugesSamples(name).push(TsdMetricSample.of(value, unit));
        }
    }

    /**
     * Add an attribute that describes the captured metrics or context.
     *
     * @method
     * @param {string} key The name of the attribute.
     * @param {string} value The value of the attribute.
     * @emits 'error' if the metrics object is closed
     */
    public annotate(key:string, value:string) {
        if (this._metricsStateObject.assertIsOpen()) {
            this._metricsEvent.annotations[key] = value;
        }
    }

    /**
     * Close the metrics object. This should complete publication of metrics to
     * the underlying data store. Once the metrics object is closed, no further
     * metrics can be recorded.
     *
     * @method
     * @emits 'error' if the metrics object is already closed
     */
    public close() {
        if (this._metricsStateObject.assertIsOpen()) {
            this.annotate("finalTimestamp", new Date().toISOString());
            this._metricsStateObject.isOpen = false;
            var stenoMetricsEvent = utils.stenofy(this._metricsEvent);

            var emittedLogEvent = false;
            try {
                var metricsEventString = JSON.stringify(stenoMetricsEvent);
                TsdMetrics.LOGGER.getValue().info(metricsEventString);
                emittedLogEvent = true;
            }
            catch (err) {
                this._metricsStateObject.getEventEmitter().emit("error", err);
            }

            if (emittedLogEvent) {
                this._metricsStateObject.getEventEmitter().emit("logEvent", this._metricsEvent, stenoMetricsEvent);
            }
        }
    }

    /**
     * Accessor to determine if this <code>Metrics</code> instance is open or
     * closed. Once closed an instance will not record new data.
     *
     * @method
     * @return True if and only if this <code>Metrics</code> instance is open.
     */
    isOpen(): boolean {
        return this._metricsStateObject.isOpen;
    }
}
/**
 * Main modules for TSD Client
 * @module tsd-metrics-client
 */

/**
 * The tsd-metrics-client module configuration parameters. See {@link module:tsd-metrics-client}
 * @typedef {Object} options
 * @property {number} LOG_MAX_SIZE - Sets the maximum number of log files backup to retain. Default: 33554432 (32 MB)
 * @property {number} LOG_BACKUPS - Sets the maximum number of log files backup to retain. Default: 10
 * @property {string} LOG_FILE_NAME - The name of the query log file. Default: "tsd-query.log"
 * @property {string} LOG_CONSOLE_ECHO - Sets a flag to output the metrics to console in addition to the query file.
 Default: false
 */

/**
 * Set the global configuration of the tsd module. It should be specified once per application.
 * @param {options} [options] A hash that specifies the configuration parameters
 * @alias module:tsd-metrics-client
 */
module.exports = function (options:any = {}) {

    if (typeof options.LOG_MAX_SIZE !== "undefined") {
        Options.LOG_MAX_SIZE = options.LOG_MAX_SIZE;
    }

    if (typeof options.LOG_BACKUPS !== "undefined") {
        Options.LOG_BACKUPS = options.LOG_BACKUPS;
    }

    if (typeof options.LOG_FILE_NAME !== "undefined") {
        Options.LOG_FILE_NAME = options.LOG_FILE_NAME;
    }

    if (typeof options.LOG_CONSOLE_ECHO !== "undefined") {
        Options.LOG_CONSOLE_ECHO = options.LOG_CONSOLE_ECHO;
    }

    return module.exports;
};

module.exports.TsdMetrics = TsdMetrics;
module.exports.Units = Units;
