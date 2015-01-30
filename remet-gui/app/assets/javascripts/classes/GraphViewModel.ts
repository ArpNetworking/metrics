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

///<reference path="../libs/jqueryui/jqueryui.d.ts"/>
///<reference path="../libs/bootstrap/bootstrap.d.ts"/>
///<reference path="BrowseNode.ts"/>
///<reference path="ViewModel.ts"/>
///<amd-dependency path="jquery.ui"/>
import app = require('durandal/app');
import MetricData = require('./MetricData');
import MetricNodeVM = require('./MetricNodeVM');
import Color = require('./Color');
import Command = require('./Command');
import GaugeVM = require('./GaugeVM');
import GraphVM = require('./GraphVM');
import StatisticView = require('./StatisticView');
import ServiceNodeVM = require('./ServiceNodeVM');
import StatisticNodeVM = require('./StatisticNodeVM');
import ServiceData = require('./ServiceData');
import FolderNodeVM = require('./FolderNodeVM');
import ViewDuration = require('./ViewDuration');
import MetricsListData = require('./MetricsListData');
import NewMetricData = require('./NewMetricData');
import ReportData = require('./ReportData');
import ko = require('knockout');
import $ = require('jquery');
import GraphSpec = require('./GraphSpec');
import Hosts = require('./Hosts');
import ConnectionVM = require('./ConnectionVM')

module GraphViewModel {
    console.log("defining graphviewmodel");
    export var connections = Hosts.connections;
    export var graphs: KnockoutObservableArray<StatisticView> = ko.observableArray<StatisticView>();
    export var graphsById: { [id: string]: StatisticView } = {};
    export var subscriptions: GraphSpec[] = [];
    export var metricsList = ko.observableArray<ServiceNodeVM>().extend({ rateLimit: 500 });
    export var foldersList = ko.observableArray<FolderNodeVM>().extend({ rateLimit: 500 });
    export var viewDuration: ViewDuration = new ViewDuration();
    export var paused = ko.observable<boolean>(false);
    export var metricsVisible = ko.observable<boolean>(true);
    export var metricsWidth = ko.observable<boolean>(true);
    export var mode: KnockoutObservable<string> = ko.observable("graph");
    export var incomingFragment: string;

    export var sliderChanged: (event: Event, ui: any) => void = (event, ui) => { setViewDuration(ui.values); };
    export var removeGraph: (vm: StatisticView) => void = (gvm: StatisticView) => {
            var id = gvm.id;
            var graph = graphsById[id];
            if (graph != undefined) {
                graph.shutdown();
                graphs.remove(graph);
                $("#graph_div_" + graph.id).remove();
                delete graphsById[id];
            }
            //Remove the subscription so new connections wont receive the data
            subscriptions = subscriptions.filter((element: GraphSpec) => {
                return !(element.metric == gvm.spec.metric && element.service == gvm.spec.service && element.statistic == gvm.spec.statistic);
            });
            //Make sure to unsubscribe from the graph feed.
            Hosts.connections().forEach((element: ConnectionVM) => {
                element.model.protocol.unsubscribeFromMetric(gvm.spec);
            });
        };
    export var fragment = ko.computed(() => {
        var servers = Hosts.connections().map((element) => { return element.server });
        var mygraphs = graphs().map((element: StatisticView) => {
            return { service: element.spec.service, metric: element.spec.metric, stat: element.spec.statistic };
        });
        var obj = { connections: servers, graphs: mygraphs, showMetrics: metricsVisible(), mode: mode() };
        return "#graph/" + encodeURIComponent(JSON.stringify(obj));
    });
    export var searchQuery = ko.observable<string>('');
    export var graphWidth = ko.observable<string>('col-md-4');

    export var getGraphWidth = ko.computed(function() {
        return graphWidth();
    });

    export var doShade = ko.computed(function() {
        return Hosts.connections().some(function(element: ConnectionVM) {
            return element.selected();
        })
    });

    export var shouldShade = ko.computed(function() {
        return doShade()
    });

    fragment.subscribe((newValue) => { app.trigger("fragment:update", newValue); });

    export var activate = (fragment) => {
        incomingFragment = fragment;

    };

    export var attached = () => {
        if (incomingFragment !== undefined && incomingFragment !== null) {
            parseFragment(incomingFragment);
        }
        // TODO: Rewrite for KO bindings
        $('.sort-parent').sortable({
            items: '.sortable',
            connectWith: ".sort-parent"
        });

        app.on('opened').then(function(cvm) {
            subscribeToOpenedGraphs(cvm);
        });

        app.trigger("activate-graph-view");
    };

    export var toggleMetricsVisible = () => {
        metricsVisible(!metricsVisible());
    };

    export var setMetricsWidth: () => void = () => {
        metricsWidth(metricsVisible());
    };

    export var togglePause = () => {
        paused(!paused());
        for (var i = 0; i < graphs().length; i++) {
            graphs()[i].paused = paused();
        }
    };

    export var subscribe = (cvm: ConnectionVM, spec: GraphSpec) => {
        cvm.model.protocol.subscribeToMetric(spec);
    };

    export var addGraph = (graphSpec: GraphSpec) => {
        var displayName = graphSpec.metric + " (" + graphSpec.statistic + ")";
        var id = getGraphName(graphSpec);
        subscriptions.push(graphSpec);
        Hosts.connections().forEach((cvm) => {
            subscribe(cvm, graphSpec);
        });
        var existing = graphsById[id];
        if (existing != undefined) {
            return;
        }
        var graph: StatisticView;
        if (mode() == "graph") {
            graph = new GraphVM(id, displayName, graphSpec);
        } else if (mode() == "gauge") {
            graph = new GaugeVM(id, displayName, graphSpec);
        }
        graph.setViewDuration(viewDuration);
        graphsById[id] = graph;
        graphs.push(graph);
    };

    export var startGraph = (graphElement: HTMLElement, index: number, gvm: StatisticView) => {
        gvm.start();
    };

    export var disconnect = (cvm: ConnectionVM) => {
        graphs().forEach((graph: StatisticView) => { graph.disconnectConnection(cvm); });
    };

    export var setViewDuration = (window: {min: number; max: number}) => {
        viewDuration = new ViewDuration(window.min, window.max);
        for (var i = 0; i < graphs().length; i++) {
            graphs()[i].setViewDuration(viewDuration);
        }
    };

    export var parseFragment = (fragment: string) => {
        var toParse = fragment;
        if (toParse.substr(0, 7) == "#graph/") {
            toParse = toParse.substr(7);
        }
        var obj = JSON.parse(toParse);
        if (obj == null) {
            return;
        }

        var servers = obj.connections;
        var graphs = obj.graphs;
        mode(obj.mode || "graph");
        var showMetrics = obj.showMetrics;
        if (showMetrics === null || showMetrics === undefined) {
            showMetrics = true;
        }
        metricsVisible(showMetrics);

        servers.forEach((server) => {
            Hosts.connectToServer(server);
        });

        graphs.forEach((graph) => {
            addGraph(new GraphSpec(graph.service, graph.metric, graph.stat));
        });
    };


    export var idify = (value: string): string => {
        value = value.replace(/ /g, "_").toLowerCase();
        return value.replace(/\//g, "_");
    };

    export var getGraphName = (spec: GraphSpec) => {
        return idify(spec.service) + "_" + idify(spec.metric) + "_" + idify(spec.statistic);
    };

    export var sortCategories = (obsArray: KnockoutObservableArray<BrowseNode>) => {
        for (var i = 0; i < obsArray().length; i++) {
            var children = obsArray()[i].children;
            sortCategories(children);
        }
        obsArray.sort(function(left, right) {
            return left.name() == right.name() ? 0 : (left.name() < right.name() ? -1 : 1)
        });
    };

    export var createMetric = (spec: GraphSpec) => {
        var serviceNode: ServiceNodeVM = getServiceVMNode(spec.service);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(spec.service, idify(spec.service));
            metricsList.push(serviceNode);
        }
        var metricNode: MetricNodeVM = getMetricVMNode(spec.metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(spec.metric, idify(spec.metric));
            serviceNode.children.push(metricNode);
        }
        var stat: StatisticNodeVM = getStatVMNode(spec, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(spec, getGraphName(spec));
            metricNode.children.push(stat);
        }
    };

    export var createFolderMetric = (spec: GraphSpec, metricName: string, currFolder: FolderNodeVM) => {
        var serviceNode: ServiceNodeVM = getServiceFolderVMNode(spec.service, currFolder);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(spec.service, idify(spec.service));
            currFolder.children.push(serviceNode);
        }
        var metricNode: MetricNodeVM = getMetricVMNode(spec.metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(spec.metric, idify(spec.metric));
            metricNode.shortName = ko.observable<string>(metricName);
            metricNode.expanded(false);
            serviceNode.children.push(metricNode);
        }

        var stat: StatisticNodeVM = getStatVMNode(spec, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(spec, getGraphName(spec));
            metricNode.children.push(stat);
        }
    };

    export var loadFolderMetricsList = (newMetrics: MetricsListData): void => {
        newMetrics.metrics.forEach((service, index) => {
            var currFolder = getRootFolderMetric(service.name);
            service.children.forEach((metric, index) => {
                var metricSplit = metric.name.split("/");
                if (metricSplit.length > 1) {
                    addMetricFolder(service, metric, metricSplit, "", currFolder);
                } else {
                    metric.children.forEach((statistic, index) => {
                        createFolderMetric(new GraphSpec(service.name, metric.name, statistic.name), metricSplit[0], currFolder);

                    });
                }
            });
        });

        searchQuery.subscribe(function(searchTerm) {
            searchMetrics(searchTerm);
        });
    };

    export var getRootFolderMetric = (serviceName: string): FolderNodeVM => {
        for (var i = 0; i < foldersList().length; i++) {
            if (foldersList()[i].name() === serviceName) {
                return foldersList()[i];
            }
        }

        var newFolder = new FolderNodeVM(serviceName, serviceName, true);
        foldersList.push(newFolder);
        return newFolder
    };

    export var searchMetrics = (searchTerm: string) => {
        for (var i = 0; i < foldersList().length; i++) {
            $("#" + foldersList()[i].name()).collapse('show');
            searchFolders(searchTerm, foldersList()[i]);
        }
    };

    export var searchFolders = (searchTerm: string, currFolder: FolderNodeVM): boolean => {
        $("#folder_" + currFolder.id()).collapse('show');
        var regex = new RegExp(".*" + searchTerm + ".*", "i");

        if (searchTerm.length <= 0) {
            currFolder.visible(true);
            $("#folder_" + currFolder.id()).collapse('hide');
        }
        else {
            currFolder.visible(false);
        }

        var metricMatch = false;
        for (var i = 0; i < currFolder.children().length; i++) {
            var service = currFolder.children()[i];
            for (var j = 0; j < service.children().length; j++) {
                var metric = service.children()[j];

                if (searchTerm.length <= 0) {
                    metric.expanded(true);
                }
                else if (regex.test(metric.name())) {
                    metric.expanded(true);
                    currFolder.visible(true);
                    metricMatch = true;
                }
                else {
                    metric.expanded(false);
                }
            }
        }

        for (var i = 0; i < currFolder.subFolders().length; i++) {
            var subFolder = currFolder.subFolders()[i];
            var found = searchFolders(searchTerm, subFolder);
            if (found) {
                currFolder.visible(true);
                metricMatch = true;
            }
        }

        return metricMatch;
    };

    export var addMetricFolder = (service: ServiceData, metric: MetricData, metricList: string[], path: string, currFolder: FolderNodeVM) => {
        var currMetricName = metricList[0];
        var currPathPart = currMetricName;
        if (currMetricName.length === 0) {
            currMetricName = "/";
            currPathPart = "slash";
        }

        path += "_" + currPathPart;

        if (metricList.length > 1) {
            var metricFolder = getSubFolderVMNode(currMetricName, currFolder);
            if (metricFolder === undefined) {
                metricFolder = new FolderNodeVM(currMetricName, path, true);
                currFolder.subFolders.push(metricFolder);
            }

            metricList.shift();
            addMetricFolder(service, metric, metricList, path, metricFolder);
        } else {
            for (var k = 0; k < metric.children.length; k++) {
                var statistic = metric.children[k];
                createFolderMetric(new GraphSpec(service.name, metric.name, statistic.name), currMetricName, currFolder);
            }
        }
    };

    export var getServiceVMNode = (name: string): ServiceNodeVM => {
        for (var i = 0; i < metricsList().length; i++) {
            var svc = metricsList()[i];
            if (svc.name() == name) {
                return svc;
            }
        }
        return undefined;
    };

    export var getServiceFolderVMNode = (name: string, currFolder: FolderNodeVM): ServiceNodeVM => {
        for (var i = 0; i < currFolder.children().length; i++) {
            var service = currFolder.children()[i];
            if (service.name() == name) {
                return service;
            }
        }
        return undefined;
    };

    export var getSubFolderVMNode = (name: string, currFolder: FolderNodeVM): FolderNodeVM => {
        for (var i = 0; i < currFolder.subFolders().length; i++) {
            var folder = currFolder.subFolders()[i];
            if (folder.name() == name) {
                return folder;
            }
        }
        return undefined;
    };

    export var getMetricVMNode = (name: string, svcNode: ServiceNodeVM): MetricNodeVM => {
        for (var i = 0; i < svcNode.children().length; i++) {
            var metric = svcNode.children()[i];
            if (metric.name() == name) {
                return metric;
            }
        }
        return undefined;
    };

    export var getStatVMNode = (spec: GraphSpec, metricNode: MetricNodeVM): StatisticNodeVM => {
        for (var i = 0; i < metricNode.children().length; i++) {
            var stat: StatisticNodeVM = metricNode.children()[i];
            if (stat.serviceName() == spec.service && stat.metricName() == spec.metric && stat.statisticName() == spec.statistic) {
                return stat;
            }
        }
        return undefined;
    };

    export var addNewMetric = (newMetric: NewMetricData) => {
        createMetric(new GraphSpec(newMetric.service, newMetric.metric, newMetric.statistic));
    };

    export var reportData = (report: ReportData, cvm: ConnectionVM) => {
        var graphName = getGraphName(new GraphSpec(report.service, report.metric, report.statistic));
        var graph = graphsById[graphName];
        if (graph != undefined) {
            graph.postData(report.server, report.timestamp, report.data, cvm);
        }
    };

    export var subscribeToOpenedGraphs =  (cvm: ConnectionVM) => {
        subscriptions.forEach((item: GraphSpec) => {
            subscribe(cvm, item);
        });
    };

    export var switchGraphLayout = () => {
        if ($('.graph-container.col-md-4').length > 0) {
            $('.graph-container.col-md-4').each(function(index, element) { $(element).removeClass('col-md-4') });
            $('#graph-icon').removeClass('glyphicon-align-justify');
            $('#graph-icon').addClass('glyphicon-th-large');
            graphWidth('');
        } else {
            $('.graph-container').each(function(index, element) { $(element).addClass('col-md-4') });
            $('#graph-icon').removeClass('glyphicon-th-large');
            $('#graph-icon').addClass('glyphicon-align-justify');
            graphWidth('col-md-4');
        }
    };
    console.log("done defining GVM");
}

export = GraphViewModel;
