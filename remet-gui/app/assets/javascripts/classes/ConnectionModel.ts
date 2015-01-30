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

import Protocol = require("./protocol/Protocol");
import V1Protocol = require("./protocol/V1Protocol");
import V2Protocol = require("./protocol/V2Protocol");
import ConnectionVM = require("./ConnectionVM");
import WSCommand = require("./protocol/WSCommand");
import GraphViewModel = require("./GraphViewModel");
import app = require("durandal/app");

class ConnectionModel {
    socket: WebSocket = null;
    server: String;
    protocol: Protocol;
    cvm: ConnectionVM;
    connectionList: {path: String; protocol: Protocol}[];
    connectedAt: number;
    reconnectTime: number = 2000;
    attempt: number = 1;
    reconnecting: boolean = false;
    disconnect: boolean = false;
    retryConnectionHandle: number = 0;

    constructor(server: String, cvm: ConnectionVM) {
        this.server = server;
        this.cvm = cvm;
        this.heartbeat();
    }

    send(command: WSCommand) {
        if (this.socket != null && this.socket.readyState == WebSocket.OPEN) {
            this.socket.send(JSON.stringify(command));
        }
    }

    connect() {
        if (this.socket == null
            || this.socket.readyState == WebSocket.CLOSED
            || this.socket.readyState == WebSocket.CLOSING) {
            this.startNewConnect();
        }
    }

    private startNewConnect() {
        console.info("connecting to " + this.server);
        this.resetConnectionList();
        this.disconnect = false;
        this.reconnectTime = 2000;
        this.attempt = 1;
        this.connectedAt = 0;
        this.buildWebSocket();
    }

    private resetConnectionList() {
        this.connectionList = <{path: String; protocol: Protocol}[]>[
            {path: "/telemetry/v2/stream", protocol: new V2Protocol(this)},
            {path: "/telemetry/v1/stream", protocol: new V1Protocol(this)},
            {path: "/stream", protocol: new V1Protocol(this)}
        ];
    }

    buildWebSocket() {
        var serverNameComponents = this.server.split(":");
        var serverHost = serverNameComponents[0];
        var serverPort = (typeof serverNameComponents[1] === "undefined") ? "7090" : serverNameComponents[1];

        this.protocol = this.connectionList[0].protocol;
        var path = "ws://" + serverHost + ":" + serverPort + this.connectionList[0].path;
        console.info("Attempting connection to " + path + "; attempt " + this.attempt);
        var metricsSocket:WebSocket = new WebSocket(path);
        metricsSocket.onopen = this.opened;
        metricsSocket.onmessage = this.receiveData;
        metricsSocket.onclose = this.closed;
        this.socket = metricsSocket;
    }

    private heartbeat() {
        if (this.protocol != null) {
            this.protocol.heartbeat();
        }
        setTimeout(() => { this.heartbeat(); }, 5000);
    }


    opened = () => {
        this.cvm.status("connected");
        this.cvm.connected(true);
        this.connectedAt = Date.now();
        this.attempt = 1;
        this.reconnecting = false;
        this.reconnectTime = 2000;
        this.protocol.connectionInitialized();
        console.info("connection established to " + this.server);
        app.trigger('opened', this.cvm);
    };

    receiveData = (event: any) => {
        var data = JSON.parse(event.data);
        this.protocol.processMessage(data, this.cvm);
    };

    closed = (event: CloseEvent) => {
        this.cvm.connected(false);
        this.cvm.status("disconnected");
        app.trigger('connection_closed', this.cvm);
        if (this.disconnect) {
            console.info("connection closed to " + this.server);
            return;
        }
        console.error("connection closed to " + this.server);
        if (this.connectedAt > 0) {
            this.cvm.status("reconnecting");
            // We connected successfully and then got a disconnect
            var now = (new Date).getTime();
            console.info("connection had opened at " + this.connectedAt + ", duration = " + (now - this.connectedAt));
            this.connectedAt = 0;
            this.reconnecting = true;
            // Reset the connection list and try again
            this.resetConnectionList();
        } else if (this.reconnecting) {
            console.warn("reconnect failed");
            this.connectionList.shift();
        } else {
            console.warn("connection failed");
            this.connectionList.shift();
        }


        // Since the connection failed, we move to the next endpoint
        // When the list of endpoints is empty, we reset it.
        if (this.connectionList.length == 0) {
            this.resetConnectionList();

            // We've looped through all the endpoints. If we're reconnecting, increment the attempt
            // and try again after a delay
                this.attempt += 1;

                var randomWait = Math.random() * Math.pow(1.5, this.attempt) * this.reconnectTime;
                var delay = Math.min(60000, randomWait);
                console.info("will attempt reconnect in " + (delay / 1000).toFixed() + " seconds.");
                var self = this;
                this.retryConnectionHandle = setTimeout(() => { self.buildWebSocket(); }, delay);
        } else {
            // Try to connect to the next endpoint on the list immediately
            this.buildWebSocket();
        }
    };

    close() {
        this.disconnect = true;
        if (this.retryConnectionHandle != 0) {
            clearTimeout(this.retryConnectionHandle);
        }
        if (this.socket != null) {
            this.socket.close();
        }
    }
}

export = ConnectionModel;
