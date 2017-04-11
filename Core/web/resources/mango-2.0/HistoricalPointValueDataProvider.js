/**
 * Access Historical Data by a count of samples back from latest value
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 * @exports mango/HTMLDisplay
 * @module {HistoricalDataProvider} mango/HistoricalDataProvider
 * @augments DataProvider
 * @see HistoricalDataProvider
 */

define(['jquery', './DataProvider'], function($, DataProvider) {

/**
 * @constructs HistoricalDataProvider
 * @param {number|string} id - ID For provider
 * @param {Object} options - options for provider
 * @augments DataProvider
 */
function HistoricalDataProvider(id, options) {
	DataProvider.apply(this, arguments);
}
	
HistoricalDataProvider.prototype = Object.create(DataProvider.prototype);
	

/**
 * Type of Data Provider
 * @type {string}
 * @default 'HistoricalPointValueDataProvider'
 * @const
 */
HistoricalDataProvider.prototype.type = 'HistoricalPointValueDataProvider';

/**
 * @param {Object} point - point to load with xid member
 * @param {Object} options - options {historicalSamples: number}
 * @returns {Promise} - Promise of latest values to be returned
 */
HistoricalDataProvider.prototype.loadPoint = function(point, options) {
    return this.mangoApi.getLatestValues(point.xid, options.historicalSamples, this.apiOptions);
};

DataProvider.registerProvider(HistoricalPointValueDataProvider);
return HistoricalPointValueDataProvider;

}); // close define