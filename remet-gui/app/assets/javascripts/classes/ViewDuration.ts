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

class ViewDuration {
    start: number = 570000;
    end: number = 600000;

    constructor(start?: number, end?: number) {
        if (start === undefined) {
            this.start = 570000;
        } else {
            this.start = start;
        }

        if (end === undefined) {
            this.end = 600000;
        } else {
            this.end = end;
        }
    }
}

export = ViewDuration;
