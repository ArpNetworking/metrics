/// <reference path="libs/knockout/knockout.d.ts"/>
/// <reference path="libs/jquery/jquery.d.ts"/>
/// <reference path="libs/jqueryui/jqueryui.d.ts"/>
/// <reference path="libs/jqueryui/dragslider.d.ts"/>
/// <reference path="libs/d3/d3gauge.d.ts"/>
/// <reference path="libs/sammyjs/sammyjs.d.ts"/>
/// <reference path="libs/jqueryjson/jqueryjson.d.ts"/>
declare var Flotr;
declare var MozWebSocket;

class Color
{
    r: number;
    g: number;
    b: number;
    a: number = 1;

    constructor(r: number, g: number, b: number, a: number = 1)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    rgb(): number[]
    {
        return [this.r, this.g, this.b];
    }
}

interface StatisticView {
    id: string;
    paused: boolean;
    start();
    postData(server: string, timestamp: number, dataValue, cvm: ConnectionVM);
    shutdown();
    setViewDuration(duration: ViewDuration);
    updateColor(cvm: ConnectionVM);
    disconnectConnection(cvm: ConnectionVM);
}

class GaugeVM implements StatisticView {
    id: string;
    container = null;
    started: boolean = false;
    gauge: Gauge = null;
    name: string;
    paused: boolean;
    constructor(id: string, name: string) {
        this.id = id;
        this.name = name;
    }

    start() {
        if (this.started == true) {
            return;
        }
        this.started = true;
        this.container = document.getElementById(this.id);

        var config: any =
        {
            size: 250,
            label: "",
            min: 0,
            max: 100,
            minorTicks: 5
        }

        this.gauge = new Gauge(this.id, config);
        this.gauge.render();
    }

    postData(server: string, timestamp: number, dataValue, cvm: ConnectionVM) {
        if (this.paused) {
            return;
        }
        var val = dataValue;
        this.gauge.redraw(val, 2000);
    }

    setViewDuration(duration: ViewDuration){

    }

    updateColor(cvm: ConnectionVM) {

    }

    disconnectConnection(cvm: ConnectionVM) {

    }

    shutdown() {

    }
}


class Series {
    //This is really an array of elements of [timestamp, data value]
    data: number[][] = [];
    label: string = "";
    points: any = { show: true};
    lines: any = {show: true};
    color: string = "black";
    
    constructor(label: string, color: string) {
        this.color = color;
        this.label = label;
    }
}
class GraphVM implements StatisticView {
    id: string;
    name: string;
    started: boolean = false;
    container: HTMLElement = null;
    data: Series[] = [];
    dataStreams: { [key: string]: number} = {};
    graph = null;
    stop: boolean = false;
    paused: boolean = false;
    duration: number = 30000;
    endAt: number = 0;
    dataLength: number = 600000;

    constructor(id: string, name: string) {
        this.id = id;
        this.name = name;

    }

    disconnectConnection(cvm: ConnectionVM) {

    }

    shutdown() {
        this.stop = true;
    }

    setViewDuration(window: ViewDuration) {
        console.log("setting view duration");
        console.log(window);
        var endTime = this.dataLength - window.end;
        this.duration = window.end - window.start;
        this.endAt = endTime;
    }

    niceName(id: string): string {
        return id.replace(/:/g, " ");
    }

    updateColor(cvm: ConnectionVM): void {
        console.log("updating color of " + cvm.server)
        console.log(cvm.color());
        var index = this.dataStreams[cvm.server];
        console.log("color index is " + index);

        this.data[index].color = cvm.color();
        console.log("new color is");
        console.log(cvm.color());
    }

    postData(server: string, timestamp: number, dataValue, cvm: ConnectionVM) {
        var index = this.dataStreams[cvm.server];
        if (index == undefined) {
            index = this.data.length;
            this.dataStreams[cvm.server] = index;
            cvm.color.subscribe((color) => {
                this.updateColor(cvm);
            });
            this.data.push(new Series(cvm.server, cvm.color()));
        }


        if (this.data[index].data.length == 0 || this.data[index].data[this.data[index].data.length - 1][0] < timestamp) {
            this.data[index].data.push([timestamp, dataValue]);
        }
    }

    start() {
        if (this.started == true) {
            return;
        }
        this.started = true;
        this.container = document.getElementById(this.id);

        var animate = () => {
            var tickTime = 50;
            if (this.stop) {
                return;
            }

            if (this.paused) {
                // Animate
                setTimeout(function () {
                    animate();
                }, tickTime);
                return;
            }

            //set min and max
            var graphMin =  1000000000;
            var graphMax = -1000000000;

            var now = new Date().getTime();
            var graphEnd = now - this.endAt;
            var graphStart = graphEnd - this.duration;
            for (var series = 0; series < this.data.length; series++) {
                //shift the data off the array that is too old
                while (this.data[series].data[1] != undefined && this.data[series].data[1][0] < graphEnd - this.dataLength) {
                    this.data[series].data.shift();
                }

                //find the indexes in the window
                var lower = this.data[series].data.length;
                var upper = 0;
                for (var iter = this.data[series].data.length - 1; iter >=0; iter--) {
                    var timestamp = this.data[series].data[iter][0];
                    if (timestamp >= graphStart && timestamp <= graphEnd) {
                        if (iter < lower) {
                            lower = iter;
                        }
                        if (iter > upper) {
                            upper = iter;
                        }
                    }
                }

                if (lower > 0) {
                    lower--;
                }
                if (upper < this.data[series].data.length - 1) {
                    upper++;
                }

                for (var back = lower; back <= upper; back++) {
                    //it's in our view window
                    var dataVal = this.data[series].data[back][1];
                    if (dataVal > graphMax) {
                        graphMax = dataVal;
                    }
                    if (dataVal < graphMin) {
                        graphMin = dataVal;
                    }
                }
            }
            if (graphMax == graphMin) {
                graphMin--;
                graphMax++;
            }
            else {
                var spread = graphMax - graphMin;
                var buffer = spread / 10;
                graphMin -= buffer;
                graphMax += buffer;
            }

            // Draw Graph
            this.graph = Flotr.draw(this.container, this.data, {
                yaxis : {
                    max : graphMax,
                    min : graphMin
                },
                xaxis: {
                    mode : 'time',
                    noTicks : 3,
                    min : graphStart,
                    max : graphEnd,
                    timeMode: "local"

                },
                title: this.name,
                mouse: {
                    track: true,
                    sensibility: 8,
                    radius: 15
                },
                legend: {
                    show: false
                }
            });

            // Animate
            setTimeout(function () {
                animate();
            }, tickTime);
        }
        animate();
    }
}

interface BrowseNode {
    expandMe(): void;
    display: KnockoutComputed<string>;
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    name: KnockoutObservable<string>;
}

class StatisticNodeVM implements BrowseNode {
    serviceName: KnockoutObservable<string>;
    metricName: KnockoutObservable<string>;
    statisticName: KnockoutObservable<string>;
    id: KnockoutObservable<string>;
    parent: AppViewModel;
    display: KnockoutComputed<string>
    children: KnockoutObservableArray<BrowseNode>;
    expanded: KnockoutObservable<boolean>;
    name: KnockoutObservable<string>;

    constructor(serviceName: string, metricName: string, statisticName: string, id: string, parent: AppViewModel) {
        this.serviceName = ko.observable(serviceName);
        this.metricName = ko.observable(metricName);
        this.statisticName = ko.observable(statisticName);
        this.id = ko.observable(id);
        this.parent = parent;
        this.children = ko.observableArray();
        this.expanded = ko.observable(false);
        this.name = this.statisticName;
        
        this.expandMe = () => { this.parent.addGraph(this.id(), this.metricName() + " (" + this.statisticName() + ")"); }
        this.display = ko.computed<string>(() => { return this.statisticName();});
    } 
    
    expandMe: () => void;
}

class MetricNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    children: KnockoutObservableArray<StatisticNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    parent: AppViewModel;
    display: KnockoutComputed<string>

    constructor(name: string, id: string, parent: AppViewModel) {
        this.name = ko.observable(name);
        this.children = ko.observableArray();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed<string>(() => { return this.name();});
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

class ServiceNodeVM implements BrowseNode {
    name: KnockoutObservable<string>;
    children: KnockoutObservableArray<MetricNodeVM>;
    id: KnockoutObservable<string>;
    expanded: KnockoutObservable<boolean>;
    parent: AppViewModel;
    display: KnockoutComputed<string>

    constructor(name: string, id: string, parent: AppViewModel) {
        this.name = ko.observable(name);
        this.children = ko.observableArray();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed<string>(() => { return this.name();});
    }

    expandMe() {
        this.expanded(this.expanded() == false);
    }
}

class ConnectionVM {
    constructor(name: string) {
        this.server = name;
    }
    server: string;
    socket = null;
    status: KnockoutObservable<string> = ko.observable<string>("connecting");
    hasConnected = false;
    connectedAt = 0;
    abortReconnect = false;
    selected = ko.observable<boolean>(false);
    colorBase: KnockoutObservable<Color> = ko.observable(new Color(0, 0, 0));
    time: number = 2000;
    attempt: number = 0;

    shade() {
        this.selected(!this.selected());
    }

    color = ko.computed(() => {
        var colArray = this.colorBase().rgb();
        if (shouldShade() && !this.selected()) {
            //shaded color
            return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',0.3)';
        } else {
            //base color
            return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',1.0)';
        }
    }, this);

    reconnectString = ko.computed(() => {
        if (this.status() != "connected") {
            if (this.hasConnected) {
                return "reconnect";
            }
            else {
                return "retry";
            }
        }
        else {
            return "";
        }
    });
}

class ViewDuration
{
    start: number = 570000;
    end: number = 600000;
    
    constructor(start?: number, end?: number) {
        if (start === undefined) {
            this.start = 570000;
        } else {
            this.start = start;
        }

        if (end === undefined) {
            this.end = 600000;
        } else {
            this.end = end;
        }
    }
}

class AppViewModel
{
    graphs: KnockoutObservableArray<StatisticView> = ko.observableArray();
    graphsById: {[id: string]:StatisticView} = {};
    metricsList = ko.observableArray();
    sockets = ko.observableArray();
    connections: KnockoutObservableArray<ConnectionVM> = ko.observableArray();
    connectionIndex: { [id:string] : ConnectionVM;} = {};
    viewDuration: ViewDuration = new ViewDuration();
    paused = ko.observable<boolean>(false);
    metricsVisible = ko.observable<boolean>(true);
    metricsWidth = ko.observable<boolean>(true);
    mode: KnockoutObservable<string> = ko.observable("graph");
    private colors: Color[] = [new Color(31,120,180), new Color(51,160,44), new Color(227,26,28), new Color(255,127,0),
        new Color(106,61,154), new Color(166,206,227), new Color(178,223,138), new Color(251,154,153),
        new Color(253,191,111), new Color(202,178,214), new Color(255,255,153)];
    private colorId = 0;
    sliderChanged: (event: Event, ui) => void;
    removeGraph: (vm: StatisticView) => void;
    removeConnection: (ConnectionVM) => void;
    fragment = ko.computed(function() {
        var servers = jQuery.map(this.connections(), function(element) { return element.server });
        var graphs = jQuery.map(this.graphs(), function(element) {return {id: element.id, name: element.name};});
        var obj = { connections: servers, graphs: graphs, showMetrics: this.metricsVisible(), mode: this.mode() };
        return "#" + jQuery.toJSON(obj);
    },this);

    doShade = ko.computed(function() {
        return this.connections().some(function(element) {
            return element.selected();
        })
    },this);

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
                delete this.graphsById[id];
            }
        }

        this.removeConnection = (cvm: ConnectionVM) => {
            cvm.abortReconnect = true;
            var ws = cvm.socket;
            ws.close();
            this.connections.remove(cvm);
            delete this.connectionIndex[cvm.server]
        }
        this.setMetricsWidth = () => {
            this.metricsWidth(this.metricsVisible());
        }
        
        this.reconnect = (cvm: ConnectionVM) => {
            var server = cvm.server;
            this.doConnect(server, cvm);
        }
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

//    addGraph(id: string, metric: string, statistic: string) {
    addGraph(id: string, metric: string) {
        var existing = this.graphsById[id];
        if (existing != undefined) {
            return;
        }
        var graph: StatisticView;
//        var name: string = metric + " (" + statistic + ")";
        var name: string = metric;
        if (this.mode() == "graph") {
            graph = new GraphVM(id, name);
        } else if (this.mode() == "gauge") {
            graph = new GaugeVM(id, name);
        }
        graph.setViewDuration(this.viewDuration);
        this.graphsById[id] = graph;
        this.graphs.push(graph);
    }

    startGraph(graphElement, index, gvm: StatisticView) {
        gvm.start();
    }


    setViewDuration(window: number[]) {
        var viewDuration = new ViewDuration(window[0], window[1]);
        this.viewDuration = viewDuration;
        for (var i = 0; i < this.graphs().length; i++) {
            this.graphs()[i].setViewDuration(viewDuration);
        }
    }
    
    parseFragment(fragment: string) {
        var obj = jQuery.parseJSON(fragment.substring(1));
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

        jQuery.each(servers, function(index, server) {
            self.connectToServer(server);
        });
        
        jQuery.each(graphs, function(index, graph) {
            self.addGraph(graph.id, graph.name);
        });
        
    }


    idify(value: string) : string {
        value = value.replace(/ /g, "_").toLowerCase();
        return value.replace(/\//g, "_");
    }

    getGraphName(service: string, metric: string, statistic: string) {
        return this.idify(service) + "_" + this.idify(metric) + "_" + this.idify(statistic);
    }

    sortCategories(obsArray) {
        for (var i = 0; i < obsArray().length; i++) {
            var children = obsArray()[i].children;
            this.sortCategories(children);
        }
        obsArray.sort(function(left, right) {
            return left.name() == right.name() ? 0 : (left.name() < right.name() ? -1 : 1)
        });
    }

    createMetric(service: string, metric: string, statistic: string) {
        var serviceNode: ServiceNodeVM = this.getServiceVMNode(service);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(service, this.idify(service), this);
            this.metricsList.push(serviceNode);
        }
        var metricNode: MetricNodeVM = this.getMetricVMNode(metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(metric, this.idify(metric), this);
            serviceNode.children.push(metricNode);
        }
        var stat: StatisticNodeVM = this.getStatVMNode(service, metric, statistic, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(service, metric, statistic, this.getGraphName(service, metric, statistic), this);
            metricNode.children.push(stat);
        }
        this.sortCategories(this.metricsList);
    }

    loadMetricsList(newMetrics) {
        for (var i = 0; i < newMetrics.metrics.length; i++) {
            var svc = newMetrics.metrics[i];
            for (var j = 0; j < svc.children.length; j++) {
                var metric = svc.children[j];
                for (var k = 0; k < metric.children.length; k++) {
                    var statistic = metric.children[k];
                    this.createMetric(svc.name, metric.name, statistic.name);
                }
            }
        }
    }


    getServiceVMNode(name: string) : ServiceNodeVM{
        for (var i = 0; i < this.metricsList().length; i++) {
            var svc = this.metricsList()[i];
            if (svc.name() == name) {
                return svc;
            }
        }
        return undefined;
    }

    getMetricVMNode(name: string, svcNode: ServiceNodeVM) : MetricNodeVM {
        for (var i = 0; i < svcNode.children().length; i++) {
            var metric = svcNode.children()[i];
            if (metric.name() == name) {
                return metric;
            }
        }
        return undefined;
    }

    getStatVMNode(service: string, metric: string, statistic: string, metricNode: MetricNodeVM) : StatisticNodeVM {
        for (var i = 0; i < metricNode.children().length; i++) {
            var stat: StatisticNodeVM = metricNode.children()[i];
            if (stat.serviceName() == service && stat.metricName() == metric && stat.statisticName() == statistic) {
                return stat;
            }
        }
        return undefined;
    }

    addNewMetric(newMetric) {
        this.createMetric(newMetric.service, newMetric.metric, newMetric.statistic);
    }

    reportData(metric, cvm) {
        var graphName = this.getGraphName(metric.service, metric.metric, metric.statistic);
        var graph = this.graphsById[graphName];
        if (graph != undefined) {
            graph.postData(metric.server, metric.timestamp, metric.data, cvm);
        }
    }

    processMessage(data, cvm) {
        if (data.command == "metricsList") {
            this.loadMetricsList(data.data);
        }
        else if (data.command == "newMetric") {
            this.addNewMetric(data.data);
        }
        else if (data.command == "report") {
            this.reportData(data.data, cvm);
        }
        else if (data.response == "ok") {

        }
        else {
            console.log("unhandled message: ");
            console.log(data);
        }
    }

    reconnect: (cvm: ConnectionVM) => void;

    doConnect(server: string, cvm: ConnectionVM) {
        console.log("connecting to " + server);
        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var metricsSocket: WebSocket = new WS("ws://" + server + ":7090/stream");


        var getMetricsList = () => {
            metricsSocket.send(JSON.stringify({command: "getMetrics"}));
        };

        var receiveEvent = (event) => {
            var data = JSON.parse(event.data);
            this.processMessage(data, cvm);
        };

        var errored = (event) => {
            console.log("error on socket to " + server + ": " + event);
        };

        var opened = () => {
            cvm.status("connected");
            cvm.hasConnected = true;
            cvm.connectedAt = (new Date).getTime();
            getMetricsList();
            console.log("connection established to " + server);
        };

        var retryLoop = (cvm: ConnectionVM) => {
            console.log("retry loop, attempt: " + cvm.attempt + ", waitTime: " + cvm.time);
            if (cvm.socket.readyState == 1) {
                console.log("connection has been restored");
                return;
            }
            if (cvm.abortReconnect == true) {
                console.log("aborting reconnect");
                return;
            }
            var maxTime = 60000;
            var doubled = cvm.time * 1.5;
            if (doubled > maxTime) {
                doubled = maxTime;
            }
            cvm.time = doubled;
            cvm.attempt = cvm.attempt + 1;
            if (cvm.attempt > 10 ) {
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

        var closed = (event) => {
            cvm.status("disconnected");
            console.log("connection closed to " + server);
            var now = (new Date).getTime();
            if (cvm.connectedAt > 0) {
                console.log("connection had opened at " + cvm.connectedAt + ", duration = " + (now - cvm.connectedAt));
                console.log("trying to reconnect in 2 seconds");
                cvm.time = 2000;
                cvm.attempt = 0;
                cvm.connectedAt = 0;
                cvm.status("reconnecting");
                retryLoop(cvm);
            } else {
                console.log("connection had never completed successfully");
            }
            console.log(event);
        };

        metricsSocket.onopen = opened;
        metricsSocket.onerror = errored;
        metricsSocket.onmessage = receiveEvent;
        metricsSocket.onclose = closed;

        cvm.socket = metricsSocket;
    }

    getColor() : Color {
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
        for(var i = 0; i < this.connections().length; i++) {
            var c = this.connections()[i];
            if (c.server == server) {
                return;
            }
        }

        var connectionNode: ConnectionVM = new ConnectionVM(server);
        connectionNode.colorBase(this.getColor());
        this.connectionIndex[server] = connectionNode;
        this.doConnect(server, connectionNode);

//        for (var i = 0; i < this.graphs().length; i++) {
//            var graph: StatisticView = this.graphs()[i];
//            graph.updateColor(connectionNode);
//        }
        this.connections.push(connectionNode);

        var heartbeat = (node) => {
            var metricsSocket = node.socket;
            if (metricsSocket.readyState == 1) {
                metricsSocket.send(JSON.stringify({command: "heartbeat"}));
            }
            setTimeout(() => {
                heartbeat(node);
            }, 5000);
        };

        heartbeat(connectionNode);
    }
}

interface KnockoutBindingHandlers {
    slider: KnockoutBindingHandler;
    legendBlock: KnockoutBindingHandler;
    slide: KnockoutBindingHandler;
    stackdrag: KnockoutBindingHandler;
}

ko.bindingHandlers.slider = {
    init: function(element, valueAccessor) {
        // First get the latest data that we're bound to
        var value = valueAccessor();

        $(element).dragslider(value);
    }
};

ko.bindingHandlers.slide = {
    update: function(element, valueAccessor, allBindingsAccessor) {
        var shouldShow = ko.utils.unwrapObservable(valueAccessor());
        var bindings = allBindingsAccessor();
        var direction = ko.utils.unwrapObservable(bindings.direction);
        var duration = ko.utils.unwrapObservable(bindings.duration) || 400;
        var after = ko.utils.unwrapObservable(bindings.after);

        var effectOptions = { "direction": direction };
        
        if (shouldShow) {
            after();
            $(element).show("slide", effectOptions, duration);
        } else {
            $(element).hide("slide", effectOptions, duration, after);
        }
        
        
    }
}

ko.bindingHandlers.stackdrag = {
        init: function(element, valueAccessor) {
            var thisLevel = $(element).parent().children();
            var value = valueAccessor();
            console.log("value");
            console.log(value);
            var valueUnwrapped = ko.utils.unwrapObservable(value);

//            console.log(thisLevel);
            jQuery.each(thisLevel, function(index, e) { $(e).draggable(valueUnwrapped);});
            console.log("valueUnwrapped");
            console.log(valueUnwrapped);
        }
    }

ko.bindingHandlers.legendBlock = {
    init: function(element, valueAccessor) {
        // First get the latest data that we're bound to
        var value = valueAccessor();

        // Next, whether or not the supplied model property is observable, get its current value
        var valueUnwrapped = ko.utils.unwrapObservable(value);

        var context = element.getContext('2d');

        context.beginPath();
        context.rect(3, 3, element.width - 6, element.height - 6);
        context.fillStyle = valueUnwrapped;
        context.fill();
        context.lineWidth = 2;
        context.strokeStyle = 'black';
        context.stroke();
    },
    update: function(element, valueAccessor) {
        // First get the latest data that we're bound to
        var value = valueAccessor();

        // Next, whether or not the supplied model property is observable, get its current value
        var valueUnwrapped = ko.utils.unwrapObservable(value);

        var context = element.getContext('2d');
        context.clearRect(0, 0, element.width, element.height);
        context.beginPath();
        context.rect(3, 3, element.width - 6, element.height - 6);
        context.fillStyle = valueUnwrapped;
        context.fill();
        context.lineWidth = 2;
        context.strokeStyle = 'black';
        context.stroke();
    }
};

var appModel = new AppViewModel();

function getConnectionNode(server: string) {
    return appModel.connectionIndex[server];
};

var shouldShade = ko.computed(function() {
    return appModel.doShade()
}, appModel);

ko.applyBindings(appModel);

appModel.parseFragment(document.location.hash);
