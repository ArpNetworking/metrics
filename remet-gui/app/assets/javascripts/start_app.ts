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
/// <reference path="libs/requirejs/require.d.ts"/>

requirejs.config({
    deps: ["remet_app"],
    paths : {
        'bean' : '/assets/lib/bean/bean.min', //Required by flotr2
        'bootstrap' : '/assets/lib/bootstrap/js/bootstrap.min',
        'd3' : '/assets/lib/d3js/d3.min',
        'gauge' : 'gauge.min',
        'naturalSort' : 'naturalSort',

        //Durandal
        'durandal/activator': '/assets/lib/durandal/js/activator',
        'durandal/app': '/assets/lib/durandal/js/app',
        'durandal/binder': '/assets/lib/durandal/js/binder',
        'durandal/composition': '/assets/lib/durandal/js/composition',
        'durandal/events': '/assets/lib/durandal/js/events',
        'durandal/system': '/assets/lib/durandal/js/system',
        'durandal/transitions/entrance': '/assets/lib/durandal/js/transitions/entrance',
        'durandal/viewEngine': '/assets/lib/durandal/js/viewEngine',
        'durandal/viewLocator': '/assets/lib/durandal/js/viewLocator',
        'plugins/dialog': '/assets/lib/durandal/js/plugins/dialog',
        'plugins/history': '/assets/lib/durandal/js/plugins/history',
        'plugins/http': '/assets/lib/durandal/js/plugins/http',
        'plugins/observable': '/assets/lib/durandal/js/plugins/observable',
        'plugins/router': '/assets/lib/durandal/js/plugins/router',
        'plugins/serializer': '/assets/lib/durandal/js/plugins/serializer',
        'plugins/widget': '/assets/lib/durandal/js/plugins/widget',

        'flotr2' : '/assets/lib/flotr2/flotr2.amd',
        'jquery' : '/assets/lib/jquery/jquery.min',
        'jquery.ui' : '/assets/lib/jquery-ui/jquery-ui.min',
        'jqrangeslider' : '/assets/lib/jQRangeSlider/jQAllRangeSliders-withRuler-min',
        'knockout' : '/assets/lib/knockout/knockout',
        'text' : '/assets/lib/requirejs-text/text', //Required by durandal
        'typeahead' : '/assets/lib/typeaheadjs/typeahead.bundle',
        'underscore' : '/assets/lib/underscorejs/underscore-min' //Required by flotr2
    },
    shim : {
        'remet_app' : {
            deps : [ 'text', 'jquery', 'jquery.ui', 'bean', 'underscore', 'jqrangeslider',
                    'bootstrap', 'd3', 'gauge', 'knockout', 'classes/KnockoutBindings', 'classes/GraphViewModel']
        },
        'jquery.ui' : {
            deps : [ 'jquery' ]
        },
        'jqrangeslider' : {
            deps : [ 'jquery.ui' ]
        },
        'bootstrap' : {
            deps : [ 'jquery' ]
        }
    }
});
