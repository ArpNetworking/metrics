var getConnectionNode = null;
var shouldShade = null;
function GraphVM(id, name) {
    var self = this;
    self.id = id;
    self.name = name;
    self.started = false;
    self.container = null;
    self.data = [];
    self.dataStreams = [];
    self.graph = null;
    self.stop = false;
    self.paused = false;
    self.duration = 30000;
    self.endAt = 0;
    self.dataLength = 600000;

    self.shutdown = function () {
        self.stop = true;
    };

    self.setViewDuration = function (window) {
        var endTime = self.dataLength - window[1];
        self.duration = window[1] - window[0];
        self.endAt = endTime;
    };

    self.niceName = function(id) {
        return id.replace(/:/g, " ");
    };

    self.updateColor = function(cvm) {
        var index = self.dataStreams[cvm.server];
        self.data[index].color = cvm.color();

    };

    self.postData = function(server, timestamp, dataValue, cvm) {
        var index = self.dataStreams[cvm.server];
        if (index == undefined) {
            index = self.data.length;
            self.dataStreams[cvm.server] = index;
            cvm.color.subscribe(function(color){
                self.updateColor(cvm, color);
            });
            self.data.push({data: [], label: cvm.server, points: {show: true}, lines: {show: true}, color: cvm.color()});
        }


        if (self.data[index].data.length == 0 || self.data[index].data[self.data[index].data.length - 1][0] < timestamp) {
            self.data[index].data.push([timestamp, dataValue]);
        }
    };

    self.startGraph = function() {
        if (self.started == true) {
            return;
        }
        self.started = true;
        self.container = document.getElementById(self.id);

        function animate () {
            var tickTime = 50;
            if (self.stop) {
                return;
            }

            if (self.paused) {
                // Animate
                setTimeout(function () {
                    animate();
                }, tickTime);
                return;
            }

            //set min and max
            var graphMin =  1000000000;
            var graphMax = -1000000000;

            var now = (new Date).getTime();
            var graphEnd = now - self.endAt;
            var graphStart = graphEnd - self.duration;
            for (var series = 0; series < self.data.length; series++) {
                while (self.data[series].data[1] != undefined && self.data[series].data[1][0] < graphEnd - self.dataLength) {
                    self.data[series].data.shift();
                }

                //find the indexes in the window
                var lower = self.data[series].data.length;
                var upper = 0;
                for (var iter = self.data[series].data.length - 1; iter >=0; iter--) {
                    var timestamp = self.data[series].data[iter][0];
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
                if (upper < self.data[series].data.length - 1) {
                    upper++;
                }

                for (var back = lower; back <= upper; back++) {
                    //it's in our view window
                    var dataVal = self.data[series].data[back][1];
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
            self.graph = Flotr.draw(self.container, self.data, {
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
                title: self.niceName(id),
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
    };
}

function BrowseNodeVM(name, id, children, isMetric, parent) {
    var self = this;
    self.name = ko.observable(name);
    self.children = ko.observableArray(children);
    self.id = ko.observable(id);
    self.expanded = ko.observable(false);
    self.isMetric = ko.observable(isMetric);
    self.parent = parent;

    self.expandMe = function() {
        if (!self.isMetric()) {
            self.expanded(self.expanded() == false);
        }
        else {
            self.parent.addGraph(self.id(), self.name());
        }
    };
    self.display = ko.computed(function() { return self.name();});
}

function ConnectionVM(name) {
    var self = this;
    self.server = name;
    self.socket = null;
    self.status = ko.observable("connecting");
    self.hasConnected = false;
    self.connectedAt = 0;
    self.abortReconnect = false;
    self.selected = ko.observable(false);
    self.colorBase = ko.observableArray([]);
    self.shade = function() {
        self.selected(!self.selected());
    };
    self.color = ko.computed(function() {
        if (shouldShade() && !self.selected()) {
            //shaded color
            var colArray = self.colorBase();
            return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',0.3)';
        } else {
            //base color
            var colArray = self.colorBase();
            return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',1.0)';
        }
    }, self);
    self.reconnectString = ko.computed(function() {
        if (self.status() != "connected") {
            if (self.hasConnected) {
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

var AppViewModel = function() {
    self = this;
    self.graphs = ko.observableArray();
    self.graphsById = [];
    self.metricsList = ko.observableArray();
    self.sockets = ko.observableArray();
    self.connections = ko.observableArray();
    self.connectionIndex = {};
    self.viewDuration = [570000, 600000];
    self.paused = ko.observable(false);
    self.colors = [[31,120,180], [51,160,44], [227,26,28], [255,127,0], [106,61,154], [166,206,227], [178,223,138], [251,154,153], [253,191,111], [202,178,214], [255,255,153]];
    self.colorId = 0;
    self.doShade = ko.computed(function() {
        return self.connections().some(function(element) {
            return element.selected();
        })
    },self);

    self.togglePause = function() {
        self.paused(!self.paused());
        for (var i = 0; i < self.graphs().length; i++) {
            self.graphs()[i].paused = self.paused();
        }
    };

    self.addGraph = function(id, name) {
        var existing = self.graphsById[id];
        if (existing != undefined) {
            return;
        }
        var graph = new GraphVM(id, name);
        graph.setViewDuration(self.viewDuration);
        self.graphsById[id] = graph;
        self.graphs.push(graph);
    };

    self.startGraph = function (graphElement, index, element) {
        element.startGraph();
    };

    self.removeGraph = function(gvm) {
        var id = gvm.id;
        var graph = self.graphsById[id];
        if (graph != undefined) {
            graph.shutdown();
            self.graphs.remove(graph);
            delete self.graphsById[id];
        }
    };

    self.removeConnection = function(cvm) {
        cvm.abortReconnect = true;
        var ws = cvm.socket;
        ws.close();
        self.connections.remove(cvm);
    };

    self.setViewDuration = function(window) {
        self.viewDuration = window;
        for (var i = 0; i < self.graphs().length; i++) {
            self.graphs()[i].setViewDuration(window);
        }
    };

    self.sliderChanged = function(event, ui) {
        self.setViewDuration(ui.values);
    };

    self.idify = function(value) {
        return value.replace(/ /g, "_").toLowerCase();
    };

    self.getGraphName = function(service, metric, statistic) {
        return self.idify(service) + ":" + self.idify(metric) + ":" + self.idify(statistic);
    };

    self.sortCategories = function(obsArray) {
        for (var i = 0; i < obsArray().length; i++) {
            var children = obsArray()[i].children;
            self.sortCategories(children);
        }
        obsArray.sort(function(left, right) {
            return left.name() == right.name() ? 0 : (left.name() < right.name() ? -1 : 1)
        });
    };

    self.createMetric = function(service, metric, statistic) {
        var serviceNode = self.getServiceVMNode(service);
        if (serviceNode == undefined) {
            serviceNode = new BrowseNodeVM(service, self.idify(service), [], false, self);
            self.metricsList.push(serviceNode);
        }
        var metricNode = self.getMetricVMNode(metric, serviceNode);
        if (metricNode == undefined) {
            metricNode = new BrowseNodeVM(metric, self.idify(metric), [], false, self);
            serviceNode.children.push(metricNode);
        }
        var stat = self.getStatVMNode(statistic, metricNode);
        if (stat == undefined) {
            stat = new BrowseNodeVM(statistic, self.getGraphName(service, metric, statistic), [], true, self);
            metricNode.children.push(stat);
        }
        self.sortCategories(self.metricsList);
    };

    self.loadMetricsList = function(newMetrics, cvm) {
        for (var i = 0; i < newMetrics.metrics.length; i++) {
            var svc = newMetrics.metrics[i];
            for (var j = 0; j < svc.children.length; j++) {
                var metric = svc.children[j];
                for (var k = 0; k < metric.children.length; k++) {
                    var statistic = metric.children[k];
                    self.createMetric(svc.name, metric.name, statistic.name);
                }
            }
        }
    };


    self.getServiceVMNode = function(name) {
        for (var i = 0; i < self.metricsList().length; i++) {
            var svc = self.metricsList()[i];
            if (svc.name() == name) {
                return svc;
            }
        }
        return undefined;
    };

    self.getMetricVMNode = function(name, svcNode) {
        for (var i = 0; i < svcNode.children().length; i++) {
            var metric = svcNode.children()[i];
            if (metric.name() == name) {
                return metric;
            }
        }
        return undefined;
    };

    self.getStatVMNode = function(name, metricNode) {
        for (var i = 0; i < metricNode.children().length; i++) {
            var stat = metricNode.children()[i];
            if (stat.name() == name) {
                return stat;
            }
        }
        return undefined;
    };

    self.addNewMetric = function(newMetric, cvm) {
        self.createMetric(newMetric.service, newMetric.metric, newMetric.statistic);
    };

    self.reportData = function(metric, cvm) {
        var graphName = self.getGraphName(metric.service, metric.metric, metric.statistic);
        var graph = self.graphsById[graphName];
        if (graph != undefined) {
            graph.postData(metric.server, metric.timestamp, metric.data, cvm);
        }
    };

    self.processMessage = function(data, cvm) {
        if (data.command == "metricsList") {
            self.loadMetricsList(data.data, cvm);
        }
        else if (data.command == "newMetric") {
            self.addNewMetric(data.data, cvm);
        }
        else if (data.command == "report") {
            self.reportData(data.data, cvm);
        }
        else if (data.response == "ok") {

        }
        else {
            console.log("unhandled message: ");
            console.log(data);
        }
    };

    self.reconnect = function(cvm) {
        var server = cvm.server;
        self.doConnect(server, cvm);
    };

    self.doConnect = function(server, cvm) {
        console.log("connecting to " + server);
        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var metricsSocket = new WS("ws://" + server + ":7090/stream");


        var getMetricsList = function() {
            metricsSocket.send(JSON.stringify({command: "getMetrics"}));
        };

        var receiveEvent = function(event) {
            var data = JSON.parse(event.data);
            self.processMessage(data, cvm);
        };

        var errored = function(event) {
            console.log("error on socket to " + server + ": " + event);
        };

        var opened = function() {
            cvm.status("connected");
            cvm.hasConnected = true;
            cvm.connectedAt = (new Date).getTime();
            getMetricsList();
            console.log("connection established to " + server);
        };

        var retryLoop = function(cvm) {
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
                self.doConnect(cvm.server, cvm);
                setTimeout(function() {
                    retryLoop(cvm);
                }, cvm.time);
            }
        };

        var closed = function(event) {
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
    };

    self.getColor = function() {
        var color = self.colors[self.colorId];
        self.colorId++;
        return color;
    };

    self.connect = function() {
        var server = $("#connectTo").val();
        $("#connectTo").val("");

        //check to make sure the server is not already in the connect list
        for(var i = 0; i < self.connections().length; i++) {
            var c = self.connections()[i];
            if (c.server == server) {
                return;
            }
        }

        var connectionNode = new ConnectionVM(server);
        connectionNode.colorBase(self.getColor());
        self.connectionIndex[server] = connectionNode;
        self.doConnect(server, connectionNode);

        self.connections.push(connectionNode);

        var heartbeat = function(node) {
            var metricsSocket = node.socket;
            if (metricsSocket.readyState == 1) {
                metricsSocket.send(JSON.stringify({command: "heartbeat"}));
            }
            setTimeout(function () {
                heartbeat(node);
            }, 5000);
        };

        heartbeat(connectionNode);
    }
};

ko.bindingHandlers.slider = {
    init: function(element, valueAccessor) {
        // First get the latest data that we're bound to
        var value = valueAccessor();

        $(element).dragslider(value);
    }
};

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

getConnectionNode = function(server) {
    return appModel.connectionIndex[server];
};

shouldShade = ko.computed(function() {
    return appModel.doShade()
}, appModel);


ko.applyBindings(appModel);
