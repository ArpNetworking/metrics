import GraphViewModel = require('./GraphViewModel');
import StatisticNodeVM = require('./StatisticNodeVM');
import ko = require('knockout');

class MetricNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    shortName: KnockoutObservable<string>;
    children: KnockoutObservableArray<StatisticNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    parent: GraphViewModel;
    display: KnockoutComputed<string>

    constructor(name: string, id: string, parent: GraphViewModel) {
        this.name = ko.observable(name);
        this.children = ko.observableArray<StatisticNodeVM>();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed<string>(() => { return this.name(); });
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

export = MetricNodeVM;