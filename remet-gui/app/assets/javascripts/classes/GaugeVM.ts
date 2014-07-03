///<reference path="../libs/d3/d3gauge.d.ts"/>
import ConnectionVM = require('./ConnectionVM');
import StatisticView = require('./StatisticView');
import ViewDuration = require('./ViewDuration');
import GraphSpec = require('./GraphSpec')

class GaugeVM implements StatisticView {
    id: string;
    container: HTMLElement = null;
    started: boolean = false;
    gauge: Gauge = null;
    name: string;
    paused: boolean;
    spec: GraphSpec;

    constructor(id: string, name: string, spec: GraphSpec) {
        this.id = id;
        this.name = name;
        this.spec = spec;
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
            };

        this.gauge = new Gauge(this.id, config);
        this.gauge.render();
    }

    postData(server: string, timestamp: number, dataValue: number, cvm: ConnectionVM) {
        if (this.paused) {
            return;
        }
        var val = dataValue;
        this.gauge.redraw(val, 2000);
    }

    setViewDuration(duration: ViewDuration) {  }

    updateColor(cvm: ConnectionVM) {  }

    disconnectConnection(cvm: ConnectionVM) {  }

    shutdown() {  }
}

export = GaugeVM;
