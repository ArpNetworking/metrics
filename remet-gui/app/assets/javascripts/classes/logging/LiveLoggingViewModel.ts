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

import app = require('durandal/app');
import ko = require('knockout');
import ConnectionVM = require('../ConnectionVM');
import Hosts = require('../Hosts');
import Log = require('./Log');
import Command = require('../Command');
import ReportLog = require('./ReportLog');
import LogNodeVM = require('./LogNodeVM');
import ReportLogVM = require('./ReportLogVM');

class LiveLoggingViewModel implements ViewModel {
  connections = Hosts.connections;
  available_log_names:KnockoutObservableArray<LogNodeVM> = ko.observableArray<LogNodeVM>();
  logsByName: { [id: string]: Log } = {};
  logs:KnockoutObservableArray<Log> = ko.observableArray<Log>();

  shouldShade = ko.computed(function() {
      return true
  }, this);

  selectedTab:KnockoutObservable<Log> = ko.observable<Log>();

  constructor() {
  }

  attached() {
    app.on('receive_event').then(function(data, cvm) {
      this.processMessage(data, cvm);
    }, this);

    // if the connection was established before opening the logs view, we would have missed the 'logsList'
    // command so let's make sure we are not missing anything
    if(this.available_log_names.length == 0){
        this.connections().forEach((cvm: ConnectionVM) => cvm.model.protocol.getLogs());
    }
  }

  addLog(log_name: string, cvm: ConnectionVM) {
    var existing = this.logsByName[log_name];
    if (existing != undefined) {
      return;
    }

    var log: Log;
    log = new Log(log_name, this.logs);
    this.logsByName[log_name] = log;
    var logNode = new LogNodeVM(log, this)
    this.available_log_names.push(logNode);
    this.logs.push(log);
  }

  selectLogTab(log:Log) {
    if(this.logs.indexOf(log) == -1) {
      this.connections().forEach((cvm) => log.subscribe(cvm));
      this.logs.push(log);
    }
    this.selectedTab(log);
  }

  removeLogTab(log:Log) {
    if(this.logs.indexOf(log) != -1) {
      this.connections().forEach((cvm) => log.unsubscribe(cvm));
      this.logs.remove(log);
    }
  }

  processMessage(data: any, cvm: ConnectionVM) {
    if (data.command == "logsList") {
      console.warn("logsList:");
      console.warn(data.data.logs);

      data.data.logs.forEach((log_name) => {
        this.addLog(log_name, cvm);
      });
    }
    else if (data.command == "reportLog") {
      console.warn("reportlog " + data.data);
      var rlCommand: Command<ReportLog> = data;
      this.reportLog(rlCommand.data, cvm);
    }
    else {
      console.warn("unhandled message: " + data.command);
      console.warn(data);
    }
  }

  reportLog(report: ReportLog, cvm: ConnectionVM) {
    var log = this.logsByName[report.logFile];
    if (log != undefined) {
      log.postdata(new ReportLogVM(report, cvm));
    }
  }
}

export = LiveLoggingViewModel;
