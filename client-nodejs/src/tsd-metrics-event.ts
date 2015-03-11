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

import utils = require("./utils");
import timers = require("./tsd-timer");
import counters = require("./tsd-counter");
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
 * This class holds all metrics details captured for a metric event and ready to be serialized. An instance is passed
 * to the [Sink.record()]{@linkcode Sink#record} method in every sink, for the [Sink]{@linkcode Sink} to
 * serialize the event in its own format and to its own medium.
 *
 * @class
 * @alias MetricsEvent
 * @author Mohammed Kamel (mkamel at groupon dot com)
 **/
export class TsdMetricsEvent implements tsdDef.MetricsEvent {

    /**
     * The annotations represented as hash of arrays indexed by annotation name.
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, string>}
     */
    public annotations:{[name:string]: string} = {};

    /**
     * Counters and their samples recorded represented as hash of counter name to
     * [MetricSample]{@linkcode MetricSample}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, MetricsList<MetricSample>>}
     */
    public counters:{[name:string]: tsdDef.MetricsList<tsdDef.MetricSample>} = {};

    /**
     * Gauges and their samples recorded represented as hash of counter name to
     * [MetricSample]{@linkcode MetricSample}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, MetricsList<MetricSample>>}
     */
    public gauges:{[name:string]: tsdDef.MetricsList<tsdDef.MetricSample>} = {};

    /**
     * Timers and their samples recorded represented as hash of counter name to
     * [Timer]{@linkcode Timer}
     *
     * @memberof! MetricsEvent#
     * @type {Object.<string, MetricsList<Timer>>}
     */
    public timers:{[name:string]: tsdDef.MetricsList<tsdDef.Timer>} = {};
}
