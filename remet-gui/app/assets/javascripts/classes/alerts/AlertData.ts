/*
 * Copyright 2015 Groupon.com
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

///<reference path="../../libs/knockout/knockout.d.ts" />
import ko = require('knockout');
import Operator = require('./Operator');
import Quantity = require('../Quantity');

class AlertData {
    id: string;
    context: string;
    name: string;
    metric: string;
    service: string;
    cluster: string;
    statistic: string;
    period: string;
    operator: Operator;
    value: Quantity;
    extensions: { [id: string]: string };
    contextStyle: KnockoutComputed<string>;
    contextTip: KnockoutComputed<string>;

    constructor(id: string, context: string, name: string, metric: string, service: string, cluster: string, statistic: string, period: string, operator: Operator, value: Quantity, extensions: { [id: string]: string }) {
        this.id = id;
        this.context = context;
        this.name = name;
        this.metric = metric;
        this.service = service;
        this.cluster = cluster;
        this.statistic = statistic;
        this.period = period;
        this.operator = operator;
        this.value = value;
        this.extensions = extensions;
        var self = this;

        this.contextStyle = ko.computed<string>(() => {
            if (this.context == "HOST") {
                return "fa-cube";
            } else if (this.context == "CLUSTER") {
                return "fa-cubes";
            } else {
                return "fa-question";
            }
        }, self);

        this.contextTip = ko.computed<string>(() => {
            if (this.context == "HOST") {
                return "Evaluated Per Host";
            } else if (this.context == "CLUSTER") {
                return "Evaluated For Cluster";
            } else {
                return "";
            }
        }, self);
    }
}

export = AlertData;
