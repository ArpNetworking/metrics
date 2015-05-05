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

///<reference path="../../libs/knockout/knockout.d.ts" />
import ko = require('knockout');
import ConnectionVM = require('../ConnectionVM');
import Hosts = require('../Hosts');
import MetricsSoftwareState = require('./MetricsSoftwareState');

class HostData {
    hostname: string;
    metricsSoftwareState: string;
    connectionStyle: KnockoutComputed<string>;
    connectable: KnockoutComputed<boolean>;
    connection: KnockoutComputed<boolean>;

    constructor(hostname: string, metricsState: string) {
        this.hostname = hostname;
        this.metricsSoftwareState = metricsState;
        var self = this;

        this.connectable  = ko.computed<boolean>(() => {
            var connections: ConnectionVM[] = Hosts.connections();
            for (var i in connections) {
                var connection = connections[i];
                if (connection.server == this.hostname) {
                    console.log("connectable false");
                    return false;
                }
            }
            return this.metricsSoftwareState != "NOT_INSTALLED";
        }, self);

        this.connectionStyle = ko.computed<string>(() => {
            var connections: ConnectionVM[] = Hosts.connections();
            var i;
            for (i in connections) {
                var connection = connections[i];
                if (connection.server == this.hostname) {
                    if (connection.connected()) {
                        return "host-connected glyphicon-transfer";
                    } else {
                        return "connect-up-to-date glyphicon-flash pulse";
                    }
                }
            }

            if (this.metricsSoftwareState == "OLD_VERSION_INSTALLED") {
                return "connect-old-version glyphicon-flash";
            } else if (this.metricsSoftwareState == "LATEST_VERSION_INSTALLED") {
                return "connect-up-to-date glyphicon-flash";
            } else if (this.metricsSoftwareState == "NOT_INSTALLED") {
                return "connect-host-not-installed glyphicon-flash";
            } else {
                return "connect-unknown glyphicon-flash";
            }
        }, self);
    }


}

export = HostData;
