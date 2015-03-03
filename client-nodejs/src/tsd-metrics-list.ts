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

/**
 * Class for holding a list of samples.
 *
 * @class
 * @alias MetricsList
 */
export class TsdMetricsList<T extends tsdDef.MetricSample> implements tsdDef.MetricsList <T> {

    /**
     * An array holding metrics samples.
     *
     * @member {MetricsSample[]} values
     * @memberof MetricsList
     * @instance
     */
    private _values:T[] = [];

    /**
     * An array holding metrics samples.
     *
     * @method
     * @returns {T[]}
     */
    public getValues():T[] {
        return this._values

    }

    /**
     * Push a value to the list.
     *
     * @method
     * @param {MetricSample} value The value to be pushed
     * @ignore
     */
    public push(value:T):void {
        this._values.push(value);
    }

    /**
     * Creates a new MetricsList with all elements that pass the test implemented by the provided predicate.
     *
     * @method
     * @param predicate The function to test if element should be taken.
     * @return {MetricsList<MetricSample>} new MetricsList list containing only the items matching the filter
     * predicate
     */
    public filter(predicate:(T) => boolean):TsdMetricsList<T> {
        var ret = new TsdMetricsList<T>();
        ret._values = this._values.filter(predicate);
        return ret;
    }
}
