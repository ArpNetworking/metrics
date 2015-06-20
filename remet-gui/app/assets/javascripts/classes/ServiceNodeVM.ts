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

///<reference path="./BrowseNode.ts"/>
///<reference path="../libs/knockout/knockout.d.ts" />
///<reference path="../libs/naturalSort/naturalSort.d.ts" />
import MetricNodeVM = require('./MetricNodeVM');
import GraphViewModel = require('./GraphViewModel');
import ko = require('knockout');
import ns = require('naturalSort');

class ServiceNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    children: KnockoutObservableArray<MetricNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    display: KnockoutComputed<string>;

    constructor(name: string, id: string) {
        this.name = ko.observable(name);
        this.children = ko.observableArray<MetricNodeVM>().extend({ rateLimit: 100, method: "notifyWhenChangesStop" });;
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.display = ko.computed<string>(() => { return this.name(); });
    }

    sort(recursive: boolean = false) {
        if (recursive) {
            ko.utils.arrayForEach(this.children(), (child: MetricNodeVM) => { child.sort(true) });
        }
        this.children.sort((left:MetricNodeVM, right:MetricNodeVM) => {
            ns.insensitive = true;
            return ns.naturalSort(left.name(), right.name());
        });
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

export = ServiceNodeVM;
