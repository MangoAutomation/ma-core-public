/**
 * Display Statistics In a Table
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {StatisticsTableDisplay} mango/StatisticsTableDisplay
 * @augments BaseDisplay
 * @see StatisticsTableDisplay
 * @tutorial statisticsTableDisplay
 */
define(['jquery', './BaseDisplay'], function($, BaseDisplay) {

/**
 * @constructs StatisticsTableDisplay
 * @augments BaseDisplay
 * @example var display = new StatisticsTableDisplay({selection: $('#div')});
 * @param {Object} options - options for display
 */
function StatisticsTableDisplay(options) {
	BaseDisplay.apply(this, arguments);
    this.dataProviderIds = [];
    this.rows = {};
    
    for(var i in options) {
        this[i] = options[i];
    }
}

StatisticsTableDisplay.prototype = Object.create(BaseDisplay.prototype);

/**
 * Point Info to display
 * @param {Array}
 * @default ['name']
 */
StatisticsTableDisplay.prototype.pointProperties = ['name'];

/**
 * Statistics to display
 * @type {Array}
 * @default ['average', 'maximum', 'minimum']
 */
StatisticsTableDisplay.prototype.dataProperties = ['average', 'maximum', 'minimum'];
 
    
/**
 * Clear out the table
 */
StatisticsTableDisplay.prototype.onClear = function() {
        this.selection.find('tbody tr').remove();
    delete this.rows;
    this.rows = {};
};

/**
 * Show the loading message as a row in the table
 */
StatisticsTableDisplay.prototype.loading = function() {
    if (this.selection.find('tbody tr.loading').length > 0)
        return;
    
    var tr = $('<tr>');
    tr.addClass('loading');
    td = $('<td>');
    td.text('Loading');
    td.attr('colspan', this.pointProperties.length + this.dataProperties.length);
    tr.append(td);
    this.selection.find('tbody').append(tr);
};

/**
 * Load in the statistics
 * @param {Statistics} data - statistics data to laod
 * @param {DataPoint} dataPoint - data point of statistics
 */
StatisticsTableDisplay.prototype.onLoad = function(data, dataPoint) {
    if (!data.hasData)
        return;
    
    this.selection.find('tbody tr.loading').remove();
    
    var row = this.rows[dataPoint.xid];
    if (!row) {
        row = this.createRow(dataPoint);
    }
    
    var prop, td, value;
    
    for (var i in this.pointProperties) {
        prop = this.pointProperties[i];
        td = row.find('.point-prop-' + prop);
        value = dataPoint[prop];
        td.text(this.renderCellText(value));
    }
    
    for (i in this.dataProperties) {
        prop = this.dataProperties[i];
        td = row.find('.data-prop-' + prop);
        value = data[prop];
        td.text(this.renderCellText(value));
    }
};

/**
 * Create Row
 * @param {DataPoint} dataPoint - Data Point to create row for
 */
StatisticsTableDisplay.prototype.createRow = function(dataPoint) {
    var tr = $('<tr>');
    tr.addClass('stats-row-xid-' + dataPoint.xid);
    
    var prop, td;
    
    for (var i in this.pointProperties) {
        prop = this.pointProperties[i];
        td = $('<td>');
        td.addClass('point-prop-' + prop);
        tr.append(td);
    }
    
    for (i in this.dataProperties) {
        prop = this.dataProperties[i];
        td = $('<td>');
        td.addClass('data-prop-' + prop);
        tr.append(td);
    }
    
    this.selection.find('tbody').append(tr);
    return tr;
};

/**
 * Render Cell Text
 * @param {Object|number} value - Value to render
 */
StatisticsTableDisplay.prototype.renderCellText = function(value) {
    // Are we a PointValueTime
    if (value && typeof value === 'object' && 'value' in value && 'timestamp' in value) {
        return this.renderValue(value.value) + ' @ ' + this.renderTime(value.timestamp);
    }
    
    return this.renderValue(value);
};


return StatisticsTableDisplay;
}); // define
