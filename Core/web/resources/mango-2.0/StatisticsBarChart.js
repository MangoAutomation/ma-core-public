/**
 * Javascript Objects for the Displaying Bar Charts using Statistics values
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @exports mango/statisticsBarChart
 * @module {StatisticsBarChart} mango/statisticsBarChart
 */

define(['jquery'], function($) {
"use strict";

/**
 * @constructs StatisticsBarChartConfiguration
 * @param divId
 * @param dataProviderIds
 * @param mixin
 * @param options
 */
StatisticsBarChartConfiguration = function(divId, dataProviderIds, amChartMixin, mangoChartMixin, options){
    this.divId = divId;
    this.amChartMixin = amChartMixin;
    this.mangoChartMixin = mangoChartMixin;
    this.dataProviderIds = dataProviderIds;
    
    for(var i in options) {
        this[i] = options[i];
    }

    this.configuration = $.extend(true, {}, this.getBaseConfiguration(), this.amChartMixin);
    
    //Ensure we have a balloon function
    for(i=0; i<this.configuration.graphs.length; i++){
        if(typeof this.configuration.graphs[i].balloonFunction == 'undefined')
            this.configuration.graphs[i].balloonFunction = this.balloonFunction;
    }
    
    //Ensure we have a data provider
    if(typeof this.configuration.dataProvider == 'undefined')
        this.configuration.dataProvider = [];
    
};

/**
 * Bar Chart Config
 */
StatisticsBarChartConfiguration.prototype = {
        divId: null, //Div of chart
        
        amChartMixin: null, //Any AmChart JSON configuration to override
        
        mangoChartMixin: null, //Any Mango Serial Chart mixins
        
        configuration: null, //The full config with mixin
       
        dataProviderIds: null, //List of my data provider ids
        
        dataPointMappings: null, //List of Data Point Matching Items (not required)
        
        balloonFunction: function(graphDataItem, amGraph){
            if(typeof graphDataItem.values != 'undefined'){
                return graphDataItem.category + "<br>" + graphDataItem.values.value.toFixed(2);
            }else{
                return "";
            }
        },
        
        /**
         * Displaying Loading... on top of chart div
         */
        chartLoading: function(){
            $('#' + this.divId).html('<b>Loading Chart...</b>');
        },
        
        /**
         * Do the heavy lifting and create the item
         * @return AmChart created
         */
        createDisplay: function(){
            this.chartLoading();
            var serial = new MangoStatisticsBarChart(this.divId,
                    AmCharts.makeChart(this.divId, this.configuration), 
                    this.dataProviderIds, this.dataPointMappings);
            
            return $.extend(true, {}, serial, this.mangoChartMixin);
        },
        
        
        /**
         * Return the base Serial Chart Configuration
         */
        getBaseConfiguration: function(){
            return  {                    
            type: "serial",
            //Note the path to images
            pathToImages: "/modules/dashboards/web/js/amcharts/images/",
            //Set to date field in result data
            categoryField: "xid",
            rotate: true,
            startDuration: 1,
            categoryAxis: {
                gridPosition: "start",
                position: "left"
            },
            trendLines: [],
            chartCursor: {
                "categoryBalloonDateFormat": "JJ:NN:SS"
            },
            "chartScrollbar": {},
            "graphs": [],
            "guides": [],
            "valueAxes": [],
            "allLabels": [],
            "balloon": {},
            legend: {
                useGraphSettings: true,
                /**
                 * Method to render the Legend Values better
                 */
                valueFunction: function(graphDataItem){
                    if(typeof graphDataItem.values != 'undefined')
                        if(typeof graphDataItem.values.value != 'undefined')
                            return graphDataItem.values.value.toFixed(2);
                    
                    return ""; //Otherwise nada
                }
            },
            "titles": []
        };
     }
};


/**
 * @constructs BarChartObject
 * @param amChart
 * @param dataProviderIds
 * @param options
 */
MangoStatisticsBarChart = function(divId, amChart, dataProviderIds, dataPointMappings, options){
    this.divId = divId;
    this.amChart = amChart;
    this.dataProviderIds = dataProviderIds;
    this.categoryField = 'xid'; //How to separate Categories
    this.dataPointMappings = dataPointMappings;
    
    for(var i in options) {
        this[i] = options[i];
    }
};

MangoStatisticsBarChart.prototype = {
        divId: null, //Id of chart div
        seriesValueMapping: null, //Set to 'xid' or 'name' if using multiple series on a chart, otherwise default of 'value' is used
 
        
        /**
         * Data Provider listener to clear data
         */
        onClear: function(){
            while(this.amChart.dataProvider.length >0){
                this.amChart.dataProvider.pop();
            }
            this.amChart.validateData();
        },
        
        /**
         * Data Provider Listener
         * On Data Provider load we add new data
         */
        onLoad: function(statistics, dataPoint){
            
            if(statistics.hasData){
                var entry = {};
                entry.minimum = statistics.minimum.value;
                entry.maximum = statistics.maximum.value;
                entry.average = statistics.average;
                entry.integral = statistics.integral;
                entry.sum = statistics.sum;
                entry.first = statistics.first.value;
                entry.last = statistics.last.value;
                entry.count = statistics.count;
                    
                entry[this.categoryField] = dataPoint[this.categoryField];
                this.amChart.dataProvider.push(entry);
            }

            this.amChart.validateData();
            
        }
};

//make the related sub types accessible through the returned type
//alternatively could make only visible internally or put them in separate files
StatisticsBarChartConfiguration.MangoStatisticsBarChart = MangoStatisticsBarChart;

return StatisticsBarChartConfiguration;

}); // close define