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
import metricsSample = require("../tsd-metric-sample");
import metricsList = require("../tsd-metrics-list");
/* istanbul ignore next */ //this is basically a skeleton that gets extended by TsdSink classes
/**
 * Base class for sinks, which represents a destination to record metrics to. Derived classes can be passed to
 * [tsd.setSink] {@link module:tsd-metrics-client#init} method.
 *
 * @example
 * var tsd = require("tsd-metrics-client");
 * var util = require("util");
 * function MySink() {
 * }
 * util.inherits(MySink, tsd.Sink);
 * MySink.prototype.record = function (metricsEvent) {
 *      console.log(metricsEvent);
 * };
 * var mySink =  new MySink();
 * tsd.init([mySink, tsd.Sinks.createQueryLogSink()])
 *
 * @class
 * @alias Sink
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */
export class TsdSink implements tsdDef.Sink {
    /**
     * Callback passed to [Sink.record]{@linkcode Sink#record} to be used by sink implementations to report errors.
     * @callback ErrorReporterCallback
     * @param {string} errorMessage Message to be reported in the metrics error events.
     */

    /**
     * Invoked by [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} to record data to this
     * [TsdSink]{@linkcode TsdSink}.
     *
     * @abstract
     * @param {MetricsEvent} metricsEvent logEntry to be recorded by the sync.
     * @param {ErrorReporterCallback} errorReporter Callback to be used to report errors during record.
     */
    record(metricsEvent:tsdDef.MetricsEvent):void {
        throw new Error('must be implemented by subclass!');
    }

    /**
     * Static helper to check if an object is a MetricSample.
     *
     * @param object
     * @returns {boolean}
     */
    public static isMetricSample(object:any):boolean {
        return object instanceof metricsSample.TsdMetricSample;
    }


    /**
     * Static helper to check if an object is a MetricsList.
     *
     * @param object
     * @returns {boolean}
     */
    public static isMetricsList(object:any):boolean {
        return object instanceof metricsList.TsdMetricsList;
    }
}