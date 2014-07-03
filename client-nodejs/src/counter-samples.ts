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
import counters = require("./tsd-counter");
import tsd = require("tsd-metrics-client");
import metricsList = require("./tsd-metrics-list");
import utils = require("./utils");

//aliases
import MetricsStateObject = tsd.MetricsStateObject;
import TsdCounter = counters.TsdCounter;
import TsdMetricsList = metricsList.TsdMetricsList;

/**
 * Class for managing samples for a counter.
 */
export class CounterSamples {
    private samples:TsdMetricsList<tsdDef.Counter> = new TsdMetricsList < tsdDef.Counter >();

    public constructor(private metricsStateObject:MetricsStateObject){
    }

    /**
     * Add a new counter sample.
     */
    public addCounter(): tsdDef.Counter{
        var tsdCounter = new TsdCounter(this.metricsStateObject);
        this.samples.push(tsdCounter);
        return tsdCounter;
    }

    public toJSON() {
        return this.samples;
    }
}
