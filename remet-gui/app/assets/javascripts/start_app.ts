/// <reference path="libs/requirejs/require.d.ts"/>

requirejs.config({
    deps: ["remet_app"],
    paths : {
        'bean' : 'bean', //Required by flotr2
        'bootstrap' : '/assets/lib/bootstrap/js/bootstrap.min',
        'd3' : '/assets/lib/d3js/d3.min',
        'gauge' : 'gauge.min',
        'flotr2' : '/assets/lib/flotr2/flotr2.amd',
        'jquery' : '/assets/lib/jquery/jquery.min',
        'jquery.ui' : '/assets/lib/jquery-ui/jquery-ui.min',
        'jqrangeslider' : 'jqrangeslider/jQAllRangeSliders-withRuler-min',
        'knockout' : '/assets/lib/knockout/knockout',
        'rickshaw' : '/assets/lib/rickshaw/rickshaw.min',
        'text' : '/assets/lib/requirejs-text/text', //Required by durandal
        'underscore' : '/assets/lib/underscorejs/underscore-min' //Required by flotr2
    },
    shim : {
        'remet_app' : {
            deps : [ 'text', 'jquery', 'jquery.ui', 'bean', 'underscore', 'jqrangeslider',
                    'bootstrap', 'd3', 'gauge', 'rickshaw' ]
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
