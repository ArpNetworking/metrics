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

///<reference path='defs/metrics.d.ts'/>
import tsdDef = require("tsdDef");
import tsd = require("tsd-metrics-client");
import tsdUtils = require("./utils");

export class TsdMetricSample implements tsdDef.MetricSample {
    /**
     * Static factory
     *
     * @param value The magnitude of the sample.
     * @param metrics The unit that the magnitude is expressed in. Default = undefined (i.e. has not unit).
     */
    public static of(value:number, unit:tsdDef.Unit = undefined): tsdDef.MetricSample{
        return new TsdMetricSample(value, unit);
    }

    /**
      * Constructor.
      *
      * @param value The magnitude of the sample.
      * @param metrics The unit that the magnitude is expressed in.
      */
     constructor(private value:number, private unit:tsdDef.Unit) {
     }

    /**
     * Access the magnitude of the sample.
     *
     * @return The magnitude.
     */
    getValue(): number {
        return this.value;
    }

    /**
     * Access the units that the magnitude is expressed in.
     *
     * @return The units of the magnitude.
     */
    getUnit(): tsdDef.Unit {
        return this.unit;
    }

    public toJSON() {
        return { value: this.value, unit: this.unit }
    }
}
