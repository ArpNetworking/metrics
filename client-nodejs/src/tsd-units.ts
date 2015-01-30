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

class UnitImpl implements tsdDef.Unit {

    constructor(public name:string) {
    }

    public toJSON() {
        return this.name;
    }
}

/**
 * Units available for recording metrics. The units are used to aggregate values
 * of the same metric published in different units (e.g. bytes and kilobytes).
 * Publishing a metric with units from different domains will cause some of the
 * data to be discarded by the aggregator (e.g. bytes and seconds). This
 * includes discarding data when some data has a unit and some data does not
 * have any unit. This library cannot detect such inconsistencies since
 * aggregation can occur across Metric instances, processes and even hosts.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 * @enum
 */
export var Units = {
    // Time
    /**
     * Nanoseconds.
     */
    NANOSECOND: <tsdDef.Unit>new UnitImpl("nanosecond"),

    /**
     * Microseconds.
     */
    MICROSECOND: <tsdDef.Unit>new UnitImpl("microsecond"),

    /**
     * Milliseconds.
     */
    MILLISECOND: <tsdDef.Unit>new UnitImpl("millisecond"),

    /**
     * Seconds.
     */
    SECOND: <tsdDef.Unit>new UnitImpl("second"),

    /**
     * Minutes.
     */
    MINUTE: <tsdDef.Unit>new UnitImpl("minute"),

    /**
     * Hours.
     */
    HOUR: <tsdDef.Unit>new UnitImpl("hour"),

    /**
     * Days.
     */
    DAY: <tsdDef.Unit>new UnitImpl("day"),

    /**
     * Weeks.
     */
    WEEK: <tsdDef.Unit>new UnitImpl("week"),

    // Size
    /**
     * Bytes.
     */
    BYTE: <tsdDef.Unit>new UnitImpl("byte"),

    /**
     * Kilobytes.
     */
    KILOBYTE: <tsdDef.Unit>new UnitImpl("kilobyte"),

    /**
     * Megabytes.
     */
    MEGABYTE: <tsdDef.Unit>new UnitImpl("megabyte"),

    /**
     * Gigabytes.
     */
    GIGABYTE: <tsdDef.Unit>new UnitImpl("gigabyte")
};
