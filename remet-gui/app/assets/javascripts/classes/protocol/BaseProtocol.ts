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

import Protocol = require("./Protocol");
import ConnectionModel = require("../ConnectionModel");
import ConnectionVM = require("../ConnectionVM");
import GraphSpec = require("../GraphSpec");
import WSCommand = require("./WSCommand");

class BaseProtocol implements Protocol {
    connectionInitialized():void { }

    private connectionModel: ConnectionModel;

    constructor(cm: ConnectionModel) {
        this.connectionModel = cm;
    }

    processMessage(data:any, cvm:ConnectionVM):void { }

    subscribeToMetric(spec:GraphSpec):void { }

    unsubscribeFromMetric(spec:GraphSpec):void { }

    heartbeat(): void { }

    send(command: WSCommand) {
        this.connectionModel.send(command);
    }

    getLogs(): void { }

    subscribeLog(log: string, regexes: string[]): void { }

    unsubscribeLog(log: string, regexes: string[]): void { }
}

export = BaseProtocol;
