/**
 * Data Provider for RealTime Updates Via Web Sockets
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {RealtimeDataProvider} mango/RealtimeDataProvider
 * @see RealtimeDataProvider
 * @augments DataProvider
 * @tutorial pointValuesWebSocket
 */
define(['jquery', './DataProvider', './PointEventManager'],
function($, DataProvider, PointEventManager) {
"use strict";

//use a static PointEventManager which is shared between all RealtimeDataProviders
/**
 * @member {PointEventManager} pointEventManager - Static member for all RealtimeDataProviders
 */
var pointEventManager = new PointEventManager();

/**
 * Constructor
 * @constructs RealtimeDataProvider
 * @param {!number|string} id - Data Provider ID
 * @param {Object} options - Options for provider
 * @augments DataProvider
 */
function RealtimeDataProvider(id, options) {
    DataProvider.apply(this, arguments);
    this.eventHandler = this.eventHandler.bind(this);
}

RealtimeDataProvider.prototype = Object.create(DataProvider.prototype);


/**
 * Return data as Array of size 1
 * @type {boolean}
 * @default false
 */
RealtimeDataProvider.prototype.asArray = false;

/**
 * Type of Data Provider
 * @type {string}
 * @default 'RealtimeDataProvider'
 * @const
 */
RealtimeDataProvider.prototype.type = 'RealtimeDataProvider';

/**
 * What events do we register for 
 * @type {string} 
 * @default 'Update'
 */
RealtimeDataProvider.prototype.eventType = 'UPDATE';

/** 
 * Number of initial values to request at start
 * @type {number} 
 * @default 1 
 */
RealtimeDataProvider.prototype.numInitialValues = 1;


/**
 * Clear out our pointConfigurations if required
 * 
 * Signal to all Listeners to clear ALL their data
 * 
 * @param {boolean} clearConfigurations - boolean to clear pointConfigurations too
 */
RealtimeDataProvider.prototype.clear = function(clearConfigurations) {
    var self = this;

    if (clearConfigurations) {
        $.each(this.pointConfigurations, function(key, pointConfig) {
            var point = self.toPoint(pointConfig);
            pointEventManager.unsubscribe(point.xid, self.eventType, self.eventHandler);
        });
    }
    
    DataProvider.prototype.clear.apply(this, arguments);
};

/**
 * This provider never needs to reload as its continually updated
 * @param {?Object} changedOptions
 * @returns {boolean} 
 */
RealtimeDataProvider.prototype.needsToLoad = function(changedOptions) {
    // never need to reload as its continually updated
    if (this.previousOptions)
        return false;
    return true;
};

/**
 * @param {Object} point - point to load with xid member
 * @param {Object} options - options {from: from date, to: to date}
 * @returns {Object} Statistics Object
 */
RealtimeDataProvider.prototype.loadPoint = function(point, options) {
    return this.mangoApi.getLatestValues(point.xid, this.numInitialValues, this.apiOptions);
};

/**
 * Disable the data provider by unsubscribing for events
 * on the Web Socket
 */
RealtimeDataProvider.prototype.disable = function() {
    var self = this;
    $.each(this.pointConfigurations, function(key, pointConfig) {
        var point = self.toPoint(pointConfig);
        pointEventManager.unsubscribe(point.xid, self.eventType, self.eventHandler);
    });

    DataProvider.prototype.disable.apply(this, arguments);
};

/**
 * Enable the data provider by subscribing for events 
 * on the WebSocket
 */
RealtimeDataProvider.prototype.enable = function() {
    var self = this;
    $.each(this.pointConfigurations, function(key, pointConfig) {
        var point = self.toPoint(pointConfig);
        pointEventManager.subscribe(point.xid, self.eventType, self.eventHandler);
    });

    DataProvider.prototype.enable.apply(this, arguments);
};

/**
 * Add a data point configuration to our list
 * @param {Object} dataPointConfiguration - configuration to add
 */
RealtimeDataProvider.prototype.addDataPoint = function(dataPointConfiguration) {
    var ret = DataProvider.prototype.addDataPoint.apply(this, arguments);
    if (!ret)
        return ret;
    
    if (this.enabled) {
        var point = this.toPoint(dataPointConfiguration);
        var xid = point.xid;
        try {
            pointEventManager.subscribe(xid, this.eventType, this.eventHandler);
        }
        catch (e) {
            // fail silently if WebSocket not supported
        }
    }
};

/**
 * Remove a data point configuration from our list
 * @param {Object} dataPointConfiguration - configuration to remove
 */
RealtimeDataProvider.prototype.removeDataPoint = function(dataPointConfiguration) {
    var ret = DataProvider.prototype.removeDataPoint.apply(this, arguments);
    if (!ret)
        return ret;

	if (this.enabled) {
    	var point = this.toPoint(dataPointConfiguration);
    	var xid = point.xid;
    	try{
    		pointEventManager.unsubscribe(xid, this.eventType, this.eventHandler);
    	}catch(e){
            // fail silently if WebSocket not supported
    	}
    }
};


/**
 * Handle the Events
 * @param {Object} event
 * @param {Object} payload
 */
RealtimeDataProvider.prototype.eventHandler = function(event, payload) {
    if ((payload.event !== this.eventType) && (payload.event !== 'REGISTERED'))
        return;
    
    var value = $.extend({}, payload.value);
    
    value.originalValue = value.value;
    value.renderedValue = payload.renderedValue;
    value.convertedValue = payload.convertedValue;
    
    if (this.apiOptions.rendered)
        value.value = value.renderedValue;
    else if (this.apiOptions.converted)
        value.value = value.convertedValue;
    
    if(this.asArray === true)
    	value = [value];
    var self = this;
    $.each(this.pointConfigurations, function(key, pointConfig) {
        var point = self.toPoint(pointConfig);
        if (point.xid === payload.xid) {
            self.notifyListeners(value, point);
        }
    });
};

DataProvider.registerProvider(RealtimeDataProvider);
return RealtimeDataProvider;

});