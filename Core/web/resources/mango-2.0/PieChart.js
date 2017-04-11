/**
 * Pie Chart to display Data in a chart.
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 * @module {PieChart} mango/PieChart
 * @tutorial dataPointsPieChart
 */
define(['jquery', 'moment-timezone', './SerialChart'], function($, moment, SerialChart) {

/**
 * @constructs PieChart
 * @param {Object} options - options for chart
 * @augments SerialChart
 * @tutorial dataPointsPieChart
 */
function PieChart(options) {
	SerialChart.apply(this, arguments);
	this.amChart = {}; //Clear our Serial Chart configuration
    //Bind ourself for access to our zoomDuration
    this.chartZoomed.bind(this);
    this.balloonFunction.bind(this);
    
    $.extend(this, options);
    
    this.amChart = $.extend(true, {}, this.getBaseConfiguration(), this.amChart);
}

PieChart.prototype = Object.create(SerialChart.prototype);

/**
 * Title field in Pie Chart
 */
PieChart.prototype.titleField = 'name';

/**
 * Method called when a balloon is shown on the chart
 */
PieChart.prototype.balloonFunction = function(graphDataItem, amGraph) {
    if (!graphDataItem.values)
        return '';
    else
    	return "<b>" + graphDataItem.title +  "</b><br>" + graphDataItem.value.toFixed(2) + " (" + graphDataItem.percents.toFixed(2) + "%)";
};


/**
 * Event when chart has zoomed
 * @param zoomEvent
 */
PieChart.prototype.chartZoomed = function(zoomEvent){
	/*No Op*/
};

/**
 * Value Field for Pie
 * @param dataPoint
 */
PieChart.prototype.valueFieldForPoint = function(dataPoint) {
    return 'total';
};

/**
 * Get the Title for the Slice, defaults to using Name
 * @param dataPoint
 */
PieChart.prototype.titleFieldForPoint = function(dataPoint) {
    return dataPoint.name;
};

/**
 * Do the heavy lifting and create the item
 * @return AmChart created
 */
PieChart.prototype.createDisplay = function() {
    var self = this;
    var deferred = $.Deferred();
    
    require(['amcharts/pie'], function() {
        self.amChart = AmCharts.makeChart(self.divId, self.amChart);
        deferred.resolve(self);
    });
    
    return deferred.promise();
};

/**
 * Data Provider listener to clear data
 */
PieChart.prototype.onClear = function() {
    this.removeLoading();
    
    while (this.amChart.dataProvider.length > 0) {
        this.amChart.dataProvider.pop();
    }

    this.amChart.validateData();
};

/**
 * Data Provider Listener
 * On Data Provider load we add new data
 */
PieChart.prototype.onLoad = function(data, dataPoint) {
    this.removeLoading();
    
    var titleField = this.titleFieldForPoint(dataPoint);
    var valueField = this.valueFieldForPoint(dataPoint);

    var sliceValue = this.computeSliceValue(data, dataPoint);
    
    //Check to see if it already exists in the chart
    for(i=0; i<this.amChart.dataProvider.length; i++){
        if(this.amChart.dataProvider[i][this.titleField] == titleField){
            this.amChart.dataProvider[i][valueField] = sliceValue;
            return; //Done
        }
    }
    //We didn't find our set, so add a brand new one
    var entry = {};
    entry[valueField] = sliceValue;
    entry[this.titleField] = titleField;
    this.amChart.dataProvider.push(entry);       
};

/**
 * Redraw the chart without reloading data
 */
PieChart.prototype.redraw = function() {
    this.amChart.validateData();
    this.amChart.animateAgain(); //TODO Not sure why this is required...
};

/**
 * Compute the Slice Value - Defaults to computing the total
 * @param data - Array of point values
 */
PieChart.prototype.computeSliceValue = function(data, dataPoint){
	var sliceValue = 0;
	var fromField = this.fromFieldForPoint(dataPoint);
	for(var i=0; i<data.length; i++){
		sliceValue += data[i][fromField];
	}
	return sliceValue;
};

/**
 * Return the graph type
 * @returns {string} - defaults to 'column'
 */
PieChart.prototype.graphType = function(valueField, dataPoint) {
    return 'pie';
};

/**
 * TODO REMOVE THIS
 * Create a Graph
 */
PieChart.prototype.createGraph = function(valueField, dataPoint) {
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

/**
 * Return the base Pie Chart Configuration
 */
PieChart.prototype.getBaseConfiguration = function() {
    return {
    	startEffect: "easeOutSine",
    	startDuration: 1,
    	sequencedAnimation: true,
    	startRadius: "150%",
        type: "pie",
        dataProvider: [],
        //Note the path to images
        pathToImages: "/resources/amcharts/images/",
        titleField: "name",
        valueField: "total",
        allLabels: [],
    	balloon: {},
        titles: []
    };
};

return PieChart;

}); // define
