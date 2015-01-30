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

import ConnectionVM = require('../ConnectionVM');
import Hosts = require('../Hosts');

class ReportLog {
  logFile: string;
  logLine: string;
  matchingRegexes: string[];
  server: ConnectionVM;
  timestamp: number;

  constructor(data: any) {
    this.logFile = data.logFile;
    this.logLine = data.logLine;
    this.matchingRegexes = data.matchingRegexes;
    this.server = Hosts.connectionIndex[data.server];
    this.timestamp = data.timestamp;
  }
}

export = ReportLog;
