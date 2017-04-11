/**
 * Display Data as List Items in an <ul>
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 * @module {ListDisplay} mango/ListDisplay
 * @see ListDisplay
 * @augments TextDisplay
 * @tutorial listDisplay
 */
define(['jquery', './TextDisplay'], function($, TextDisplay) {
'use strict';

/**
 * @constructs ListDisplay
 * @augments TextDisplay
 * @example var display = new ListDisplay({selection: $('#ulId')});
 * @param {Object} options - options for display
 */
function ListDisplay(options){
	TextDisplay.apply(this, arguments);
	
    for(var i in options) {
        this[i] = options[i];
    }
}

ListDisplay.prototype = Object.create(TextDisplay.prototype);


/**
 * Style Class for List
 * @type {string} 
 * @default null
 */
ListDisplay.prototype.styleClass = null;


/**
 * Type of List Item to create
 * @type {string}
 * @default '<li>'
 */
ListDisplay.prototype.listItemTag = '<li>';

/**
 * Data Provider Listener
 * On Data Provider load we add new data
 * @param {Array|number|PointValueTime} data - Value to update with if array then data[0] is used
 * @param {?Object} dataPoint - data point that corresponds to the value
 */
ListDisplay.prototype.onLoad = function(data, dataPoint) {
    
	if (!$.isArray(data)) {
    	data = [data]; //Create one if its not
    }
    
    var self = this;
    
	for(var i=0; i<data.length; i++){
	    var value = data[i][this.valueAttribute];
	    if (value === null || value === undefined) {
	        // we will often want convertedValue or renderedValue but they aren't available
	        // on non-numeric points
	        value = data[i].value;
	    }
		if (typeof this.manipulateValue === 'function')
		value = this.manipulateValue(value, dataPoint);
		
		var label = this.renderText(value);
		var li = $(this.listItemTag);
		li.text(label);
		if(this.styleClass !== null)
		    li.attr('class', this.styleClass);
		if(typeof this.onClick === 'function'){
			var onClickData = data[i];
			var onClickDataPoint = dataPoint;
			li.click({value: onClickData, dataPoint: onClickDataPoint}, self.onClick);
		}
		this.selection.append(li);
	}
};

/**
 * Called when the <li> is clicked.
 * Override as necessary, by default does nothing.
 * @function
 * @param {Object} event - event.data.value and event.data.dataPoint set
 */
ListDisplay.prototype.onClick = null;

return ListDisplay;

}); // close define
