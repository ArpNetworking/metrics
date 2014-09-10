///<reference path="../libs/flotr2/flotr2.d.ts"/>
import ConnectionVM = require('./ConnectionVM');
import StatisticView = require('./StatisticView');
import Series = require('./Series');
import ViewDuration = require('./ViewDuration');
import GraphSpec = require('./GraphSpec');

import Flotr = require('flotr2');

class GraphVM implements StatisticView {
    id: string;
    name: string;
    started: boolean = false;
    container: HTMLElement = null;
    data: Series[] = [];
    dataStreams: { [key: string]: number } = {};
    stop: boolean = false;
    paused: boolean = false;
    duration: number = 30000;
    endAt: number = 0;
    dataLength: number = 600000;
    spec: GraphSpec;

    constructor(id: string, name: string, spec: GraphSpec) {
        this.id = id;
        this.name = name;
        this.spec = spec;
    }

    disconnectConnection(cvm: ConnectionVM) {

    }

    shutdown() {
        this.stop = true;
    }

    setViewDuration(window: ViewDuration) {
        var endTime = this.dataLength - window.end;
        this.duration = window.end - window.start;
        this.endAt = endTime;
    }

    niceName(id: string): string {
        return id.replace(/:/g, " ");
    }

    updateColor(cvm: ConnectionVM): void {
        var index = this.dataStreams[cvm.server];
        this.data[index].color = cvm.color();
    }

    postData(server: string, timestamp: number, dataValue: number, cvm: ConnectionVM) {
        var index = this.dataStreams[cvm.server];
        if (index == undefined) {
            index = this.data.length;
            this.dataStreams[cvm.server] = index;
            cvm.color.subscribe((color: String) => {
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
                setTimeout(function() {
                    animate();
                }, tickTime);
                return;
            }

            //set min and max
            var graphMin = 1000000000;
            var graphMax = -1000000000;

            var now = new Date().getTime() - 1000;
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
                for (var iter = this.data[series].data.length - 1; iter >= 0; iter--) {
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
            } else if (graphMin > graphMax) {
                //This is the case with no data
                graphMin = 0;
                graphMax = 1;
            }
            else {
                var spread = graphMax - graphMin;
                var buffer = spread / 10;
                graphMin -= buffer;
                graphMax += buffer;
            }

            // Draw Graph
            Flotr.draw(this.container, this.data, {
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
            setTimeout(function() {
                animate();
            }, tickTime);
        }
        animate();
    }
}

export = GraphVM;
