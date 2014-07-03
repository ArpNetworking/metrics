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

import tsdDef = require("tsdDef");
import tsd = require("tsd-metrics-client");
import sample = require("./tsd-metric-sample");

//aliases
import TsdMetricSample = sample.TsdMetricSample;

export class TsdCounter extends TsdMetricSample implements tsdDef.Counter {
    private isOpen:boolean = true;

    constructor(private metricsStateObject:tsd.MetricsStateObject) {
        super(0, undefined);
    }

    /**
     * Increment the counter sample by the specified value.
     *
     * @param value The value to increment the counter by. Default = 1
     */
    public increment(value:number = 1):void {
        if(this.metricsStateObject.assertIsOpen("Cannot increment/decrement counter")) {
            (<any>this).value = (<any>this).value + value;
        }
    }

    /**
     * Decrement the counter sample by the specified value.
     *
     * @param value The value to decrement the counter by. Default = 1
     */
    public decrement(value:number = 1):void {
        this.increment(-1 * value);
    }
}
