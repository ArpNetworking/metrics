///<reference path="../libs/jqueryui/jqueryui.d.ts"/>
///<reference path="../libs/bootstrap/bootstrap.d.ts"/>
///<reference path="./BrowseNode.ts"/>
///<amd-dependency path="jquery.ui"/>
import MetricData = require('./MetricData');
import MetricNodeVM = require('./MetricNodeVM');
import ConnectionVM = require('./ConnectionVM');
import Color = require('./Color');
import Command = require('./Command');
import GaugeVM = require('./GaugeVM');
import GraphVM = require('./GraphVM');
import StatisticView = require('./StatisticView');
import ServiceNodeVM = require('./ServiceNodeVM');
import StatisticNodeVM = require('./StatisticNodeVM');
import ServiceData = require('./ServiceData');
import FolderNodeVM = require('./FolderNodeVM');
import KnockoutBindings = require('./KnockoutBindings');
import ViewDuration = require('./ViewDuration');
import MetricsListData = require('./MetricsListData');
import NewMetricData = require('./NewMetricData');
import ReportData = require('./ReportData');
import ko = require('knockout');
import $ = require('jquery');
import GraphSpec = require('./GraphSpec');

class GraphViewModel {
    graphs: KnockoutObservableArray<StatisticView> = ko.observableArray<StatisticView>();
    graphsById: { [id: string]: StatisticView } = {};
    subscriptions: GraphSpec[] = [];
    metricsList = ko.observableArray<ServiceNodeVM>();
    foldersList = ko.observableArray<FolderNodeVM>();
    sockets = ko.observableArray();
    connections: KnockoutObservableArray<ConnectionVM> = ko.observableArray<ConnectionVM>();
    connectionIndex: { [id: string]: ConnectionVM; } = {};
    viewDuration: ViewDuration = new ViewDuration();
    paused = ko.observable<boolean>(false);
    metricsVisible = ko.observable<boolean>(true);
    metricsWidth = ko.observable<boolean>(true);
    mode: KnockoutObservable<string> = ko.observable("graph");
    private colors: Color[] = [new Color(31, 120, 180), new Color(51, 160, 44), new Color(227, 26, 28), new Color(255, 127, 0),
        new Color(106, 61, 154), new Color(166, 206, 227), new Color(178, 223, 138), new Color(251, 154, 153),
        new Color(253, 191, 111), new Color(202, 178, 214), new Color(255, 255, 153)];
    private colorId = 0;
    sliderChanged: (event: Event, ui: any) => void;
    removeGraph: (vm: StatisticView) => void;
    removeConnection: (cvm: ConnectionVM) => void;
    fragment = ko.computed(function() {
        var servers = jQuery.map(this.connections(), function(element) { return element.server });
        var graphs = jQuery.map(this.graphs(), function(element: StatisticView) {
            return { service: element.spec.service, metric: element.spec.metric, stat: element.spec.statistic };
        });
        var obj = { connections: servers, graphs: graphs, showMetrics: this.metricsVisible(), mode: this.mode() };
        return "#" + encodeURIComponent(JSON.stringify(obj));
    }, this);
    searchQuery = ko.observable<String>('');
    graphWidth = ko.observable<string>('col-md-4');

    getGraphWidth = ko.computed(function() {
        return this.graphWidth();
    }, this);

    doShade = ko.computed(function() {
        return this.connections().some(function(element: ConnectionVM) {
            return element.selected();
        })
    }, this);

    shouldShade = ko.computed(function() {
        return this.doShade()
    }, this);

    constructor() {
        this.sliderChanged = (event, ui) => {
            this.setViewDuration(ui.values);
        };

        this.removeGraph = (gvm: StatisticView) => {
            var id = gvm.id;
            var graph = this.graphsById[id];
            if (graph != undefined) {
                graph.shutdown();
                this.graphs.remove(graph);
                $("#graph_div_" + graph.id).remove();
                delete this.graphsById[id];
            }
            //Remove the subscription so new connections wont receive the data
            this.subscriptions = this.subscriptions.filter((element: GraphSpec) => {
                return !(element.metric == gvm.spec.metric && element.service == gvm.spec.service && element.statistic == gvm.spec.statistic);
            });
            //Make sure to unsubscribe to the graph feed.
            this.connections().forEach((element: ConnectionVM) => {
                element.socket.send(JSON.stringify({ command: "unsubscribe", service: gvm.spec.service, metric: gvm.spec.metric, statistic: gvm.spec.statistic}))
            });
        };

        this.removeConnection = (cvm: ConnectionVM) => {
            cvm.abortReconnect = true;
            var ws = cvm.socket;
            ws.close();
            this.connections.remove(cvm);
            delete this.connectionIndex[cvm.server]
        };

        this.setMetricsWidth = () => {
            this.metricsWidth(this.metricsVisible());
        };

        this.reconnect = (cvm: ConnectionVM) => {
            var server = cvm.server;
            this.doConnect(server, cvm);
        };

        KnockoutBindings();
    }

    attached() {
        if (document.location.hash) {
            this.parseFragment(document.location.hash);
        }

        // TODO: Rewrite for KO bindings
        $('.sort-parent').sortable({
            items: '.sortable',
            connectWith: ".sort-parent"
        });
    }

    toggleMetricsVisible() {
        this.metricsVisible(!this.metricsVisible());
    }

    setMetricsWidth: () => void;

    togglePause() {
        this.paused(!this.paused());
        for (var i = 0; i < this.graphs().length; i++) {
            this.graphs()[i].paused = this.paused();
        }
    }

    subscribe(cvm: ConnectionVM, spec: GraphSpec) {
        if (cvm.socket.readyState == WebSocket.OPEN) {
            cvm.socket.send(JSON.stringify({ command: "subscribe", service: spec.service, metric: spec.metric, statistic: spec.statistic }));
        }
    }

    addGraph(graphSpec: GraphSpec) {
        var displayName = graphSpec.metric + " (" + graphSpec.statistic + ")";
        var id = this.getGraphName(graphSpec);
        this.subscriptions.push(graphSpec);
        this.connections().forEach((cvm) => {
            this.subscribe(cvm, graphSpec);
        });
        var existing = this.graphsById[id];
        if (existing != undefined) {
            return;
        }
        var graph: StatisticView;
        if (this.mode() == "graph") {
            graph = new GraphVM(id, displayName, graphSpec);
        } else if (this.mode() == "gauge") {
            graph = new GaugeVM(id, displayName, graphSpec);
        }
        graph.setViewDuration(this.viewDuration);
        this.graphsById[id] = graph;
        this.graphs.push(graph);
    }

    startGraph(graphElement: HTMLElement, index: number, gvm: StatisticView) {
        gvm.start();
    }


    setViewDuration(window: {min: number; max: number}) {
        var viewDuration = new ViewDuration(window.min, window.max);
        this.viewDuration = viewDuration;
        for (var i = 0; i < this.graphs().length; i++) {
            this.graphs()[i].setViewDuration(viewDuration);
        }
    }

    parseFragment(fragment: string) {
        var jsonStr = decodeURIComponent(fragment.substring(1));
        var obj = JSON.parse(jsonStr);
        if (obj == null) {
            return;
        }

        var servers = obj.connections;
        var graphs = obj.graphs;
        var self = this;
        this.mode(obj.mode || "graph");
        var showMetrics = obj.showMetrics;
        if (showMetrics === null || showMetrics === undefined) {
            showMetrics = true;
        }
        this.metricsVisible(showMetrics);

        servers.forEach((server) => {
            self.connectToServer(server);
        });

        graphs.forEach((graph) => {
            self.addGraph(new GraphSpec(graph.service, graph.metric, graph.stat));
        });
    }


    idify(value: string): string {
        value = value.replace(/ /g, "_").toLowerCase();
        return value.replace(/\//g, "_");
    }

    getGraphName(spec: GraphSpec) {
        return this.idify(spec.service) + "_" + this.idify(spec.metric) + "_" + this.idify(spec.statistic);
    }

    sortCategories(obsArray: KnockoutObservableArray<BrowseNode>): void {
        for (var i = 0; i < obsArray().length; i++) {
            var children = obsArray()[i].children;
            this.sortCategories(children);
        }
        obsArray.sort(function(left, right) {
            return left.name() == right.name() ? 0 : (left.name() < right.name() ? -1 : 1)
        });
    }

    createMetric(spec: GraphSpec) {
        var serviceNode: ServiceNodeVM = this.getServiceVMNode(spec.service);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(spec.service, this.idify(spec.service), this);
            this.metricsList.push(serviceNode);
        }
        var metricNode: MetricNodeVM = this.getMetricVMNode(spec.metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(spec.metric, this.idify(spec.metric), this);
            serviceNode.children.push(metricNode);
        }
        var stat: StatisticNodeVM = this.getStatVMNode(spec, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(spec, this.getGraphName(spec), this);
            metricNode.children.push(stat);
        }
        this.sortCategories(this.metricsList);
    }

    createFolderMetric(spec: GraphSpec, metricName: string, currFolder: FolderNodeVM) {
        var serviceNode: ServiceNodeVM = this.getServiceFolderVMNode(spec.service, currFolder);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(spec.service, this.idify(spec.service), this);
            currFolder.children.push(serviceNode);
        }
        var metricNode: MetricNodeVM = this.getMetricVMNode(spec.metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(spec.metric, this.idify(spec.metric), this);
            metricNode.shortName = ko.observable<string>(metricName);
            metricNode.expanded(false);
            serviceNode.children.push(metricNode);
        }

        var stat: StatisticNodeVM = this.getStatVMNode(spec, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(spec, this.getGraphName(spec), this);
            metricNode.children.push(stat);
        }
        this.sortCategories(currFolder.children);
    }

    loadFolderMetricsList(newMetrics: MetricsListData): void {
        newMetrics.metrics.forEach((service, index) => {
            var currFolder = this.getRootFolderMetric(service.name);
            service.children.forEach((metric, index) => {
                var metricSplit = metric.name.split("/");
                if (metricSplit.length > 1) {
                    this.addMetricFolder(service, metric, metricSplit, "", currFolder);
                } else {
                    metric.children.forEach((statistic, index) => {
                        this.createFolderMetric(new GraphSpec(service.name, metric.name, statistic.name), metricSplit[0], currFolder);

                    });
                }
            });
        });

        this.searchQuery.subscribe(function(searchTerm) {
            this.searchMetrics(searchTerm);
        }, this);
    }

    getRootFolderMetric(serviceName: string): FolderNodeVM {
        for (var i = 0; i < this.foldersList().length; i++) {
            if (this.foldersList()[i].name() === serviceName) {
                return this.foldersList()[i];
            }
        }

        var newFolder = new FolderNodeVM(serviceName, serviceName, true);
        this.foldersList.push(newFolder);
        return newFolder
    }

    searchMetrics(searchTerm: string) {
        for (var i = 0; i < this.foldersList().length; i++) {
            $("#" + this.foldersList()[i].name()).collapse('show');
            this.searchFolders(searchTerm, this.foldersList()[i]);
        }
    }

    searchFolders(searchTerm: string, currFolder: FolderNodeVM): boolean {
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
            var found = this.searchFolders(searchTerm, subFolder);
            if (found) {
                currFolder.visible(true);
                metricMatch = true;
            }
        }

        return metricMatch;
    }

    addMetricFolder(service: ServiceData, metric: MetricData, metricList: string[], path: string, currFolder: FolderNodeVM) {
        var currMetricName = metricList[0];
        var currPathPart = currMetricName;
        if (currMetricName.length === 0) {
            currMetricName = "/";
            currPathPart = "slash";
        }

        path += "_" + currPathPart;

        if (metricList.length > 1) {
            var metricFolder = this.getSubFolderVMNode(currMetricName, currFolder);
            if (metricFolder === undefined) {
                metricFolder = new FolderNodeVM(currMetricName, path, true);
                currFolder.subFolders.push(metricFolder);
            }

            metricList.shift();
            this.sortCategories(currFolder.subFolders);
            this.addMetricFolder(service, metric, metricList, path, metricFolder);
        } else {
            for (var k = 0; k < metric.children.length; k++) {
                var statistic = metric.children[k];
                this.createFolderMetric(new GraphSpec(service.name, metric.name, statistic.name), currMetricName, currFolder);
            }
            this.sortCategories(currFolder.children);
        }
    }

    getServiceVMNode(name: string): ServiceNodeVM {
        for (var i = 0; i < this.metricsList().length; i++) {
            var svc = this.metricsList()[i];
            if (svc.name() == name) {
                return svc;
            }
        }
        return undefined;
    }

    getServiceFolderVMNode(name: string, currFolder: FolderNodeVM): ServiceNodeVM {
        for (var i = 0; i < currFolder.children().length; i++) {
            var service = currFolder.children()[i];
            if (service.name() == name) {
                return service;
            }
        }
        return undefined;
    }

    getSubFolderVMNode(name: string, currFolder: FolderNodeVM): FolderNodeVM {
        for (var i = 0; i < currFolder.subFolders().length; i++) {
            var folder = currFolder.subFolders()[i];
            if (folder.name() == name) {
                return folder;
            }
        }
        return undefined;
    }

    getMetricVMNode(name: string, svcNode: ServiceNodeVM): MetricNodeVM {
        for (var i = 0; i < svcNode.children().length; i++) {
            var metric = svcNode.children()[i];
            if (metric.name() == name) {
                return metric;
            }
        }
        return undefined;
    }

    getStatVMNode(spec: GraphSpec, metricNode: MetricNodeVM): StatisticNodeVM {
        for (var i = 0; i < metricNode.children().length; i++) {
            var stat: StatisticNodeVM = metricNode.children()[i];
            if (stat.serviceName() == spec.service && stat.metricName() == spec.metric && stat.statisticName() == spec.statistic) {
                return stat;
            }
        }
        return undefined;
    }

    addNewMetric(newMetric: NewMetricData) {
        this.createMetric(new GraphSpec(newMetric.service, newMetric.metric, newMetric.statistic));
    }

    reportData(report: ReportData, cvm: ConnectionVM) {
        var graphName = this.getGraphName(new GraphSpec(report.service, report.metric, report.statistic));
        var graph = this.graphsById[graphName];
        if (graph != undefined) {
            graph.postData(report.server, report.timestamp, report.data, cvm);
        }
    }

    processMessage(data: any, cvm: ConnectionVM) {
        if (data.command == "metricsList") {
            var mlCommand: Command<MetricsListData> = data;
            this.loadFolderMetricsList(mlCommand.data);
        }
        else if (data.command == "newMetric") {
            var nmCommand: Command<NewMetricData> = data;
            this.addNewMetric(nmCommand.data);
        }
        else if (data.command == "report") {
            var rdCommand: Command<ReportData> = data;
            this.reportData(rdCommand.data, cvm);
        }
        else if (data.response == "ok") {

        }
        else {
            console.warn("unhandled message: ");
            console.warn(data);
        }
    }

    reconnect: (cvm: ConnectionVM) => void;

    subscribeToOpenedGraphs(cvm: ConnectionVM) {
        this.subscriptions.forEach((item: GraphSpec) => {
            this.subscribe(cvm, item);
        });
    }

    doConnect(server: string, cvm: ConnectionVM) {
        console.info("connecting to " + server);
        var metricsSocket: WebSocket = new WebSocket("ws://" + server + ":7090/stream");


        var getMetricsList = () => {
            metricsSocket.send(JSON.stringify({ command: "getMetrics" }));
        };

        var receiveEvent = (event: any) => {
            var data = JSON.parse(event.data);
            this.processMessage(data, cvm);
        };

        var errored = (event: ErrorEvent) => {
            console.warn("error on socket to " + server + ": " + event);
        };

        var opened = () => {
            cvm.status("connected");
            cvm.hasConnected = true;
            cvm.connectedAt = (new Date).getTime();
            getMetricsList();
            console.info("connection established to " + server);
            this.subscribeToOpenedGraphs(cvm);
        };

        var retryLoop = (cvm: ConnectionVM) => {
            console.info("retry loop, attempt: " + cvm.attempt + ", waitTime: " + cvm.time);
            if (cvm.socket.readyState == 1) {
                console.info("connection has been restored");
                return;
            }
            if (cvm.abortReconnect == true) {
                console.info("aborting reconnect");
                return;
            }
            var maxTime = 60000;
            var doubled = cvm.time * 1.5;
            if (doubled > maxTime) {
                doubled = maxTime;
            }
            cvm.time = doubled;
            cvm.attempt = cvm.attempt + 1;
            if (cvm.attempt > 10) {
                cvm.status("disconnected");
                return;
            }
            if (cvm.socket.readyState == 3) {
                this.doConnect(cvm.server, cvm);
                setTimeout(function() {
                    retryLoop(cvm);
                }, cvm.time);
            }
        };

        var closed = (event: CloseEvent) => {
            cvm.status("disconnected");
            console.error("connection closed to " + server);
            var now = (new Date).getTime();
            if (cvm.connectedAt > 0) {
                console.info("connection had opened at " + cvm.connectedAt + ", duration = " + (now - cvm.connectedAt));
                console.info("trying to reconnect in 2 seconds");
                cvm.time = 2000;
                cvm.attempt = 0;
                cvm.connectedAt = 0;
                cvm.status("reconnecting");
                retryLoop(cvm);
            } else {
                console.info("connection had never completed successfully, not retrying connection");
            }
        };

        metricsSocket.onopen = opened;
        metricsSocket.onerror = errored;
        metricsSocket.onmessage = receiveEvent;
        metricsSocket.onclose = closed;

        cvm.socket = metricsSocket;
    }

    getColor(): Color {
        var color = this.colors[this.colorId];
        this.colorId++;
        return color;
    }

    connect() {
        var serverBox = $("#connectTo");
        var server = serverBox.val();
        serverBox.val("");
        this.connectToServer(server);
    }

    connectToServer(server: string) {
        //check to make sure the server is not already in the connect list
        for (var i = 0; i < this.connections().length; i++) {
            var c = this.connections()[i];
            if (c.server == server) {
                return;
            }
        }

        var connectionNode = new ConnectionVM(server, this);
        connectionNode.colorBase(this.getColor());
        this.connectionIndex[server] = connectionNode;
        this.doConnect(server, connectionNode);

        this.connections.push(connectionNode);

        var heartbeat = (node: ConnectionVM) => {
            var metricsSocket = node.socket;
            if (metricsSocket.readyState == 1) {
                metricsSocket.send(JSON.stringify({ command: "heartbeat" }));
            }
            setTimeout(() => {
                heartbeat(node);
            }, 5000);
        };

        heartbeat(connectionNode);
    }

    switchGraphLayout() {
        if ($('.graph-container.col-md-4').length > 0) {
            $('.graph-container.col-md-4').each(function(index, element) { $(element).removeClass('col-md-4') });
            $('#graph-icon').removeClass('glyphicon-align-justify');
            $('#graph-icon').addClass('glyphicon-th-large');
            this.graphWidth('');
        } else {
            $('.graph-container').each(function(index, element) { $(element).addClass('col-md-4') });
            $('#graph-icon').removeClass('glyphicon-th-large');
            $('#graph-icon').addClass('glyphicon-align-justify');
            this.graphWidth('col-md-4');
        }
    }
}

export = GraphViewModel;
