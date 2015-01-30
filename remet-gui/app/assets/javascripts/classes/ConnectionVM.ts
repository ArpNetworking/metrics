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

///<reference path="./ViewModel.ts"/>
import Color = require('./Color');
import ConnectionModel = require('./ConnectionModel');
import ko = require('knockout');

declare var require;

class ConnectionVM {
    constructor(name: string) {
        this.server = name;
        this.model = new ConnectionModel(this.server, this);
    }
    server: string;
    status: KnockoutObservable<string> = ko.observable<string>("connecting");
    connected: KnockoutObservable<boolean> = ko.observable<boolean>(false);
    selected = ko.observable<boolean>(false);
    colorBase: KnockoutObservable<Color> = ko.observable(new Color(0, 0, 0));
    model: ConnectionModel;

    shade() {
        this.selected(!this.selected());
    }

    alpha = ko.computed(() => {
        //shaded
        var gvm = require('./GraphViewModel');
        var shouldShade = gvm.shouldShade();
        var notSelected = !this.selected();
        if (shouldShade && notSelected) {
            return 0.3;
        } else {
            return 1.0;
        }
    }, this);

    color = ko.computed(() => {
        var colArray = this.colorBase().rgb();
        return 'rgba(' + colArray[0] + ',' + colArray[1] + ',' + colArray[2] + ',' + this.alpha() + ')';
    }, this);

    reconnectString = ko.computed(() => {
        if (this.status() != "connected") {
            if (this.model && this.model.connectedAt > 0) {
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

    connect() {
        this.model.connect();
    }

    close() {
        this.model.close();
    }
}

export = ConnectionVM;
