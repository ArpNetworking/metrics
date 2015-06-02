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

///<reference path="../libs/knockout/knockout.d.ts" />
///<reference path="./BrowseNode.ts"/>
import GraphSpec = require('./GraphSpec');
import ko = require('knockout');

declare var require;
class StatisticNodeVM implements BrowseNode {
    serviceName: KnockoutObservable<string>;
    metricName: KnockoutObservable<string>;
    statisticName: KnockoutObservable<string>;
    id: KnockoutObservable<string>;
    display: KnockoutComputed<string>;
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    name: KnockoutObservable<string>;

    constructor(spec: GraphSpec, id: string) {
        this.serviceName = ko.observable(spec.service);
        this.metricName = ko.observable(spec.metric);
        this.statisticName = ko.observable(spec.statistic);
        this.id = ko.observable(id);
        this.children = ko.observableArray<BrowseNode>().extend({ rateLimit: 100, method: "notifyWhenChangesStop" });;
        this.expanded = ko.observable(false);
        this.name = this.statisticName;

        this.expandMe = () => {
            require('./GraphViewModel').addGraph(new GraphSpec(this.serviceName(), this.metricName(), this.statisticName())); };
        this.display = ko.computed<string>(() => { return this.statisticName(); });
    }

    expandMe: () => void;
}

export = StatisticNodeVM;
