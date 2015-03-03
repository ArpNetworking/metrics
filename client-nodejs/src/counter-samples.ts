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
import counters = require("./tsd-counter");
import tsd = require("./tsd-metrics-client");
import metricsList = require("./tsd-metrics-list");
import utils = require("./utils");

//aliases
import MetricsStateObject = tsd.MetricsStateObject;
import TsdCounter = counters.TsdCounter;
import TsdMetricsList = metricsList.TsdMetricsList;

/**
 * Class for managing samples for a counter.
 *
 * @class
 * @alias CounterSamples
 * @ignore
 */
export class CounterSamples extends TsdMetricsList<tsdDef.MetricSample> {

    /**
     * Constructor.
     *
     * @param {MetricsStateObject} _metricsStateObject Object holding state of the parent metrics object.
     * @param _name Name of the counter that these samples belongs to.
     */
    public constructor(private _name:string, private _metricsStateObject:MetricsStateObject) {
        super();
    }

    /**
     * Add a new counter sample.
     *
     * @method
     * @return {Counter} The new counter sample instance added to the counter samples.
     */
    public addCounter():tsdDef.Counter {
        var tsdCounter = new TsdCounter(this._name, this._metricsStateObject);
        this.push(tsdCounter);
        return tsdCounter;
    }
}
