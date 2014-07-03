var Color = (function () {
    function Color(r, g, b, a) {
        if (typeof a === "undefined") { a = 1; }
        this.a = 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
    Color.prototype.rgb = function () {
        return [this.r, this.g, this.b];
    };
    return Color;
})();

var GaugeVM = (function () {
    function GaugeVM(id, name) {
        this.container = null;
        this.started = false;
        this.gauge = null;
        this.id = id;
        this.name = name;
    }
    GaugeVM.prototype.start = function () {
        if (this.started == true) {
            return;
        }
        this.started = true;
        this.container = document.getElementById(this.id);

        var config = {
            size: 250,
            label: "",
            min: 0,
            max: 100,
            minorTicks: 5
        };

        this.gauge = new Gauge(this.id, config);
        this.gauge.render();
    };

    GaugeVM.prototype.postData = function (server, timestamp, dataValue, cvm) {
        if (this.paused) {
            return;
        }
        var val = dataValue;
        this.gauge.redraw(val, 2000);
    };

    GaugeVM.prototype.setViewDuration = function (duration) {
    };

    GaugeVM.prototype.updateColor = function (cvm) {
    };

    GaugeVM.prototype.disconnectConnection = function (cvm) {
    };

    GaugeVM.prototype.shutdown = function () {
    };
    return GaugeVM;
})();

var Series = (function () {
    function Series(label, color) {
        //This is really an array of elements of [timestamp, data value]
        this.data = [];
        this.label = "";
        this.points = { show: true };
        this.lines = { show: true };
        this.color = "black";
        this.color = color;
        this.label = label;
    }
    return Series;
})();
var GraphVM = (function () {
    function GraphVM(id, name) {
        this.started = false;
        this.container = null;
        this.data = [];
        this.dataStreams = {};
        this.graph = null;
        this.stop = false;
        this.paused = false;
        this.duration = 30000;
        this.endAt = 0;
        this.dataLength = 600000;
        this.id = id;
        this.name = name;
    }
    GraphVM.prototype.disconnectConnection = function (cvm) {
    };

    GraphVM.prototype.shutdown = function () {
        this.stop = true;
    };

    GraphVM.prototype.setViewDuration = function (window) {
        console.log("setting view duration");
        console.log(window);
        var endTime = this.dataLength - window.end;
        this.duration = window.end - window.start;
        this.endAt = endTime;
    };

    GraphVM.prototype.niceName = function (id) {
        return id.replace(/:/g, " ");
    };

    GraphVM.prototype.updateColor = function (cvm) {
        console.log("updating color of " + cvm.server);
        console.log(cvm.color());
        var index = this.dataStreams[cvm.server];
        console.log("color index is " + index);

        this.data[index].color = cvm.color();
        console.log("new color is");
        console.log(cvm.color());
    };

    GraphVM.prototype.postData = function (server, timestamp, dataValue, cvm) {
        var _this = this;
        var index = this.dataStreams[cvm.server];
        if (index == undefined) {
            index = this.data.length;
            this.dataStreams[cvm.server] = index;
            cvm.color.subscribe(function (color) {
                _this.updateColor(cvm);
            });
            this.data.push(new Series(cvm.server, cvm.color()));
        }

        if (this.data[index].data.length == 0 || this.data[index].data[this.data[index].data.length - 1][0] < timestamp) {
            this.data[index].data.push([timestamp, dataValue]);
        }
    };

    GraphVM.prototype.start = function () {
        var _this = this;
        if (this.started == true) {
            return;
        }
        this.started = true;
        this.container = document.getElementById(this.id);

        var animate = function () {
            var tickTime = 50;
            if (_this.stop) {
                return;
            }

            if (_this.paused) {
                // Animate
                setTimeout(function () {
                    animate();
                }, tickTime);
                return;
            }

            //set min and max
            var graphMin = 1000000000;
            var graphMax = -1000000000;

            var now = new Date().getTime();
            var graphEnd = now - _this.endAt;
            var graphStart = graphEnd - _this.duration;
            for (var series = 0; series < _this.data.length; series++) {
                while (_this.data[series].data[1] != undefined && _this.data[series].data[1][0] < graphEnd - _this.dataLength) {
                    _this.data[series].data.shift();
                }

                //find the indexes in the window
                var lower = _this.data[series].data.length;
                var upper = 0;
                for (var iter = _this.data[series].data.length - 1; iter >= 0; iter--) {
                    var timestamp = _this.data[series].data[iter][0];
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
                if (upper < _this.data[series].data.length - 1) {
                    upper++;
                }

                for (var back = lower; back <= upper; back++) {
                    //it's in our view window
                    var dataVal = _this.data[series].data[back][1];
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
            } else {
                var spread = graphMax - graphMin;
                var buffer = spread / 10;
                graphMin -= buffer;
                graphMax += buffer;
            }

            // Draw Graph
            _this.graph = Flotr.draw(_this.container, _this.data, {
                yaxis: {
                    max: graphMax,
                    min: graphMin
                },
                xaxis: {
                    mode: 'time',
                    noTicks: 3,
                    min: graphStart,
                    max: graphEnd,
                    timeMode: "local"
                },
                title: _this.name,
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
        };
        animate();
    };
    return GraphVM;
})();

var StatisticNodeVM = (function () {
    function StatisticNodeVM(serviceName, metricName, statisticName, id, parent) {
        var _this = this;
        this.serviceName = ko.observable(serviceName);
        this.metricName = ko.observable(metricName);
        this.statisticName = ko.observable(statisticName);
        this.id = ko.observable(id);
        this.parent = parent;
        this.children = ko.observableArray();
        this.expanded = ko.observable(false);
        this.name = this.statisticName;

        this.expandMe = function () {
            _this.parent.addGraph(_this.id(), _this.metricName() + " (" + _this.statisticName() + ")");
        };
        this.display = ko.computed(function () {
            return _this.statisticName();
        });
    }
    return StatisticNodeVM;
})();

var MetricNodeVM = (function () {
    function MetricNodeVM(name, id, parent) {
        var _this = this;
        this.name = ko.observable(name);
        this.children = ko.observableArray();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed(function () {
            return _this.name();
        });
    }
    MetricNodeVM.prototype.expandMe = function () {
        this.expanded(this.expanded() == false);
    };
    return MetricNodeVM;
})();

var ServiceNodeVM = (function () {
    function ServiceNodeVM(name, id, parent) {
        var _this = this;
        this.name = ko.observable(name);
        this.children = ko.observableArray();
        this.id = ko.observable(id);
        this.expanded = ko.observable(false);
        this.parent = parent;
        this.display = ko.computed(function () {
            return _this.name();
        });
    }
    ServiceNodeVM.prototype.expandMe = function () {
        this.expanded(this.expanded() == false);
    };
    return ServiceNodeVM;
})();

var ConnectionVM = (function () {
    function ConnectionVM(name) {
        var _this = this;
        this.socket = null;
        this.status = ko.observable("connecting");
        this.hasConnected = false;
        this.connectedAt = 0;
        this.abortReconnect = false;
        this.selected = ko.observable(false);
        this.colorBase = ko.observable(new Color(0, 0, 0));
        this.time = 2000;
        this.attempt = 0;
        this.color = ko.computed(function () {
            var colArray = _this.colorBase().rgb();
            if (shouldShade() && !_this.selected()) {
                //shaded color
                return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',0.3)';
            } else {
                //base color
                return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',1.0)';
            }
        }, this);
        this.reconnectString = ko.computed(function () {
            if (_this.status() != "connected") {
                if (_this.hasConnected) {
                    return "reconnect";
                } else {
                    return "retry";
                }
            } else {
                return "";
            }
        });
        this.server = name;
    }
    ConnectionVM.prototype.shade = function () {
        this.selected(!this.selected());
    };
    return ConnectionVM;
})();

var ViewDuration = (function () {
    function ViewDuration(start, end) {
        this.start = 570000;
        this.end = 600000;
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
    return ViewDuration;
})();

var AppViewModel = (function () {
    function AppViewModel() {
        var _this = this;
        this.graphs = ko.observableArray();
        this.graphsById = {};
        this.metricsList = ko.observableArray();
        this.sockets = ko.observableArray();
        this.connections = ko.observableArray();
        this.connectionIndex = {};
        this.viewDuration = new ViewDuration();
        this.paused = ko.observable(false);
        this.metricsVisible = ko.observable(true);
        this.metricsWidth = ko.observable(true);
        this.mode = ko.observable("graph");
        this.colors = [
            new Color(31, 120, 180),
            new Color(51, 160, 44),
            new Color(227, 26, 28),
            new Color(255, 127, 0),
            new Color(106, 61, 154),
            new Color(166, 206, 227),
            new Color(178, 223, 138),
            new Color(251, 154, 153),
            new Color(253, 191, 111),
            new Color(202, 178, 214),
            new Color(255, 255, 153)
        ];
        this.colorId = 0;
        this.fragment = ko.computed(function () {
            var servers = jQuery.map(this.connections(), function (element) {
                return element.server;
            });
            var graphs = jQuery.map(this.graphs(), function (element) {
                return { id: element.id, name: element.name };
            });
            var obj = { connections: servers, graphs: graphs, showMetrics: this.metricsVisible(), mode: this.mode() };
            return "#" + jQuery.toJSON(obj);
        }, this);
        this.doShade = ko.computed(function () {
            return this.connections().some(function (element) {
                return element.selected();
            });
        }, this);
        this.sliderChanged = function (event, ui) {
            _this.setViewDuration(ui.values);
        };

        this.removeGraph = function (gvm) {
            var id = gvm.id;
            var graph = _this.graphsById[id];
            if (graph != undefined) {
                graph.shutdown();
                _this.graphs.remove(graph);
                delete _this.graphsById[id];
            }
        };

        this.removeConnection = function (cvm) {
            cvm.abortReconnect = true;
            var ws = cvm.socket;
            ws.close();
            _this.connections.remove(cvm);
            delete _this.connectionIndex[cvm.server];
        };
        this.setMetricsWidth = function () {
            _this.metricsWidth(_this.metricsVisible());
        };

        this.reconnect = function (cvm) {
            var server = cvm.server;
            _this.doConnect(server, cvm);
        };
    }
    AppViewModel.prototype.toggleMetricsVisible = function () {
        this.metricsVisible(!this.metricsVisible());
    };

    AppViewModel.prototype.togglePause = function () {
        this.paused(!this.paused());
        for (var i = 0; i < this.graphs().length; i++) {
            this.graphs()[i].paused = this.paused();
        }
    };

    //    addGraph(id: string, metric: string, statistic: string) {
    AppViewModel.prototype.addGraph = function (id, metric) {
        var existing = this.graphsById[id];
        if (existing != undefined) {
            return;
        }
        var graph;

        //        var name: string = metric + " (" + statistic + ")";
        var name = metric;
        if (this.mode() == "graph") {
            graph = new GraphVM(id, name);
        } else if (this.mode() == "gauge") {
            graph = new GaugeVM(id, name);
        }
        graph.setViewDuration(this.viewDuration);
        this.graphsById[id] = graph;
        this.graphs.push(graph);
    };

    AppViewModel.prototype.startGraph = function (graphElement, index, gvm) {
        gvm.start();
    };

    AppViewModel.prototype.setViewDuration = function (window) {
        var viewDuration = new ViewDuration(window[0], window[1]);
        this.viewDuration = viewDuration;
        for (var i = 0; i < this.graphs().length; i++) {
            this.graphs()[i].setViewDuration(viewDuration);
        }
    };

    AppViewModel.prototype.parseFragment = function (fragment) {
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

        jQuery.each(servers, function (index, server) {
            self.connectToServer(server);
        });

        jQuery.each(graphs, function (index, graph) {
            self.addGraph(graph.id, graph.name);
        });
    };

    AppViewModel.prototype.idify = function (value) {
        value = value.replace(/ /g, "_").toLowerCase();
        return value.replace(/\//g, "_");
    };

    AppViewModel.prototype.getGraphName = function (service, metric, statistic) {
        return this.idify(service) + "_" + this.idify(metric) + "_" + this.idify(statistic);
    };

    AppViewModel.prototype.sortCategories = function (obsArray) {
        for (var i = 0; i < obsArray().length; i++) {
            var children = obsArray()[i].children;
            this.sortCategories(children);
        }
        obsArray.sort(function (left, right) {
            return left.name() == right.name() ? 0 : (left.name() < right.name() ? -1 : 1);
        });
    };

    AppViewModel.prototype.createMetric = function (service, metric, statistic) {
        var serviceNode = this.getServiceVMNode(service);
        if (serviceNode === undefined) {
            serviceNode = new ServiceNodeVM(service, this.idify(service), this);
            this.metricsList.push(serviceNode);
        }
        var metricNode = this.getMetricVMNode(metric, serviceNode);
        if (metricNode === undefined) {
            metricNode = new MetricNodeVM(metric, this.idify(metric), this);
            serviceNode.children.push(metricNode);
        }
        var stat = this.getStatVMNode(service, metric, statistic, metricNode);
        if (stat === undefined) {
            stat = new StatisticNodeVM(service, metric, statistic, this.getGraphName(service, metric, statistic), this);
            metricNode.children.push(stat);
        }
        this.sortCategories(this.metricsList);
    };

    AppViewModel.prototype.loadMetricsList = function (newMetrics) {
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
    };

    AppViewModel.prototype.getServiceVMNode = function (name) {
        for (var i = 0; i < this.metricsList().length; i++) {
            var svc = this.metricsList()[i];
            if (svc.name() == name) {
                return svc;
            }
        }
        return undefined;
    };

    AppViewModel.prototype.getMetricVMNode = function (name, svcNode) {
        for (var i = 0; i < svcNode.children().length; i++) {
            var metric = svcNode.children()[i];
            if (metric.name() == name) {
                return metric;
            }
        }
        return undefined;
    };

    AppViewModel.prototype.getStatVMNode = function (service, metric, statistic, metricNode) {
        for (var i = 0; i < metricNode.children().length; i++) {
            var stat = metricNode.children()[i];
            if (stat.serviceName() == service && stat.metricName() == metric && stat.statisticName() == statistic) {
                return stat;
            }
        }
        return undefined;
    };

    AppViewModel.prototype.addNewMetric = function (newMetric) {
        this.createMetric(newMetric.service, newMetric.metric, newMetric.statistic);
    };

    AppViewModel.prototype.reportData = function (metric, cvm) {
        var graphName = this.getGraphName(metric.service, metric.metric, metric.statistic);
        var graph = this.graphsById[graphName];
        if (graph != undefined) {
            graph.postData(metric.server, metric.timestamp, metric.data, cvm);
        }
    };

    AppViewModel.prototype.processMessage = function (data, cvm) {
        if (data.command == "metricsList") {
            this.loadMetricsList(data.data);
        } else if (data.command == "newMetric") {
            this.addNewMetric(data.data);
        } else if (data.command == "report") {
            this.reportData(data.data, cvm);
        } else if (data.response == "ok") {
        } else {
            console.log("unhandled message: ");
            console.log(data);
        }
    };

    AppViewModel.prototype.doConnect = function (server, cvm) {
        var _this = this;
        console.log("connecting to " + server);
        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var metricsSocket = new WS("ws://" + server + ":7090/stream");

        var getMetricsList = function () {
            metricsSocket.send(JSON.stringify({ command: "getMetrics" }));
        };

        var receiveEvent = function (event) {
            var data = JSON.parse(event.data);
            _this.processMessage(data, cvm);
        };

        var errored = function (event) {
            console.log("error on socket to " + server + ": " + event);
        };

        var opened = function () {
            cvm.status("connected");
            cvm.hasConnected = true;
            cvm.connectedAt = (new Date()).getTime();
            getMetricsList();
            console.log("connection established to " + server);
        };

        var retryLoop = function (cvm) {
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
            if (cvm.attempt > 10) {
                cvm.status("disconnected");
                return;
            }
            if (cvm.socket.readyState == 3) {
                _this.doConnect(cvm.server, cvm);
                setTimeout(function () {
                    retryLoop(cvm);
                }, cvm.time);
            }
        };

        var closed = function (event) {
            cvm.status("disconnected");
            console.log("connection closed to " + server);
            var now = (new Date()).getTime();
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

    AppViewModel.prototype.getColor = function () {
        var color = this.colors[this.colorId];
        this.colorId++;
        return color;
    };

    AppViewModel.prototype.connect = function () {
        var serverBox = $("#connectTo");
        var server = serverBox.val();
        serverBox.val("");
        this.connectToServer(server);
    };

    AppViewModel.prototype.connectToServer = function (server) {
        for (var i = 0; i < this.connections().length; i++) {
            var c = this.connections()[i];
            if (c.server == server) {
                return;
            }
        }

        var connectionNode = new ConnectionVM(server);
        connectionNode.colorBase(this.getColor());
        this.connectionIndex[server] = connectionNode;
        this.doConnect(server, connectionNode);

        //        for (var i = 0; i < this.graphs().length; i++) {
        //            var graph: StatisticView = this.graphs()[i];
        //            graph.updateColor(connectionNode);
        //        }
        this.connections.push(connectionNode);

        var heartbeat = function (node) {
            var metricsSocket = node.socket;
            if (metricsSocket.readyState == 1) {
                metricsSocket.send(JSON.stringify({ command: "heartbeat" }));
            }
            setTimeout(function () {
                heartbeat(node);
            }, 5000);
        };

        heartbeat(connectionNode);
    };
    return AppViewModel;
})();

ko.bindingHandlers.slider = {
    init: function (element, valueAccessor) {
        // First get the latest data that we're bound to
        var value = valueAccessor();

        $(element).dragslider(value);
    }
};

ko.bindingHandlers.slide = {
    update: function (element, valueAccessor, allBindingsAccessor) {
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
};

ko.bindingHandlers.stackdrag = {
    init: function (element, valueAccessor) {
        var thisLevel = $(element).parent().children();
        var value = valueAccessor();
        console.log("value");
        console.log(value);
        var valueUnwrapped = ko.utils.unwrapObservable(value);

        //            console.log(thisLevel);
        jQuery.each(thisLevel, function (index, e) {
            $(e).draggable(valueUnwrapped);
        });
        console.log("valueUnwrapped");
        console.log(valueUnwrapped);
    }
};

ko.bindingHandlers.legendBlock = {
    init: function (element, valueAccessor) {
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
    update: function (element, valueAccessor) {
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

function getConnectionNode(server) {
    return appModel.connectionIndex[server];
}
;

var shouldShade = ko.computed(function () {
    return appModel.doShade();
}, appModel);

ko.applyBindings(appModel);

appModel.parseFragment(document.location.hash);
