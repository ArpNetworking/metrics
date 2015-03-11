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

import events = require("events");
import _ = require("underscore");

/**
 * Internal module for reporting errors.
 *
 * @Ignore
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */

/**
 * Event emitter for error events.
 *
 * @type {EventEmitter}
 * @ignore
 */
var _errorsEventEmitter:events.EventEmitter =
    new events.EventEmitter().addListener("error", (error)=> {
        //do-nothing listener in order not to exit the process
    });
//according to http://nodejs.org/docs/v0.3.5/api/events.html#events.EventEmitter
//if an error event is emitted and nothing was listening, the process will exit, so we are adding this

/**
 * Report an error message through the event emitter.
 *
 * @param {string} message Error message to be reported.
 * @ignore
 */
export function report(message) {
    _errorsEventEmitter.emit("error", new Error(message));
}

/**
 * Assert that a condition is true and emit an error event if not.
 *
 * @method
 * @param condition The condition to assert.
 * @param message The message to set to the error if condition not met.
 * @returns {boolean}
 */
export function assert(condition:boolean, message:string):boolean {
    if (!condition) {
        report(message);
    }
    return condition;
}

/**
 * Assert that an object is defined and its value is also defined.
 *
 * @method
 * @param object The object to test.
 * @param message The message to set to the error if condition not met.
 * @returns {boolean} True of the object is defined, otherwise false.
 * @ignore
 */
export function assertDefined(object:any, message:string):boolean {
    return assert(!_.isUndefined(object), message);
}

/**
 * Assert that an object is not defined or its value is undefined.
 *
 * @method
 * @param object The object to test.
 * @param message The message to set to the error if condition not met.
 * @returns {boolean} True of the object is undefined, otherwise false.
 * @ignore
 */
export function assertUndefined(object:any, message:string):boolean {
    return assert(_.isUndefined(object), message);
}

/**
 * Register an error callback to be notified whenever an error occurs in metrics library.
 *
 * @param errorCallback function the takes an <code>Error</code> containing the error being raised
 */
export function onError(errorCallback:(Error)=>void){
    if(!_.isFunction(errorCallback)){
        throw new Error("errorCallback is not a function");
    }
    _errorsEventEmitter.on("error", errorCallback);

}