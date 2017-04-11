/**
 * Serial Chart to display Data in a chart.
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {SerialChart} mango/SerialChart
 * @tutorial allDataPointsChart
 * @tutorial dataPointChart
 */
define(['jquery', 'moment-timezone'], function($, moment) {

/**
 * @constructs SerialChart
 * @param {Object} options - options for chart
 * @tutorial allDataPointsChart
 * @tutorial dataPointChart
 */
function SerialChart(options) {
    this.divId = null;
    this.amChart = null;
    this.uniqueAxes = false;
    this.axisLeftRight = true;
    this.axisOffset = 0;
    
    //Bind ourself for access to our zoomDuration
    this.chartZoomed.bind(this);
    this.balloonFunction.bind(this);
    this.labelFunction.bind(this);
    
    $.extend(this, options);
    
    if (!baseConfiguration.categoryAxis.labelFunction)
        baseConfiguration.categoryAxis.labelFunction = this.labelFunction;
    
    this.amChart = $.extend(true, {}, getBaseConfiguration(), this.amChart);
}

/**
 * Is the category Axis date based
 */
SerialChart.prototype.categoryIsDate = true;

/**
 * Create the label for the categoryAxis
 * 
 * @param {string} valueText
 * @param {date} date 
 * @param {Axis} categoryAxis
 * @param {string} periodFormat
 */
SerialChart.prototype.labelFunction = function(valueText, date, categoryAxis, periodFormat) {
	if(categoryAxis.parseDates === true){
	    var formatString;
	    switch (periodFormat) {
	    case 'fff':
	    case 'ss':
	        formatString = 'LTS';
	        break;
	    case 'mm':
	    case 'hh':
	        formatString = 'LT';
	        break;
	    case 'DD':
	    case 'WW':
	        formatString = 'MMM DD';
	        break;
	    case 'MM':
	        formatString = 'MMM';
	        break;
	    case 'YYYY':
	        formatString = 'YYYY';
	        break;
	    }
	    
	    return moment(date).format(formatString);
	}else{
		return date.dataContext[categoryAxis.chart.categoryField].toFixed(2); //Date isn't actually a date in this situation
	}
};

/**
 * Method called when a balloon is shown on the chart
 */
SerialChart.prototype.balloonFunction = function(graphDataItem, amGraph) {
    if (!graphDataItem.values)
        return '';
    
    var label;
    //var dateFormatted = moment(graphDataItem.category).format('lll Z z');
    if(this.categoryIsDate){
	    var duration;
	    if(typeof this.zoomDuration === 'undefined'){
	    	duration = moment.duration(amGraph.data[amGraph.data.length-1].time - amGraph.data[0].time);
	    }else{
	    	//Set if we are zoomed in, via us, the zoom listener
	    	duration = this.zoomDuration;
	    }
	    var formatString;
	    if(duration.years() > 0){
	        formatString = 'YYYY MMM DD LTS';
	    }else if(duration.months() > 0){
	        formatString = 'MMM DD LTS';
	    }else if(duration.days() > 0){
	    	formatString = 'DD LTS';
	    }else{
	        formatString = 'LTS';
	    }
	    var dateFormatted = moment(graphDataItem.category).format(formatString);
	    
	    label = amGraph.title + '<br>' +
	        dateFormatted + "<br><strong>" +
	        graphDataItem.values.value.toFixed(2);
	    
	    if (amGraph.unit) {
	        label += ' ' + amGraph.unit;
	    }
	    
	    label += "</strong>";
    }else{
    	label = amGraph.title + '<br>' +
    	graphDataItem.category +
    	"<br><strong>" + graphDataItem.values.value.toFixed(2);
    	
    	if (amGraph.unit) {
	        label += ' ' + amGraph.unit;
	    }
    }
    
    return label;
};

/**
 * Displaying Loading... on top of chart div
 */
SerialChart.prototype.loading = function() {
    if ($('#' + this.divId + ' .amcharts-main-div').find('div.loading').length > 0)
        return;
    var loadingDiv = $('<div>');
    loadingDiv.addClass('loading');
    loadingDiv.text('Loading Chart...');
    $('#' + this.divId + ' .amcharts-main-div').prepend(loadingDiv);
};

/**
 * Remove the loading display
 */
SerialChart.prototype.removeLoading = function() {
    $('#' + this.divId + ' .amcharts-main-div').find('div.loading').remove();
};

/**
 * Event when chart has zoomed
 * @param zoomEvent
 */
SerialChart.prototype.chartZoomed = function(zoomEvent){
	if(this.categoryIsDate === true)
		this.zoomDuration = moment.duration(zoomEvent.endDate.getTime() - zoomEvent.startDate.getTime());
};


/**
 * Do the heavy lifting and create the item
 * @return AmChart created
 */
SerialChart.prototype.createDisplay = function() {
    var self = this;
    var deferred = $.Deferred();
    
    require(['amcharts/serial'], function() {
        self.amChart = AmCharts.makeChart(self.divId, self.amChart);
        self.amChart.addListener('zoomed', self.chartZoomed);
        deferred.resolve(self);
    });
    
    return deferred.promise();
};


/**
 * Data Provider listener to clear data
 */
SerialChart.prototype.onClear = function() {
    this.removeLoading();
    
    while (this.amChart.dataProvider.length > 0) {
        this.amChart.dataProvider.pop();
    }
    
    while (this.amChart.graphs.length > 0) {
        var graph = this.amChart.graphs[0];
        this.amChart.removeGraph(graph);
    }
    
    // leave first auto axis
    while (this.amChart.valueAxes.length > 1) {
        var axis = this.amChart.valueAxes[1];
        this.amChart.removeValueAxis(axis);
    }
    
    // start creating axes at default positions
    this.axisLeftRight = true;
    this.axisOffset = 0;
    
    this.amChart.validateData();
};

/**
 * Data Provider Listener
 * On Data Provider load we add new data
 */
SerialChart.prototype.onLoad = function(data, dataPoint) {
    this.removeLoading();
    
    var valueField = this.valueFieldForPoint(dataPoint);
    var fromField = this.fromFieldForPoint(dataPoint);
    
    var graphId = this.graphId(valueField, dataPoint);
    var graph = this.findGraph(graphId) || this.createGraph(valueField, dataPoint);
    
    var dataProvider = this.amChart.dataProvider;
    if(this.categoryIsDate === true){
	    for (i = 0; i < data.length; i++) {
	        var dataItem = data[i];
	        var date = this.dataItemToDate(dataItem, dataPoint);
	        
	        // look for existing item with same date, makes cursor behave nicely
	        var existing = null;
	        for (var j = 0; j < dataProvider.length; j++) {
	            if (dataProvider[j].date.valueOf() === date.valueOf()) {
	                existing = dataProvider[j];
	                break;
	            }
	        }
	        
	        var value = dataItem[fromField];
	        if (typeof this.manipulateValue === 'function')
	            value = this.manipulateValue(value, dataPoint);
	        
	        // if it exists then update the item, otherwise insert new item
	        if (existing) {
	            existing[valueField] = value;
	        }
	        else {
	            var entry = {};
	            entry[valueField] = value;
	            entry.date = date;
	            dataProvider.push(entry);
	        }
	    }
	    
	    // sort the data as items could have been pushed to end of array
	    // not needed if categoryAxis.parseDates === true
	    dataProvider.sort(this.sortCompare);
    }else{
    	//Not date based
    	for (i = 0; i < data.length; i++) {
	        var val = data[i][fromField];
	        var category = data[i][this.amChart.categoryField];
	        var ent = {};
            ent[valueField] = val;
            ent[this.amChart.categoryField] = category;
            dataProvider.push(ent);
    	}
    }

};

/**
 * Loading data failed
 */
SerialChart.prototype.loadPointFailed = function(errorObject) {
    this.removeLoading();
};

/**
 * Redraw the chart without reloading data
 */
SerialChart.prototype.redraw = function() {
    this.amChart.validateData();
};

/**
 * Find a graph with the provided ID
 * @param {string} graphId
 */
SerialChart.prototype.findGraph = function(graphId) {
    for (var i = 0; i < this.amChart.graphs.length; i++) {
        var graph = this.amChart.graphs[i];
        if (graph.id === graphId) {
            return graph;
        }
    }
};

/**
 * Create a Graph
 */
SerialChart.prototype.createGraph = function(valueField, dataPoint) {
    var graph = new AmCharts.AmGraph();
    graph.valueField = valueField;
    graph.type = this.graphType(valueField, dataPoint);
    graph.title = this.graphTitle(valueField, dataPoint);
    graph.id = this.graphId(valueField, dataPoint);
    graph.balloonFunction = this.balloonFunction;
    graph.lineColor = dataPoint.chartColour;
    
    if (this.uniqueAxes === true) {
        var axisId = this.axisId(valueField, dataPoint);
        
        // find existing axis, it it doesn't exist create one
        graph.valueAxis = this.findAxis(axisId) || this.createAxis(graph, valueField, dataPoint);
    }
    //Allow easy addition of graph properties
    this.addGraphProperties(valueField, dataPoint, graph);
    
    this.amChart.addGraph(graph);
    return graph;
};

/**
 * Allow easy addition of properties to graph
 * @param valueField - value field for data point
 * @param dataPoint - data point for graph
 * @param graph - graph to modify
 */
SerialChart.prototype.addGraphProperties = function(valueField, dataPoint, graph) {

};

/**
 * Return the graph type
 * @returns {string} - defaults to 'smoothedLine'
 */
SerialChart.prototype.graphType = function(valueField, dataPoint) {
    return 'smoothedLine';
};

/**
 * Return the title for the chart
 * @param valueField
 * @param {Object} dataPoint
 * @return {string} - defaults to dataPoint.name
 */
SerialChart.prototype.graphTitle = function(valueField, dataPoint) {
    return dataPoint.name;
};

/**
 * Return the graphID
 * @param valueField
 * @param {Object} dataPoint
 * @return {string} - defaults to dataPoint.xid
 */
SerialChart.prototype.graphId = function(valueField, dataPoint) {
    return dataPoint.xid;
};

/**
 * Find the axis with the provided id
 * @param {string} axisId
 */
SerialChart.prototype.findAxis = function(axisId) {
    for (var i = 0; i < this.amChart.valueAxes.length; i++) {
        var axis = this.amChart.valueAxes[i];
        if (axis.id === axisId) {
            return axis;
        }
    }
};

/**
 * Create an axis
 */
SerialChart.prototype.createAxis = function(graph, valueField, dataPoint) {
    var axis = new AmCharts.ValueAxis();
    axis.id = this.axisId(valueField, dataPoint);
    axis.title = this.axisTitle(valueField, dataPoint);
    
    axis.position = this.axisLeftRight ? "left" : "right";
    axis.offset = this.axisOffset;
    
    // only display grid for first axis
    if (this.amChart.valueAxes.length > 1)
        axis.gridAlpha = 0;
    
    if (!this.axisLeftRight)
        this.axisOffset += 50;
    this.axisLeftRight = !this.axisLeftRight;
    
    this.amChart.addValueAxis(axis);
    return axis;
};

SerialChart.prototype.axisTitle = function(valueField, dataPoint) {
    return dataPoint.name;
};

SerialChart.prototype.axisId = function(valueField, dataPoint) {
    return dataPoint.xid;
};

SerialChart.prototype.valueFieldForPoint = function(dataPoint) {
    return dataPoint.xid;
};

SerialChart.prototype.fromFieldForPoint = function(dataPoint) {
    return 'value';
};

SerialChart.prototype.dataItemToDate = function(dataItem, dataPoint) {
    return new Date(dataItem.timestamp);
};

SerialChart.prototype.sortCompare = function(a, b) {
    return a.date - b.date;
};

var baseConfiguration = {
        type: "serial",
        addClassNames: true,
        dataProvider: [],
        //Note the path to images
        pathToImages: "/resources/amcharts/images/",
        categoryField: "date",
        categoryAxis: {
            parseDates: true,
            minPeriod: "mm",
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

return SerialChart;

}); // define
