/**
 * Data Provider for Statistics Data
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {StatisticsDataProvider} mango/StatisticsDataProvider
 * @see StatisticsDataProvider
 * @augments DataProvider
 */
define(['jquery', './DataProvider'], function($, DataProvider) {

/**
 * @constructs StatisticsDataProvider
 * @augments DataProvider
 * @param {string|number} id - ID for provider
 * @param {Object} options - custom options for provider
 */
function StatisticsDataProvider(id, options){
	 DataProvider.apply(this, arguments);
}
	
StatisticsDataProvider.prototype = Object.create(DataProvider.prototype);

/**
 * Type of Data Provider
 * @type {string}
 * @default 'StatisticsDataProvider'
 * @const
 */
StatisticsDataProvider.prototype.type = 'StatisticsDataProvider';

/**
 * Does the provider Need to Load data
 * @param {Object} changedOptions - { from: from date, to: to date, rollup: boolean, rollupValue: Array}
 * @returns {boolean}
 */
StatisticsDataProvider.prototype.needsToLoad = function(changedOptions) {
    if (changedOptions.from || changedOptions.to)
        return true;
    if((changedOptions.rollup) && (changedOptions.rollupValue === 'NONE'))
    	return true;
    return false;
};

/**
 * @param {Object} point - point to load with xid member
 * @param {Object} options - options {from: from date, to: to date}
 * @returns {Object} Statistics Object
 */
StatisticsDataProvider.prototype.loadPoint = function(point, options) {
    return this.mangoApi.getStatistics(point.xid, options.from, options.to, this.apiOptions);
};

DataProvider.registerProvider(StatisticsDataProvider);
return StatisticsDataProvider;

}); // close define
