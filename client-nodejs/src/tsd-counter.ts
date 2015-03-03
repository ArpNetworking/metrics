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
import tsd = require("tsd-metrics-client");
import sample = require("./tsd-metric-sample");

//aliases
import TsdMetricSample = sample.TsdMetricSample;

/**
 * Class for counter. Instances are initialized to zero on creation. The zero-value sample is recorded when the
 * [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} instance is closed if no other actions are taken on the
 * <code>Counter</code>.
 *
 * Modifying the <code>Counter</code> instance's value modifies the single sample value. When the associated
 * [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} instance is closed whatever value the sample has is
 * recorded. To create another sample you create a new <code>Counter</code> instance with the same name.
 *
 * Each Counter instance is bound to a [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} instance. After the
 * [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} instance is closed any modifications to the
 * <code>Counter</code> instances bound to that [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} instance
 * will be ignored.
 *
 * @class
 * @extends MetricSample
 * @alias Counter
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */
export class TsdCounter extends TsdMetricSample implements tsdDef.Counter {

    /**
     * Constructor.
     *
     * @param {MetricsStateObject} _metricsStateObject Object holding state of the parent metrics object.
     * @ignore
     */
    constructor(private _name:string, private _metricsStateObject:tsd.MetricsStateObject) {
        super(0, undefined);
    }

    /**
     * Increment the counter sample by the specified value.
     *
     * @method
     * @param {number} value The value to increment the counter by. Default = 1
     * @emits 'error' if the metrics object is closed
     */
    public increment(value:number = 1):void {
        if (this._metricsStateObject.assertIsOpen("Cannot increment/decrement counter '" + this._name + "'")) {
            (<any>this)._value = (<any>this)._value + value;
        }
    }

    /**
     * Decrement the counter sample by the specified value.
     *
     * @method
     * @param {number} value The value to decrement the counter by. Default = 1
     * @emits 'error' if the metrics object is closed
     */
    public decrement(value:number = 1):void {
        this.increment(-1 * value);
    }
}
