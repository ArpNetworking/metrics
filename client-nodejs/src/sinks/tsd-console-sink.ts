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
import log4js = require("log4js");
import utils = require("../utils");
import tsdSink = require("./tsd-sink");

/**
 * Implementation of [TsdSink]{@linkcode TsdSink} to record metrics to console. For an example of its
 * use please refer to the documentation for See {@link module:tsd-metrics-client#require}.
 *
 * @class
 * @alias ConsoleSink
 * @author Mohammed Kamel (mkamel at groupon dot com)
 * @ignore
 */
export class TsdConsoleSink implements tsdDef.Sink {
    /**
     * Private instance from Console to allow testing
     *
     * @type {Console}
     * @private
     * @ignore
     */
    private _console = console;
    /**
     * Invoked by [TsdMetrics]{@linkcode module:tsd-metrics-client~TsdMetrics} to record data to this <code>Sink</code>.
     *
     * @param metricsEvent logEntry to be recorded by the sync
     */
    public record(metricsEvent:tsdDef.MetricsEvent) {
        var metricsEventString = JSON.stringify(metricsEvent,
            (key, value) => {
                if (tsdSink.TsdSink.isMetricSample(value)) {
                    return {
                        value: (<tsdDef.MetricSample>value).getValue(),
                        unit: (<tsdDef.MetricSample>value).getUnit()
                    };
                }
                if (tsdSink.TsdSink.isMetricsList(value)) {
                    return {
                        values: (<tsdDef.MetricsList<tsdDef.MetricSample>>value).getValues()
                    };
                }
                if (key[0] !== "_") {
                    return value;
                }
            }, 2);

        this._console.log(metricsEventString);
    }
}

