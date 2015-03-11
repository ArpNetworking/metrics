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
/* istanbul ignore next */ //ignores inheritance snippet emitted by the TS compiler

import tsdDef = require("tsdDef");
import timers = require("./tsd-timer");
import utils = require("./utils");
import tsd = require("tsd-metrics-client");
import sample = require("./tsd-metric-sample");
import metricsList = require("./tsd-metrics-list");

//aliases
import TsdTimer = timers.TsdTimer;
import ExplicitTimer = timers.ExplicitTimer;
import MetricsStateObject = tsd.MetricsStateObject;
import TsdMetricSample = sample.TsdMetricSample;
import TsdMetricsList = metricsList.TsdMetricsList;

/**
 * Class for creating duration sample for a timer.
 *
 * @class
 * @alias TimerSamples
 * @ignore
 */
export class TimerSamples extends TsdMetricsList<tsdDef.MetricSample> {

    /**
     * Constructor.
     *
     * @param {string} _name Name of the timer.
     * @param {MetricsStateObject} _metricsStateObject Object holding state of the parent metrics object.
     */
    public constructor(private _name:string, private _metricsStateObject:MetricsStateObject) {
        super();
    }

    /**
     * Create a new timer sample.
     *
     * @method
     * @return {Timer} The new timer sample instance added to the samples.
     */
    public addTimer():tsdDef.Timer {
        var tsdTimer = new TsdTimer(this._name, this._metricsStateObject);
        this.push(tsdTimer);
        return tsdTimer;
    }

    /**
     * Create a timer sample with explicit value.
     *
     * @method
     * @param {number} duration The duration to be recorded.
     * @param {Units} unit The unit of the duration.
     */
    public addExplicitTimer(duration:number, unit:tsdDef.Unit):void {
        this.push(new ExplicitTimer(duration, unit, this._name, this._metricsStateObject));
    }
}
