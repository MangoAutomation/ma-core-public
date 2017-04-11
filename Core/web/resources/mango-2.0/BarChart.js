/**
 * Bar Chart to display Data in a chart.
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 * @module {BarChart} mango/BarChart
 * @tutorial dataPointBarChart
 */
define(['jquery', 'moment-timezone', './SerialChart'], function($, moment, SerialChart) {

/**
 * @constructs BarChart
 * @param {Object} options - options for chart
 * @augments SerialChart
 * @tutorial dataPointBarChart
 */
function BarChart(options) {
	SerialChart.apply(this, arguments);
	
    //Bind ourself for access to our zoomDuration
    this.chartZoomed.bind(this);
    this.balloonFunction.bind(this);
    
    $.extend(this, options);
    
    this.amChart = $.extend(true, {}, getBaseConfiguration(), this.amChart);
}

BarChart.prototype = Object.create(SerialChart.prototype);

/**
 * Fill alpha for bars
 */
BarChart.prototype.fillAlphas = 0.8;

/**
 * Alpha for grid
 */
BarChart.prototype.gridAlpha = 0.2;


/**
 * Method called when a balloon is shown on the chart
 */
BarChart.prototype.balloonFunction = function(graphDataItem, amGraph) {
    if (!graphDataItem.values)
        return '';
    else
        return graphDataItem.category + "<br>" + graphDataItem.values.value.toFixed(2);
};


/**
 * Event when chart has zoomed
 * @param zoomEvent
 */
BarChart.prototype.chartZoomed = function(zoomEvent){
	/*No Op*/
};


/**
 * Data Provider Listener
 * On Data Provider load we add new data
 */
BarChart.prototype.onLoad = function(data, dataPoint) {
    this.removeLoading();
    
    var valueField = this.valueFieldForPoint(dataPoint);
    var fromField = this.fromFieldForPoint(dataPoint);
    
    var graphId = this.graphId(valueField, dataPoint);
    var graph = this.findGraph(graphId) || this.createGraph(dataPoint.xid, dataPoint);
    
    var dataProvider = this.amChart.dataProvider;
    var exists = false;
    for (var j = 0; j < dataProvider.length; j++) {
    	if(dataProvider[j].xid === dataPoint.xid){
    		dataProvider[j][dataPoint.xid] = data.value;
    		exists = true;
    	}
    }
    if(exists === false){
    	data.xid = dataPoint.xid;
    	dataProvider.push(data);
    }
    this.amChart.validateData();
};

/**
 * Return the graph type
 * @returns {string} - defaults to 'column'
 */
BarChart.prototype.graphType = function(valueField, dataPoint) {
    return 'column';
};

/**
 * Create a Graph
 */
BarChart.prototype.createGraph = function(valueField, dataPoint) {
    var graph = new AmCharts.AmGraph();
    graph.valueField = valueField;
    graph.type = this.graphType(valueField, dataPoint);
    graph.title = this.graphTitle(valueField, dataPoint);
    graph.id = this.graphId(valueField, dataPoint);
    graph.balloonFunction = this.balloonFunction;
    graph.fillAlphas = this.fillAlphas;
    graph.gridAlpha = this.gridAlpha;
    
    if (this.uniqueAxes) {
        var axisId = this.axisId(valueField, dataPoint);
        
        // find existing axis, it it doesn't exist create one
        graph.valueAxis = this.findAxis(axisId) || this.createAxis(graph, valueField, dataPoint);
    }
    
    this.amChart.addGraph(graph);
    return graph;
};

var baseConfiguration = {
        type: "serial",
        addClassNames: true,
        dataProvider: [],
        //Note the path to images
        pathToImages: "/resources/amcharts/images/",
        categoryField: 'xid',
        categoryAxis: {
        	gridPosition: 'start',
        	tickPosition: 'start',
            parseDates: false,
            labelRotation: 45,
            boldPeriodBeginning: false,
            markPeriodChange: false,
            equalSpacing: true
        },
        chartScrollbar: {},
        trendLines: [],
        chartCursor: {},
        graphs: [],
        guides: [],
        valueAxes: [],
        allLabels: [],
        balloon: {},
        legend: {
            useGraphSettings: true,
            /**
             * Method to render the Legend Values better
             */
            valueFunction: function(graphDataItem) {
                if(graphDataItem.values && graphDataItem.values.value)
                    return graphDataItem.values.value.toFixed(2);

                return ""; //Otherwise nada
            }
        },
        titles: []
};

/**
 * Return the base Serial Chart Configuration
 */
function getBaseConfiguration() {
    return baseConfiguration;
}

return BarChart;

}); // define
