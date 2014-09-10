import MetricNodeVM = require('./MetricNodeVM');
import GraphViewModel = require('./GraphViewModel');
import ko = require('knockout');

class ServiceNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    children: KnockoutObservableArray<MetricNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    parent: GraphViewModel;
    display: KnockoutComputed<string>

    constructor(name: string, id: string, parent: GraphViewModel) {
        this.name = ko.observable(name);
        this.children = ko.observableArray<MetricNodeVM>();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed<string>(() => { return this.name(); });
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

export = ServiceNodeVM;