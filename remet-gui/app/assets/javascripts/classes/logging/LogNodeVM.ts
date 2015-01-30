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

///<reference path="../../libs/knockout/knockout.d.ts"/>
///<reference path="../BrowseNode.ts"/>
import Log = require('./Log');
import ko = require('knockout');
import Hosts = require('../Hosts');
import LiveLoggingViewModel = require('./LiveLoggingViewModel')
class LogNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    shortName: KnockoutObservable<string>;
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    display: KnockoutComputed<string>;

    constructor(public log:Log, public logViewModel:LiveLoggingViewModel) {
        this.name = log.log_name;
        this.expanded = ko.observable(false);
        this.display = ko.computed<string>(() => {
                var path = this.name().split("/");
                return path[path.length - 1 ];
            });
    }

    public expandMe() {
        if(this.logViewModel.logs.indexOf(this.log) == -1) {
            this.logViewModel.selectLogTab(this.log);
        } else {
            this.logViewModel.removeLogTab(this.log);
        }
    }
}

export = LogNodeVM;
