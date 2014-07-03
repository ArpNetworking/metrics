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
/* istanbul ignore next */ //ignores inhertience snippet emitted by the TS compiler

///<reference path='defs/metrics.d.ts'/>
import tsdDef = require("tsdDef");
import tsd = require("tsd-metrics-client");
import tsdUtils = require("./utils");
import sample = require("./tsd-metric-sample");
import units = require("./tsd-units");

//aliases
import TsdMetricSample = sample.TsdMetricSample;
import Units = units.Units;

export class TsdTimer extends TsdMetricSample implements tsdDef.Timer {

    private isStoppedFlag:boolean = false;
    private startTime:number = tsdUtils.getMilliTime();

    /**
     * Constructor.
    *
     * @param name name of the timer
     * @param metrics metrics object the timer belongs to
     */
    constructor(private name:string, private metricsStateObject:tsd.MetricsStateObject) {
        super(0, Units.NANOSECOND)
    }

    /**
     * Return if the timer was stopped already.
     */
    public isStopped(): boolean {
        return this.isStoppedFlag;
    }

    /**
     * Stop the timer and record timing data in the associated.
     */
    public stop():void {
        var canStop =
            this.metricsStateObject.assertIsOpen("Cannot stop timer '" + this.name + "'") &&
            this.metricsStateObject.assert(!this.isStopped(), "Timer '" + this.name + "' stopped multiple times")

        if(canStop) {
            this.isStoppedFlag = true;
            (<any>this).value = tsdUtils.getNanoTime() - this.startTime;
        }
    }
}

export class ExplicitTimer extends TsdMetricSample implements tsdDef.Timer {

    /**
     * Constructor.
     *
     * @param name name of the timer
     * @param metrics metrics object the timer belongs to
     */
    constructor(value:number, unit:tsdDef.Unit, private name:string, private metricsStateObject:tsd.MetricsStateObject) {
        super(value, unit);
    }

    /**
     * Do nothing.
     */
     /* istanbul ignore next */
    public stop():void {
        //Do nothing
        this.metricsStateObject.assert(false, "cannot stop and explicitly set timer");
    }

    /**
     * Always is stopped.
     */
    public isStopped(): boolean {
        return true;
    }
}
