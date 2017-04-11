/**
 * Gauge Chart to display Data on a gauge/dial.
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 * @module {GaugeChart} mango/GaugeChart
 * @tutorial pointValueGauge
 */
define(['jquery', 'moment-timezone'], function($, moment) {

	/**
	 * @constructs GaugeChart
	 * @param {Object} options - options for chart
	 * @tutorial pointValueGauge
	 */
	function GaugeChart(options) {
	    this.divId = null;
	    this.amChart = null;
	    
	    //Bind ourself for access to our zoomDuration
	    //TODO this.balloonFunction.bind(this);
	    
	    $.extend(this, options);
	    
	    this.amChart = $.extend(true, {}, getBaseConfiguration(), this.amChart);
	}


	/**
	 * Displaying Loading... on top of chart div
	 */
	GaugeChart.prototype.loading = function() {
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
	GaugeChart.prototype.removeLoading = function() {
	    $('#' + this.divId + ' .amcharts-main-div').find('div.loading').remove();
	};
	
	/**
	 * Do the heavy lifting and create the item
	 * @return AmChart created
	 */
	GaugeChart.prototype.createDisplay = function() {
	    var self = this;
	    var deferred = $.Deferred();
	    
	    require(['amcharts/gauge'], function() {
	        self.amChart = AmCharts.makeChart(self.divId, self.amChart);
	        deferred.resolve(self);
	    });
	    
	    return deferred.promise();
	};
	
	/**
	 * Data Provider listener to clear data
	 */
	GaugeChart.prototype.onClear = function() {
	    this.removeLoading();
	    if(this.amChart.arrows[0].setValue){
	        this.amChart.arrows[0].setValue(0);
	        this.amChart.axes[0].setBottomText("");
	    }
	};
	
	/**
	 * Data Provider Listener
	 * On Data Provider load we add new data
	 */
	GaugeChart.prototype.onLoad = function(data, dataPoint) {
		if(this.amChart.arrows[0].setValue){
	        this.amChart.arrows[0].setValue(data.value);
	        this.amChart.axes[0].setBottomText(data.renderedValue);
		}
	};
	
	/**
	 * Redraw the chart without reloading data
	 */
	GaugeChart.prototype.redraw = function() {
	    this.amChart.validateData();
	};
	
	var baseConfiguration = {                    
            type: "gauge",
            pathToImages: "/resources/amcharts/images/",
            marginBottom: 20,
            marginTop: 40,
            fontSize: 13,
            theme: "dark",
            arrows: [
                {
                    id: "GaugeArrow-1",
                    value: 0
                }
            ],
            axes: [
                {
                    axisThickness: 1,
                    bottomText: "",
                    bottomTextYOffset: -20,
                    endValue: 220,
                    id: "GaugeAxis-1",
                    valueInterval: 10,
                    bands: [
                        {
                            alpha: 0.7,
                            color: "#00CC00",
                            endValue: 90,
                            id: "GaugeBand-1",
                            startValue: 0
                        },
                        {
                            alpha: 0.7,
                            color: "#ffac29",
                            endValue: 130,
                            id: "GaugeBand-2",
                            startValue: 90
                        },
                        {
                            alpha: 0.7,
                            color: "#ea3838",
                            endValue: 220,
                            id: "GaugeBand-3",
                            innerRadius: "95%",
                            startValue: 130
                        }
                    ]
                }
            ],
            allLabels: [],
            balloon: {},
            titles: []
    };
	
	
	/**
	 * Return the base Gauge Chart Configuration
	 */
	function getBaseConfiguration() {
	    return baseConfiguration;
	}
	
	return GaugeChart;
});