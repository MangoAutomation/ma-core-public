/**
 * Create a base object with the amExport member set for export
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {exportConfig} mango/exportConfig
 * @see exportConfig
 */
define(['amcharts/exporting/amexport', 'amcharts/exporting/canvg', 'amcharts/exporting/rgbcolor', 'amcharts/exporting/filesaver',
        'amcharts/exporting/jspdf', 'amcharts/exporting/jspdf.plugin.addimage'], function() {

/**
 * 
 * @member {Object} AmExport Settings
 */
var amExport = {
        top         : 0,
        right       : 0,
        exportJPG   : true,
        exportPNG   : true,
        exportSVG   : true,
        exportPFG   : true
};

var amChart = {
    amExport: amExport
};

return amChart;

});
