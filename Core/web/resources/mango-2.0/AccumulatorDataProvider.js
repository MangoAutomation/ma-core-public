/**
 * Data Provider for RealTime Updates Via Web Sockets
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {AccumulatorDataProvider} mango/AccumulatorDataProvider
 * @augments DataProvider
 * @see AccumulatorDataProvider
 */
define(['jquery', './DataProvider'], function($, DataProvider) {
"use strict";

/**
 * Accumulator Data Providers compute accumulation of point values over time
 * @constructs AccumulatorDataProvider
 * @param {number} id - Data Provider ID
 * @param {Object} options - Extra options desired
 * @augments DataProvider
 */
function AccumulatorDataProvider(id, options) {
    DataProvider.apply(this, arguments);
}

AccumulatorDataProvider.prototype = Object.create(DataProvider.prototype);

/**
 * Type of Data Provider
 * @type {string}
 * @default 'AccumulatorDataProvider'
 * @const
 */
AccumulatorDataProvider.prototype.type = 'AccumulatorDataProvider';

/**
 * Does the Data Provider Need to reload?
 * @param {Object} changedOptions - {to: boolean, from: boolean}
 */
AccumulatorDataProvider.prototype.needsToLoad = function(changedOptions) {
    if (changedOptions.from || changedOptions.to)
        return true;
    return false;
};

/**
 * Load Point
 * @param {DataPoint} point - Point To load
 * @param {Object} options - options to load
 */
AccumulatorDataProvider.prototype.loadPoint = function(point, options) {
    var promise = this.mangoApi.getFirstLastValues(point.xid, options.from, options.to, this.apiOptions)
    .then(function(data) {
        var result = {};
        if (data.length < 2 || !data[0] || !data[1]) {
            result.value = null;
            return result;
        }
        result.value = data[1].value - data[0].value;
        result.first = data[0];
        result.last = data[1];
        return result;
    });
    return promise;
};

DataProvider.registerProvider(AccumulatorDataProvider);
return AccumulatorDataProvider;

}); // close define