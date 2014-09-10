///<reference path="../libs/knockout/knockout.d.ts"/>
import Color = require('./Color');
import GraphViewModel = require('./GraphViewModel');
import ko = require('knockout');

class ConnectionVM {
    constructor(name: string, currViewModel: GraphViewModel) {
        this.server = name;
        this.viewModel = currViewModel;
    }
    viewModel: GraphViewModel;
    server: string;
    socket: WebSocket = null;
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
        if (this.viewModel && this.viewModel.shouldShade() && !this.selected()) {
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

export = ConnectionVM;
