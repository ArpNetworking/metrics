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
///<reference path="../../libs/jqueryui/jqueryui.d.ts"/>
import ko = require('knockout');
import ExpressionData = require('./ExpressionData');
import $ = require('jquery');

interface PagerElement { name: string; page: number; disabled: boolean; active: boolean}

class ExpressionsViewModel {
    filteredExpressions: KnockoutObservableArray<ExpressionData> = ko.observableArray<ExpressionData>();
    searchExpression: KnockoutObservable<string> = ko.observable('');
    searchExpressionThrottled: KnockoutComputed<string> = ko.computed<string>(() => {
        return this.searchExpression();
    }).extend({rateLimit: {timeout: 400, method: "notifyWhenChangesStop"}});
    page: KnockoutObservable<number> = ko.observable(1);
    pagesMax: KnockoutObservable<number> = ko.observable(1);
    private pagerElements: KnockoutComputed<PagerElement[]>;
    private perPage = 24;

    constructor() {
        var self = this;
        this.searchExpressionThrottled.subscribe(() => {this.page(1); this.query();});
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
            if (element.disabled || element.page == this.page()) {
                return;
            }
            this.page(element.page);
            this.query();
        }
    }

    private gotoPage: (element: PagerElement) => void;

    query() {
        var expression = this.searchExpressionThrottled();
        var offset = (this.page() - 1) * this.perPage;
        var query: any = {limit: this.perPage, offset: offset};
        if (expression && expression != "") {
            query.contains = expression;
        }
        $.getJSON("/v1/expressions/query", query, (data) => {
            var expressionsList: ExpressionData[] = data.data;
            this.filteredExpressions.removeAll();
            this.filteredExpressions(expressionsList.map((v: ExpressionData)=> { return new ExpressionData(v.id, v.metric, v.service, v.cluster, v.script);}));
            var pages = Math.floor(data.pagination.total / this.perPage) + 1;
            // If total is a multiple of the page size
            if (data.pagination.total % this.perPage == 0) {
                pages--;
            }
            this.pagesMax(pages);
            this.page(Math.floor(data.pagination.offset / this.perPage) + 1);
            if (this.filteredExpressions().length == 0 && this.page() > 1) {
                this.page(this.page() - 1);
                this.query();
            }
        });
    }
}

export = ExpressionsViewModel;
