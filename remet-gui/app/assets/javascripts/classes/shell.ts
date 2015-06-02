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
import router = require('plugins/router');

class shell {
    router = router;
    activate() {
        router.map([
            { route: '', title: 'ReMet', moduleId: 'classes/GraphViewModel', nav: false },
            { route: 'graph/*spec', title: 'ReMet', moduleId: 'classes/GraphViewModel', nav: false },
            { route: 'live-logging', title: 'ReMet Live Logging', moduleId: 'classes/logging/LiveLoggingViewModel', nav: false },
            { route: 'host-registry', title: 'ReMet Host Registry', moduleId: 'classes/hostregistry/HostRegistryViewModel', nav: false },
            { route: 'expressions', title: 'ReMet Expressions', moduleId: 'classes/expressions/ExpressionsViewModel', nav: false },
            { route: 'alerts', title: 'ReMet Alerts', moduleId: 'classes/alerts/AlertsViewModel', nav: false }
        ]).buildNavigationModel();

        return router.activate();
    }
}

export = shell;
