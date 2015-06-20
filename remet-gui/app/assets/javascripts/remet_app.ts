/*
 * Copyright 2014 Brandon Arp
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

/// <reference path="libs/durandal/durandal.d.ts"/>
/// <amd-dependency path="bootstrap"/>

import system = require('durandal/system');
import app = require('durandal/app');
import viewLocator = require('durandal/viewLocator');

app.title = "ReMet";
app.configurePlugins({
    router: true,
    dialog: true,
    widget: true
});

viewLocator.useConvention('classes', '/assets/html');

app.start().then(function() {
    app.setRoot("../classes/shell");
});
