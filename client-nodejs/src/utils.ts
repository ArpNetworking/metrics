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
import uuid = require('node-uuid');

/**
 * Get the current time in nanoseconds.
 *
 * @ignore
 * @returns {number} value of high resolution real time in nanosecond.
 */
export function getNanoTime() {
    var time = process.hrtime();
    return time[0] * 1e9 + time[1];
}

/**
 * Get the current time in milliseconds.
 *
 * @ignore
 * @returns {number} current time in millisecond.
 */
export function getMilliTime() {
    return getNanoTime() / 1e6;
}

/**
 * Determines if an object has not properties defined.
 *
 * @ignore
 * @param {object} obj The object to tested.
 */
export function isEmptyObject(obj) {
    return !Object.keys(obj).length;
}

/**
 * Wraps a MetricsEvent object into a Steno envelope.
 *
 * @param {MetricsEvent} logEntry log entry to be enveloped.
 * @returns {{time: string, name: string, level: string, data: tsdDef.MetricsEvent, id: string, context: {}}} the log entry
 * in Steno envelope
 * @ignore
 */
export function stenofy(logEntry:tsdDef.MetricsEvent) {
    var buffer = new Buffer(16);
    uuid.v4(null, buffer, 0);

    return {
        "time": new Date().toISOString(),
        "name": "aint.metrics",
        "level": "info",
        "data": logEntry,
        "id": buffer.toString("base64"),
        "context": {}
    };
}

/* istanbul ignore next */ //The class is not currently used.
/**
 * Wrapper class for lazy initialized values.
 *
 * @class
 * @alias Lazy
 * @ignore
 */
export class Lazy<T> {
    private _factory:()=>T;
    private _value:T;

    /**
     * Constructor.
     *
     * @param {function} factory Factory function to be used to initialize the value.
     */
    constructor(factory:()=>T) {
        this._factory = factory;
    }

    /**
     * Get the initialized value, or create it if not initialized already.
     *
     * @memberof Lazy
     * @method
     * @return {Object} The initialized wrapped value
     */
    public getValue():T {
        if (this._value === undefined) {
            this._value = this._factory();
        }
        return this._value;
    }
}
