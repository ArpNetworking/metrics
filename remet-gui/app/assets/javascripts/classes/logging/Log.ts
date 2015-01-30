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

import ko = require('knockout');
import ConnectionVM = require('../ConnectionVM');
import ReportLogVM = require('./ReportLogVM');

class Log {
  lines:KnockoutObservableArray<ReportLogVM> = ko.observableArray<ReportLogVM>();
  log_name:KnockoutObservable<string> = ko.observable<string>();
  public widgetHeight:KnockoutComputed<string>;
  private isSubscribed:boolean = false;
  public displayName:string;
  constructor(
      log_name: string,
      public parentCollection:KnockoutObservableArray<Log>) {
    this.log_name(log_name);
    var path = this.log_name().split("/");
    this.displayName = path[path.length - 1 ];
  }

  public subscribe(cvm: ConnectionVM) {

        console.warn("subscribing to " + this.log_name() + " on connection " + cvm.server);
        cvm.model.protocol.subscribeLog(this.log_name(), [".*"]);
        this.isSubscribed = true;
  }

  public unsubscribe(cvm:ConnectionVM) {
      console.warn("unsubscribing to " + this.log_name() + " on connection " + cvm.server);
      cvm.model.protocol.unsubscribeLog(this.log_name(), [".*"]);
  }

  postdata(report: ReportLogVM) {
    this.lines.push(report);
  }
}

export = Log;
