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

/**
 * Get the current time in nanoseconds
 */
export function getNanoTime() {
    var time = process.hrtime();
    return time[0] * 1e9 + time[1];
}

/**
 * Get the current time in milliseconds
 */
export function getMilliTime() {
    return getNanoTime() / 1e6;
}

/**
 * Determines if an object has not properties defined.
 *
 * @param obj The object to tested.
 */
export function isEmptyObject(obj) {
  return !Object.keys(obj).length;
}

/**
 * Wrapper class for lazy initialized values
 */
export class Lazy<T> {
    private factory:()=>T;
    private value:T;

    constructor(factory:()=>T) {
        this.factory = factory;
    }

    public getValue():T {
        if (this.value === undefined) {
            this.value = this.factory();
        }
        return this.value;
    }
}
