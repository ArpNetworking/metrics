import GraphViewModel = require('./GraphViewModel');
import GraphSpec = require('./GraphSpec');
import ko = require('knockout');

class StatisticNodeVM implements BrowseNode {
    serviceName: KnockoutObservable<string>;
    metricName: KnockoutObservable<string>;
    statisticName: KnockoutObservable<string>;
    id: KnockoutObservable<string>;
    parent: GraphViewModel;
    display: KnockoutComputed<string>;
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    name: KnockoutObservable<string>;

    constructor(spec: GraphSpec, id: string, parent: GraphViewModel) {
        this.serviceName = ko.observable(spec.service);
        this.metricName = ko.observable(spec.metric);
        this.statisticName = ko.observable(spec.statistic);
        this.id = ko.observable(id);
        this.parent = parent;
        this.children = ko.observableArray<BrowseNode>();
        this.expanded = ko.observable(false);
        this.name = this.statisticName;

        this.expandMe = () => { this.parent.addGraph(new GraphSpec(this.serviceName(), this.metricName(), this.statisticName())); };
        this.display = ko.computed<string>(() => { return this.statisticName(); });
    }

    expandMe: () => void;
}

export = StatisticNodeVM;
