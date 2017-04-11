/**
 * Display Raw HTML in a Dom Node
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 * @module {HTMLDisplay} mango/HTMLDisplay
 * @see HTMLDisplay
 * @augments TextDisplay
 */
define(['jquery', './TextDisplay'], function($, TextDisplay) {
'use strict';

/**
 * @constructs HTMLDisplay
 * @augments TextDisplay
 * @param {Object} options - options for display
 */
function HTMLDisplay(options) {
	TextDisplay.apply(this, arguments);

    this.valueAttribute = 'value';
    this.suffix = '';
    this.decimalPlaces = 2;
    this.inhibitUpdateOnFocus = $(null);

    for(var i in options) {
        this[i] = options[i];
    }
    
    this.dataProviderIds = [this.dataProviderId];
}

HTMLDisplay.prototype = Object.create(TextDisplay.prototype);


/**
 * Data Provider listener to clear data
 */
HTMLDisplay.prototype.onClear = function() {
    if (this.useVal) {
        var inputs = this.selection.filter('input');
        var others = this.selection.not(inputs);
        inputs.val('');
        others.html('');
    }
    else {
        this.selection.html('');
    }
    delete this.previous;
};

/**
 * Data Provider Listener
 * On Data Provider load we add new data
 * @param {Array|number|PointValueTime} data - Value to update with if array then data[0] is used
 * @param {Object} dataPoint - data point that corresponds to the value
 */
HTMLDisplay.prototype.onLoad = function(data, dataPoint) {
    if ($.isArray(data)) {
        if (data.length) {
            data = data[0];
        }
        else return;
    }
    
    if (typeof data.minimum == 'object') {
        data.minimum = data.minimum.value;
        data.maximum = data.maximum.value;
        data.difference = data.maximum - data.minimum;
    }
    
    var value = data[this.valueAttribute];
    if (value === null || value === undefined) {
        // we will often want convertedValue or renderedValue but they aren't available
        // on non-numeric points
        value = data.value;
    }
    
    if (typeof this.manipulateValue === 'function')
        value = this.manipulateValue(value, dataPoint);

    var rendered = this.renderHTML(value);
    
    if (typeof this.onChange === 'function') {
        if (this.previous !== undefined && rendered !== this.previous) {
            this.onChange();
        }
        this.previous = rendered;
    }
    
    if (this.useVal) {
        var inputs = this.selection.filter('input');
        var others = this.selection.not(inputs);
        
        if (this.inhibitUpdateOnFocus.filter(':focus').length === 0) {
            inputs.filter(':not(:focus)').val(rendered);
        }
        others.html(rendered);
    }
    else {
        this.selection.html(rendered);
    }
};

/**
 * @param {object} value - Value to render
 * @param {DataPoint} dataPoint - Data Point to render
 */
HTMLDisplay.prototype.renderHTML = function(value, dataPoint) {
    // PointValueTime
    if (value && typeof value === 'object' && 'value' in value && 'timestamp' in valuevalue) {
    	return this.renderValue(value.value);
    }
    return this.renderValue(value);
};

return HTMLDisplay;

}); // define
