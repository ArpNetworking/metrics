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
///<reference path="./ViewModel.ts"/>
///<amd-dependency path="./KnockoutBindings" />

import app = require('durandal/app');
import Color = require('./Color');
import ConnectionVM = require('./ConnectionVM');
import GraphViewModel = require('./GraphViewModel');
import Hosts = require('./Hosts');
import ko = require('knockout');
import $ = require('jquery');

interface ResponseCallback {
    (response: {value: string}[]): void;
}

class HeaderViewModel implements ViewModel {
    fragment = ko.observable();
    connectTo: KnockoutObservable<string> = ko.observable<string>();
    autocompleteOpts: any = {
        source: {
            source: (request: string, response: ResponseCallback) => {
                $.getJSON("/hosts/v1/query", {name: request, limit: 10}, (result:any) => {
                    var hosts:{hostName: string; metricsSoftwareState: string}[] = result.data;
                    var transformed = hosts.map((host) => {
                        return {value: host.hostName}
                    });
                    response(transformed);
                });
            }
        },
        opt: {
            minLength: 2,
            delay: 500
        }
    };
    constructor() {
        app.on("fragment:update").then((data) => { this.fragment(data); } );
    }

    connections = Hosts.connections;

    connect() {
        var server = this.connectTo();
        Hosts.connectToServer(server);
        this.connectTo("");
    }

    reconnect(cvm: ConnectionVM) {
        cvm.connect();
    }

    removeConnection = (cvm: ConnectionVM) => {
        Hosts.removeConnection(cvm);
    };

    shouldShade() {
        return true;
    }
}

export = HeaderViewModel;
