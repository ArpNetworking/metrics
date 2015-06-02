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

import ConnectionVM = require('./ConnectionVM');
import ViewDuration = require('./ViewDuration');
import GraphSpec = require('./GraphSpec')

interface StatisticView {
    id: string;
    name: string;
    spec: GraphSpec;
    paused: boolean;
    targetFrameRate: number;
    start(): void;
    postData(server: string, timestamp: number, dataValue: number, cvm: ConnectionVM): void;
    shutdown(): void;
    setViewDuration(duration: ViewDuration): void;
    updateColor(cvm: ConnectionVM): void;
    disconnectConnection(cvm: ConnectionVM): void;
}

export = StatisticView;
