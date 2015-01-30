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

import timers = require("./tsd-timer");
import counters = require("./tsd-counter");
import utils = require("./utils");
import timerSamples = require("./timer-samples");
import counterSamples = require("./counter-samples");
import sample = require("./tsd-metric-sample");
import metricsList = require("./tsd-metrics-list");

//aliases
import TsdTimer = timers.TsdTimer;
import TsdCounter = counters.TsdCounter;
import CounterSamples = counterSamples.CounterSamples;
import TimerSamples = timerSamples.TimerSamples;
import TsdMetricSample = sample.TsdMetricSample;
import TsdMetricsList = metricsList.TsdMetricsList;
import Lazy = utils.Lazy;

/**
 * This class holds all all the metrics to be recorded by a <code>Sink</code>. The instance is passed to
 * <code>Sink#record()</code> method
 *
 * @class
 * @alias MetricsEvent
 * @author Mohammed Kamel (mkamel at groupon dot com)
 **/
export class MetricsEvent implements tsdDef.MetricsEvent {

    /**
     * The annotations represented as hash of arrays indexed by annotation name.
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, TsdMetricsList>}
     */
    public annotations:{[name:string]: string} = {};

    /**
     * Counters and their samples recorded represented as hash of counter name to
     * [TsdMetricSample]{@linkcode TsdMetricSample}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, TsdMetricsList>}
     */
    public counters:{[name:string]: TsdMetricsList<tsdDef.MetricSample>} = {};

    /**
     * Gauges and their samples recorded represented as hash of counter name to
     * [TsdMetricSample]{@linkcode TsdMetricSample}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, TsdMetricsList>}
     */
    public gauges:{[name:string]: TsdMetricsList<tsdDef.MetricSample>} = {};

    /**
     * Timers and their samples recorded represented as hash of counter name to
     * [TsdMetricSample]{@linkcode TsdMetricSample}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, TsdMetricsList>}
     */
    public timers:{[name:string]: TsdMetricsList<tsdDef.MetricSample>} = {};

    private static _VERSION:string = "2e";
    private _hash:Lazy<any> = new Lazy<any>(()=> {
        var hash:any = {
            annotations: this.annotations
        };

        if (!utils.isEmptyObject(this.counters)) {
            hash.counters = this.counters;
        }

        if (!utils.isEmptyObject(this.gauges)) {
            hash.gauges = this.gauges;
        }

        if (!utils.isEmptyObject(this.timers)) {
            hash.timers = this.timers;
        }

        hash.version = MetricsEvent._VERSION;
        return hash;
    });

    public toJSON() {
        return this._hash.getValue();
    }
}
