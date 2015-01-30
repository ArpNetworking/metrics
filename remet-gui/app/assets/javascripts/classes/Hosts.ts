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

///<reference path="../libs/durandal/durandal.d.ts"/>
import app = require('durandal/app');
import Color = require('./Color');
import ConnectionVM = require('./ConnectionVM');
import ko = require('knockout');

declare var require;
module Hosts {
    export var connections: KnockoutObservableArray<ConnectionVM> = ko.observableArray<ConnectionVM>();
    export var connectionIndex: { [id: string]: ConnectionVM; } = {};
    export var colors: Color[] = [new Color(31, 120, 180), new Color(51, 160, 44), new Color(227, 26, 28), new Color(255, 127, 0),
        new Color(106, 61, 154), new Color(166, 206, 227), new Color(178, 223, 138), new Color(251, 154, 153),
        new Color(253, 191, 111), new Color(202, 178, 214), new Color(255, 255, 153)];

    export var colorId = 0;

//    console.log("Hosts construct GVM = ", GraphViewModel);

    export var connectToServer = (server: string) => {
        //check to make sure the server is not already in the connect list
        for (var i = 0; i < connections().length; i++) {
            var c = connections()[i];
            if (c.server == server) {
                return;
            }
        }

        var connectionNode = new ConnectionVM(server);
        connectionNode.colorBase(getColor());
        connectionIndex[server] = connectionNode;
        connections.push(connectionNode);
        connectionNode.connect();

    };

    export var removeConnection = (cvm: ConnectionVM) => {
        connections.remove(cvm);
        delete connectionIndex[cvm.server];
        var gvm = require('./GraphViewModel');
        gvm.disconnect(cvm);
        cvm.close();
    };

    export var getColor = (): Color => {
        var color = colors[colorId];
        colorId++;
        return color;
    };
}

export = Hosts;
