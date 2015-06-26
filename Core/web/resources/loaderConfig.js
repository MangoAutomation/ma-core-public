/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */

(function(root){

var loader;

var scriptTags = document.getElementsByTagName('script');
for (var i = scriptTags.length - 1; i >= 0; i--) {
    var script = scriptTags[i];
    if (script.getAttribute('src') === '/resources/loaderConfig.js') {
        loader = script.getAttribute('data-loader') || 'RequireJS';
        break;
    }
}

var config = {
    baseUrl : '/resources',
    paths: {
        'mango': '/mango-javascript/mango-2.0',
        'mango-1.0': '/modules/dashboards/web/js/mango/v1',
        'mango-1.1': '/modules/dashboards/web/js/mango-1.1',
        'mango-2.0': '/mango-javascript/mango-2.0',
        'mango/mobile': '/resources/mango/mobile',
        'jquery': 'jquery/jquery-1.11.2.min',
        'jquery-ui/jquery-ui': 'jquery-ui/jquery-ui.min',
        'amcharts.funnel'   : 'amcharts/funnel',
        'amcharts.gauge'    : 'amcharts/gauge',
        'amcharts.pie'      : 'amcharts/pie',
        'amcharts.radar'    : 'amcharts/radar',
        'amcharts.serial'   : 'amcharts/serial',
        'amcharts.xy'       : 'amcharts/xy',
        'bootstrap': 'bootstrap/js/bootstrap.min',
        'moment': 'moment-with-locales.min',
        'moment-timezone': 'moment-timezone-with-data.min',
        'es5-shim': 'es5-shim.min',
        'jstz': 'jstz-1.0.4.min',
        'jquery.mousewheel': 'jquery.mousewheel.min',
        'jquery.select2': 'select2/js/select2.full.min',
        'jquery.notify': 'notify-combined.min'
    },
    shim: {
        "bootstrap" : {
            "deps" : ['jquery']
        },
        'amcharts.funnel'   : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts.gauge'    : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts.pie'      : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts.radar'    : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts.serial'   : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts.xy'       : {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts',
            init: function() {
                AmCharts.isReady = true;
            }
        },
        'amcharts/exporting/amexport': {
            deps: ['amcharts/amcharts'],
            exports: 'AmCharts'
        },
        'amcharts/exporting/filesaver': {
            deps: ['amcharts/amcharts']
        },
        'amcharts/exporting/jspdf.plugin.addimage': {
            deps: ['amcharts/exporting/jspdf']
        },
        'jquery.mousewheel': {"deps" : ['jquery']},
        'jquery.select2': {"deps" : ['jquery']},
        'jquery.notify': {"deps" : ['jquery']},
        'jquery-ui/jquery-ui': {"deps" : ['jquery']}
    },
    map: {
        '*': {
            'dgrid': 'dgrid-0.4'
        }
    }
};

if (loader === 'RequireJS') {
    config.paths.dojo = 'amd/dojo';
    config.paths.dijit = 'amd/dijit';
    config.paths.dojox = 'amd/dojox';
    
    // export require to global scope
    root.require = config;
}
else if (loader === 'Dojo') {
    config.tlmSiblingOfDojo = false;
    // load jquery before anything else so we can put it in noConflict mode
    config.deps = ['jquery'];
    config.callback = function($) {
        // remove $ from the global scope, jQuery global is still available
        // $ is defined by DWR and is used in Mango legacy scripts
        $.noConflict();
    };
    
    // export dojoConfig to global scope
    root.dojoConfig = config;
}

})(this); // execute anonymous function
