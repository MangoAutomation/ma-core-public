/**
 * Data Provider for RealTime Updates Via Web Sockets
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {AccumulatorRollupDataProvider} mango/AccumulatorRollupDataProvider
 * @augments PointValueDataProvider
 */
define(['jquery', './DataProvider', './PointValueDataProvider', 'moment-timezone'],
        function($, DataProvider, PointValueDataProvider, moment) {
"use strict";

/**
 * Constructor
 * @constructs AccumulatorRollupDataProvider
 * @param {!number|string} id - Data Provider ID
 * @param {Object} options - Options for provider
 * @augments PointValueDataProvider
 */
function AccumulatorRollupDataProvider() {
    PointValueDataProvider.apply(this, arguments);
}

AccumulatorRollupDataProvider.prototype = Object.create(PointValueDataProvider.prototype);

/**
 * Type of Data Provider
 * @type {string}
 * @default 'AccumulatorRollupDataProvider'
 * @const
 */
AccumulatorRollupDataProvider.prototype.type = 'AccumulatorRollupDataProvider';

/**
 * Needs to load Values?
 * @param {Object} changedOptions
 */
AccumulatorRollupDataProvider.prototype.needsToLoad = function(changedOptions) {
    if (changedOptions.from || changedOptions.to ||
            changedOptions.timePeriodType || changedOptions.timePeriods)
        return true;
    return false;
};

/**
 * Method that is called before publishing the data to the Displays
 * it is here that we can modify the data
 * @param {Array|Object} pointValues - Value(s) to manipulate
 * @param {DataPoint} dataPoint - Data Point of corresponding data
 */
AccumulatorRollupDataProvider.prototype.manipulateData = function(pointValues, dataPoint) {
    var newData = [];
    if (pointValues.length === 0)
        return newData;
    
    var previous = pointValues[0];
    
    //Subtract previous value from current.
    for (var i = 1; i < pointValues.length; i++) {
        var current = pointValues[i];
        var entry = {
                value: current.value - previous.value,
                timestamp: current.timestamp
        };
        newData.push(entry);
        
        //Move along
        previous = current;
    }
    
    return newData;
};

/**
 * Load Point
 * @todo Document better
 * @param {DataPoint} point - Point To load
 * @param {Object} options - options for load
 */
AccumulatorRollupDataProvider.prototype.loadPoint = function(point, options) {
    // clone so we can change rollup without affecting options elsewhere
    options = $.extend({}, options);
    // always use accumulator rollup
    options.rollup = 'ACCUMULATOR';
    
    var modifiedFrom = moment(options.fromMoment);
    // need to get 1 extra time period so we have accumulator previous value
    switch(options.timePeriodType) {
    case "MINUTES":
        modifiedFrom.subtract(1, 'minutes');
        break;
    case "HOURS":
        modifiedFrom.subtract(1, 'hours');
        break;
    case "DAYS":
        modifiedFrom.subtract(1, 'days');
        break;
    case "MONTHS":
        modifiedFrom.subtract(1, 'months');
        break;
    }
    options.from = modifiedFrom.toDate();
    
    return PointValueDataProvider.prototype.loadPoint.call(this, point, options);
};

DataProvider.registerProvider(AccumulatorRollupDataProvider);
return AccumulatorRollupDataProvider;

}); // close define