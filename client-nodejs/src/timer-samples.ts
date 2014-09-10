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
import timers = require("./tsd-timer");
import utils = require("./utils");
import tsd = require("tsd-metrics-client");
import sample = require("./tsd-metric-sample");
import metricsList = require("./tsd-metrics-list");

//aliases
import TsdTimer = timers.TsdTimer;
import ExplicitTimer = timers.ExplicitTimer;
import MetricsStateObject = tsd.MetricsStateObject;
import TsdMetricSample = sample.TsdMetricSample;
import TsdMetricsList = metricsList.TsdMetricsList;

/**
 * Class for creating duration sample for a timer.
 */
export class TimerSamples {
    private samples:TsdMetricsList<tsdDef.Timer> = new TsdMetricsList < tsdDef.Timer >();

    public constructor(private name:string, private metricsStateObject:MetricsStateObject) {
    }

    /**
     * Create a new timer sample.
     *
     * @param name The name of the timer to be added.
     * @param metricsStateObject Object holding state of the original meterics object.
     */
    public addTimer(): tsdDef.Timer {
        var tsdTimer = new TsdTimer(this.name, this.metricsStateObject);
        this.samples.push(tsdTimer);
        return tsdTimer;
    }

    /**
     * Create a timer sample with explicit value.
     *
     * @param duration The duration to be recorded
     * @param unit The unit of the duration
     */
    public addExplicitTimer(duration:number, unit:tsdDef.Unit): void {
        this.samples.push(new ExplicitTimer(duration, unit, this.name, this.metricsStateObject));
    }

    public toJSON() {
      return this.samples.filter((timer) =>
            this.metricsStateObject.assert(timer.isStopped(),
                                            "Skipping unstopped sample for timer '" + this.name + "'"));
    }
}
