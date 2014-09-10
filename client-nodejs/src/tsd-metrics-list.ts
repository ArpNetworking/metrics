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
 * Class for wrapping samples lists.
 */
export class TsdMetricsList<T> {
    private values:T[] = new Array < T >();

    /**
     * Push a value to the list.
     *
     * @param value The value to be pushed
     */
    public push(value:T): void {
        this[this.values.push(value) - 1] = value;
    }

    /**
     * Creates a new TsdMetricsList with all elements that pass the test implemented by the provided predicate.
     *
     * @param predicate The function to test if element should be taken.
     */
    public filter(predicate: (T) => boolean): TsdMetricsList<T> {
        var ret = new TsdMetricsList<T>();
        ret.values = this.values.filter(predicate);
        return ret;
    }

    public toJSON() {
        return { values: this.values };
    }
}
