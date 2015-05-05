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

///<reference path="../../libs/knockout/knockout.d.ts" />
import ko = require('knockout');
import HostData = require('./HostData');
import Hosts = require('../Hosts');
import MetricsSoftwareState = require('./MetricsSoftwareState');
import GraphViewModel = require('../GraphViewModel');

interface PagerElement { name: string; page: number; disabled: boolean; active: boolean}

class HostRegistryViewModel {
    filteredHosts: KnockoutObservableArray<HostData> = ko.observableArray<HostData>();
    searchHost: KnockoutObservable<string> = ko.observable('');
    searchHostThrottled: KnockoutComputed<string> = ko.computed<string>(() => {
        return this.searchHost();
    }).extend({rateLimit: {timeout: 400, method: "notifyWhenChangesStop"}});
    versionFilter: KnockoutObservable<string> = ko.observable('');
    page: KnockoutObservable<number> = ko.observable(1);
    pagesMax: KnockoutObservable<number> = ko.observable(1);
    private pagerElements: KnockoutComputed<PagerElement[]>;
    private perPage = 80;


    constructor() {
        var self = this;
        this.searchHostThrottled.subscribe(() => {this.page(1); this.query();});
        this.versionFilter.subscribe(() => {this.page(1); this.query()});
        this.query();

        this.pagerElements = ko.computed(() => {
            var p = this.page();
            var elements: PagerElement[] = [];

            var prevDisabled = p == 1;
            elements.push({name: "Prev", page: p - 1, disabled: prevDisabled, active: false});
            if (p > 3) {
                elements.push({name: "1", page: 1, disabled: false, active: false});
            }

            if (p > 4) {
                elements.push({name: "...", page: 0, disabled: true, active: false});
            }

            for (var i = p - 2; i <= p + 2; i++) {
                if (i >= 1 && i <= this.pagesMax()) {
                    var active = i == p;
                    elements.push({name: String(i), page: i, disabled: false, active: active});
                }

            }

            if (p <= this.pagesMax() - 4) {
                elements.push({name: "...", page: 0, disabled: true, active: false});
            }

            if (p <= this.pagesMax() - 3) {
                var last = this.pagesMax();
                elements.push({name: String(last), page: last, disabled: false, active: false});
            }

            var nextDisabled = p == this.pagesMax();
            elements.push({name: "Next", page: p + 1, disabled: nextDisabled, active: false});

            return elements;
        }, self);

        this.gotoPage = (element: PagerElement) => {
            console.log("element", element);
            if (element.disabled || element.page == this.page()) {
                return;
            }
            this.page(element.page);
            this.query();
        }
    }

    click(connectTo: HostData) {
        Hosts.connectToServer(connectTo.hostname);
    }

    private gotoPage: (element: PagerElement) => void;


    query() {
        var host = this.searchHostThrottled();
        var version = this.versionFilter();
        var offset = (this.page() - 1) * this.perPage;
        var query: any = {limit: this.perPage, sort_by: "HOSTNAME", offset: offset};
        if (host && host != "") {
            query.name = host;
        }
        if (version && version != "") {
            query.state = version;
        }
        $.getJSON("/v1/hosts/query", query, (data) => {
            var hostsList: HostData[] = data.data;
            this.filteredHosts.removeAll();
            this.filteredHosts(hostsList.map((v: HostData)=> { return new HostData(v.hostname, v.metricsSoftwareState);}));
            var pages = Math.floor(data.pagination.total / this.perPage) + 1;
            // If total is a multiple of the page size
            if (data.pagination.total % this.perPage == 0) {
                pages--;
            }
            this.pagesMax(pages);
            this.page(Math.floor(data.pagination.offset / this.perPage) + 1);
            if (this.filteredHosts().length == 0 && this.page() > 1) {
                this.page(this.page() - 1);
                this.query();
            }
        });
    }

}

export = HostRegistryViewModel;
