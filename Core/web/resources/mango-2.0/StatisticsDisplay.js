/**
 * @copyright 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */

define(['jquery', 'moment-timezone'], function($, moment) {
"use strict";

/**
 * Configuration for StatisticsDisplay
 * @constructor
 * @param options
 */
var StatisticsDisplay = function(options) {
    this.container = $('body');
    
    $.extend(this, options);
};

/**
 * Serial Chart Config
 */
StatisticsDisplay.prototype = {
    containerPerPoint: false,
    decimalPlaces: 2,
    separateValueAndTime: false,
    
    /**
     * Do the heavy lifting and create the item
     * @return AmChart created
     */
    createDisplay: function() {
        return this;
    },

    /**
     * Data Provider listener to clear data
     */
    onClear: function() {
        var $all = this.container.find('.minimum, .maximum, .average, .integral, .sum, .first, .last, .count');
        if (this.separateValueAndTime) {
            $all.hide().find('.value, .time, .value-time').text('');
        } else {
            $all.hide().text('');
        }
        
        $('.starts-and-runtimes').hide().find('tbody').empty();
    },
    
    containerForPoint: function(dataPoint) {
        return this.container.find('.point-' + dataPoint.xid);
    },
    
    /**
     * Data Provider Listener
     * On Data Provider load we add new data
     */
    onLoad: function(data, dataPoint) {
        this.removeLoading();
        
        if (!data.hasData) {
            this.container.find('.no-data').show();
        }
        
        var container = this.containerPerPoint ? this.containerForPoint(dataPoint) : this.container;
        
        var minimum = container.find('.minimum');
        var maximum = container.find('.maximum');
        var average = container.find('.average');
        var integral = container.find('.integral');
        var sum = container.find('.sum');
        var first = container.find('.first');
        var last = container.find('.last');
        var count = container.find('.count');
        
        if (this.separateValueAndTime) {
            if (data.minimum) {
                minimum.find('.value').text(this.renderValue(data.minimum.value));
                minimum.find('.time').text(this.renderTime(data.minimum.timestamp));
                minimum.find('.value-time').text(this.renderPointValueTime(data.minimum));
                minimum.show();
            }
            
            if (data.maximum) {
                maximum.find('.value').text(this.renderValue(data.maximum.value));
                maximum.find('.time').text(this.renderTime(data.maximum.timestamp));
                maximum.find('.value-time').text(this.renderPointValueTime(data.maximum));
                maximum.show();
            }
            
            if (data.first) {
                first.find('.value').text(this.renderValue(data.first.value));
                first.find('.time').text(this.renderTime(data.first.timestamp));
                first.find('.value-time').text(this.renderPointValueTime(data.first));
                first.show();
            }
            
            if (data.last) {
                last.find('.value').text(this.renderValue(data.last.value));
                last.find('.time').text(this.renderTime(data.last.timestamp));
                last.find('.value-time').text(this.renderPointValueTime(data.last));
                last.show();
            }
            
            if (data.average) {
                average.find('.value').text(this.renderValue(data.average.value));
                average.show();
            }

            if (data.integral) {
                integral.find('.value').text(this.renderValue(data.integral.value));
                integral.show();
            }

            if (data.sum) {
                sum.find('.value').text(this.renderValue(data.sum.value));
                sum.show();
            }

            if (data.count) {
                count.find('.value').text(this.renderCount(data.count));
                count.show();
            }
        }
        else {
            minimum.text(this.renderPointValueTime(data.minimum)).show();
            maximum.text(this.renderPointValueTime(data.maximum)).show();
            first.text(this.renderPointValueTime(data.first)).show();
            last.text(this.renderPointValueTime(data.last)).show();
            average.text(this.renderValue(data.average.value)).show();
            integral.text(this.renderValue(data.integral.value)).show();
            sum.text(this.renderValue(data.sum.value)).show();
            count.text(this.renderValue(data.count)).show();
        }
        
        if (data.startsAndRuntimes) {
            var $startsAndRuntimes = container.find('.starts-and-runtimes');
            this.renderStartsAndRuntimes($startsAndRuntimes, data.startsAndRuntimes, dataPoint);
            $startsAndRuntimes.show();
        }
    },
    
    renderPointValueTime: function(pvt) {
       return this.renderValue(pvt.value) + ' @ ' + this.renderTime(pvt.timestamp);  
    },
    
    renderValue: function(value) {
        if (typeof value === 'number') return value.toFixed(this.decimalPlaces);
        return value;
    },
    
    renderCount: function(value) {
        if (typeof value === 'number') return value.toFixed(0);
        return value;
    },
    
    renderTime: function(timestamp) {
        return moment(timestamp).format('lll');
    },
    
    renderStartsAndRuntimes: function($startsAndRuntimes, data, dataPoint) {
        var columns = [];
        
        $startsAndRuntimes.find('thead tr:first-child').children('th').each(function(i, th) {
            if ($(th).hasClass('value')) {
                columns.push('value');
            } else if ($(th).hasClass('starts')) {
                columns.push('starts');
            } else if ($(th).hasClass('runtime')) {
                columns.push('runtime');
            } else if ($(th).hasClass('proportion')) {
                columns.push('proportion');
            } else {
                columns.push(null);
            }
        });
        
        for (var i = 0; i < data.length; i++) {
            var $tbody = $startsAndRuntimes.find('tbody');
            var $tr = $('<tr>').appendTo($tbody);
            for (var j = 0; j < columns.length; j++) {
                var $td = $('<td>').appendTo($tr);
                if (columns[j]) {
                    this.renderCell($td, columns[j], data[i], dataPoint);
                }
            }
        }
    },
    
    renderCell: function($td, cssClass, rowData, dataPoint) {
        $td.addClass(cssClass);
        
        var text = null;
        switch(cssClass) {
        case 'value':
            text = this.renderMultistateValue(rowData.value, dataPoint);
            break;
        case 'starts':
            text = this.renderCount(rowData.starts);
            break;
        case 'runtime':
            text = this.renderRuntime(rowData.runtime);
            break;
        case 'proportion':
            text = this.renderProportion(rowData.proportion);
            break;
        }
        if (text) $td.html(text);
    },
    
    renderMultistateValue: function(value, dataPoint) {
    	
    	switch(dataPoint.textRenderer.type){
	  		case 'textRendererBinary':
	  			if(value === true)
	  				return '<span style="color:' + dataPoint.textRenderer.oneColour + '">' + dataPoint.textRenderer.oneLabel + '</span>';
	  			else if(value === false)
	  				return 	'<span style="color:' + dataPoint.textRenderer.zeroColour + '">' + dataPoint.textRenderer.zeroLabel + '</span>';
	  		break;
	  		case 'textRendererMultistate':
	  			for(var i=0; i<dataPoint.textRenderer.multistateValues.length; i++){
	  				var mValue = dataPoint.textRenderer.multistateValues[i];
	  				if(value === mValue.key)
	  					return '<span style="color:' + mValue.colour + '">' + mValue.text + '</span>';
	  			}
	  		break;
		}
    	
        if (typeof value === 'boolean') {
            return value ? '1' : '0';
        }
        if (typeof value === 'number') {
            return value.toFixed(0);
        }
        return value;
    },
    
    renderRuntime: function(runtime) {
        return moment.duration(runtime).humanize();
    },
    
    renderProportion: function(proportion) {
        return (proportion * 100).toFixed(2) + ' %';
    },
    
    loading: function() {
        this.container.find('.loading').show();
        this.container.find('.no-data').hide();
    },
    
    removeLoading: function() {
        this.container.find('.loading').hide();
    }
};

return StatisticsDisplay;

}); // close define