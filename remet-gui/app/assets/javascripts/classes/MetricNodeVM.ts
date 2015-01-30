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

///<reference path="../libs/knockout/knockout.d.ts"/>
///<reference path="./BrowseNode.ts"/>
import GraphViewModel = require('./GraphViewModel');
import StatisticNodeVM = require('./StatisticNodeVM');
import ko = require('knockout');

class MetricNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    shortName: KnockoutObservable<string>;
    children: KnockoutObservableArray<StatisticNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    display: KnockoutComputed<string>;

    constructor(name: string, id: string) {
        this.name = ko.observable(name);
        this.children = ko.observableArray<StatisticNodeVM>();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.display = ko.computed<string>(() => { return this.name(); });
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

export = MetricNodeVM;
