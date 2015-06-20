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
///<reference path="../libs/naturalSort/naturalSort.d.ts" />
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
import StatisticData = require('./StatisticData');
import FolderNodeVM = require('./FolderNodeVM');
import ViewDuration = require('./ViewDuration');
import MetricsListData = require('./MetricsListData');
import NewMetricData = require('./NewMetricData');
import ReportData = require('./ReportData');
import ko = require('knockout');
import kob = require('./KnockoutBindings')
import $ = require('jquery');
import GraphSpec = require('./GraphSpec');
import Hosts = require('./Hosts');
import ConnectionVM = require('./ConnectionVM')
import ns = require('naturalSort');

module GraphViewModel {
    console.log("defining graphviewmodel");
    export var connections = Hosts.connections;
    export var graphs: KnockoutObservableArray<StatisticView> = ko.observableArray<StatisticView>();
    export var graphsById: { [id: string]: StatisticView } = {};
    export var subscriptions: GraphSpec[] = [];
    export var metricsList = ko.observableArray<ServiceNodeVM>().extend({ rateLimit: 100, method: "notifyWhenChangesStop" });
    export var foldersList = ko.observableArray<FolderNodeVM>().extend({ rateLimit: 100, method: "notifyWhenChangesStop" });
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
        graph.targetFrameRate = targetFrameRate;
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

    export var createMetric = (spec: GraphSpec) => {
        var currFolder = getRootFolderMetric(spec.service);
        var metricSplit = spec.metric.split("/");
        if (metricSplit.length > 1) {
            addMetricFolder(spec.service, spec.metric, [spec.statistic], metricSplit, "", currFolder);
        } else {
            createFolderMetric(spec, metricSplit[0], currFolder);
        }
    };

    export var createFolderMetric = (spec: GraphSpec, metricName: string, currFolder: FolderNodeVM) => {
        var serviceNode: ServiceNodeVM = getServiceFolderVMNode(spec.service, currFolder);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(spec.service, idify(spec.service));
            currFolder.children.push(serviceNode);
            if (!skipSort) {
                currFolder.sortChildren();
            }
        }
        var metricNode: MetricNodeVM = getMetricVMNode(spec.metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(spec.metric, idify(spec.metric));
            metricNode.shortName = ko.observable<string>(metricName);
            metricNode.expanded(serviceNode.expanded());
            serviceNode.children.push(metricNode);
            if (!skipSort) {
                serviceNode.sort();
            }
        }

        var stat: StatisticNodeVM = getStatVMNode(spec, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(spec, getGraphName(spec));
            metricNode.children.push(stat);
            if (!skipSort) {
                metricNode.sort();
            }
        }
    };

    export var loadFolderMetricsList = (newMetrics: MetricsListData): void => {
        skipSort = true;

        newMetrics.metrics.forEach((service, index) => {
            var currFolder = getRootFolderMetric(service.name);
            service.children.forEach((metric, index) => {
                var metricSplit = metric.name.split("/");
                if (metricSplit.length > 1) {
                    var statisticNames = metric.children.map((s) =>  {return s.name; });
                    addMetricFolder(service.name, metric.name, statisticNames, metricSplit, "", currFolder);
                } else {
                    metric.children.forEach((statistic, index) => {
                        createFolderMetric(new GraphSpec(service.name, metric.name, statistic.name), metricSplit[0], currFolder);
                    });
                }
            });
        });

        skipSort = false;

        metricsList.sort((left:ServiceNodeVM, right:ServiceNodeVM) => {
            left.sort(true);
            right.sort(true);
            ns.insensitive = true;
            return ns.naturalSort(left.name(), right.name());
        });
        foldersList.sort((left:FolderNodeVM, right:FolderNodeVM) => {
            left.sortChildren(true);
            left.sortSubFolders(true);
            right.sortChildren(true);
            right.sortSubFolders(true);
            ns.insensitive = true;
            return ns.naturalSort(left.name(), right.name());
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
        if (!skipSort) {
            foldersList.sort((left:FolderNodeVM, right:FolderNodeVM) => {
                ns.insensitive = true;
                return ns.naturalSort(left.name(), right.name());
            });
        }
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
        var regex: RegExp = null;
        if (searchTerm[0] == '/' && searchTerm[searchTerm.length - 1] == '/') {
            // Treat the search term as a regex
            regex = new RegExp(searchTerm.substr(1, searchTerm.length - 2), "i");
        } else if (searchTerm.indexOf("*") >= 0 || searchTerm.indexOf("?") >= 0) {
            // Treat the search term as a wildcard expression:
            // ? = match any one character
            // * = match zero or more characters
            var escapedSearch = searchTerm.replace(/[-\/\\^$+.()|[\]{}]/g, '\\$&');
            regex = new RegExp(escapedSearch.replace("?", ".").replace("*", ".*"), "i");
        } else {
            // Just search for any path containing the search term
            regex = new RegExp(searchTerm.replace(/[-\/\\^$+.()|[\]{}]/g, '\\$&'), "i");
        }

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

    export var addMetricFolder = (service: string, metric: string, statistics: string[], metricList: string[], path: string, currFolder: FolderNodeVM) => {
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
                if (!skipSort) {
                    currFolder.sortSubFolders();
                }
                if (currFolder.expanded) {
                    metricFolder.expandMe();
                }
            }

            metricList.shift();
            addMetricFolder(service, metric, statistics, metricList, path, metricFolder);
        } else {
            for (var k = 0; k < statistics.length; k++) {
                var statistic = statistics[k];
                createFolderMetric(new GraphSpec(service, metric, statistic), currMetricName, currFolder);
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
        //if ($('.graph-container.col-md-4').length > 0) {
        if (graphLayout == 'GRID') {
            graphLayout = 'ROW';
            $('.graph-container.col-md-4').each(function(index, element) { $(element).removeClass('col-md-4') });
            $('#graph-icon').prop("title", "Click for Grid Layout");
            $('#graph-icon').removeClass('fa-align-justify');
            $('#graph-icon').addClass('fa-th-large');
            graphWidth('');
        } else {
            graphLayout = 'GRID';
            $('.graph-container').each(function(index, element) { $(element).addClass('col-md-4') });
            $('#graph-icon').prop("title", "Click for Row Layout");
            $('#graph-icon').removeClass('fa-th-large');
            $('#graph-icon').addClass('fa-align-justify');
            graphWidth('col-md-4');
        }
    };

    export var switchRenderRate = () => {
        if (targetFrameRate == 60) {
            targetFrameRate = 1;
            $('#render-icon').prop("title", "Click for Continuous");
            $('#render-icon').removeClass('fa-spinner');
            $('#render-icon').addClass('fa-circle-o-notch');
        } else {
            targetFrameRate = 60;
            $('#render-icon').prop("title", "Click for Stepped");
            $('#render-icon').removeClass('fa-circle-o-notch');
            $('#render-icon').addClass('fa-spinner');
        }
        ko.utils.arrayForEach(graphs(), (graph: StatisticView) => { graph.targetFrameRate = targetFrameRate });
    };
    console.log("done defining GVM");

    var skipSort = false;
    var graphLayout = 'GRID';
    var targetFrameRate = 60;
    var requireJsForceLoadKnockoutBindings = kob;
}

export = GraphViewModel;
