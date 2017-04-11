/**
 * Display Point Values as Text in a Dom Node
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {BaseDisplay} mango/BaseDisplay
 * @see BaseDisplay
 */
define(['jquery', 'moment-timezone'], function($, moment) {

	
/**
 * Base Operations for a Display
 * @constructs BaseDisplay
 * @param {Object} options
 */
function BaseDisplay(options){
    $.extend(this, options);
}

/**
 * Type of Display
 * @type {string}
 * @default 'BaseDisplay'
 * @const
 */
BaseDisplay.prototype.type = 'BaseDisplay';

/**
 * Attribute of Data to display
 * @type {string}
 * @default 'value'
 */
BaseDisplay.prototype.valueAttribute = 'value';

/** 
 * any appending text for rendering the value
 * @type {string} 
 * @default ''
 */
BaseDisplay.prototype.suffix = '';
/** 
 * number of decimal places to round the rendered value
 * @type {number}  
 * @default 2
 */
BaseDisplay.prototype.decimalPlaces = 2;

/** 
 * Time Format for moment.js
 * @type {string}  
 * @default 'lll'
 */
BaseDisplay.prototype.timeFormat = 'lll';

/**
 * Dom Node of Display
 * @default null
 * @type {Object}
 */
BaseDisplay.prototype.selection = null;


/**
 * Create the display
 */
BaseDisplay.prototype.createDisplay = function() {
    return this;
};

/**
 * Data Provider listener to clear data
 */
BaseDisplay.prototype.onClear = function() {
};

/**
 * Data Provider Listener
 * On Data Provider load we add new data
 */
BaseDisplay.prototype.onLoad = function(data, dataPoint) {
};

/**
 * Display Loading Message
 * By default does nothing, override as necessary
 */
BaseDisplay.prototype.loading = function(){
	
};

/**
 * Remove the loading display
 */
BaseDisplay.prototype.removeLoading = function() {
    
};

/**
 * Optionally display a message or do something when a point fails to load
 * By default does nothing, override as necessary
 * @param {Object} errorObject describing the failure
 */
BaseDisplay.prototype.loadPointFailed = function(errorObject) {
    this.removeLoading();
};

/**
 * Render A number to BaseDisplay#decimalPlaces. If anything 
 * else is provided it is returned unchanged.
 * @param {Object|number}
 */
BaseDisplay.prototype.renderValue = function(value) {
    if (typeof value === 'number')
        return value.toFixed(this.decimalPlaces);
    return value;
};

/**
 * Render Timestamp using BaseDisplay#timeFormat
 */
BaseDisplay.prototype.renderTime = function(timestamp) {
    return moment(timestamp).format(this.timeFormat);
};


return BaseDisplay;
	
});// Close define