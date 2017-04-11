/**
 * Data Provider Base Class
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {DataProvider} mango/DataProvider
 * @see DataProvider
 */
define(['jquery', './api'], function($, MangoAPI) {
"use strict";


/**
 * Data Provider constructor
 * @constructs DataProvider
 * @param {!number|string} id - Data Provider ID
 * @param {Object} options - Options for provider
 */
function DataProvider(id, options) {
	
    this.id = id;
    this.listeners = [];
    this.pointConfigurations = [];
    this.apiOptions = {};
    
    $.extend(this, options);
    
    if (!this.mangoApi) {
        this.mangoApi = MangoAPI.defaultApi;
    }
    
    if (this.enabled) {
        this.enable();
    }
}
/**
 * Cancel previous load on new load?
 * @default true
 * @type {boolean}
 * 
 */
DataProvider.prototype.cancelLastLoad = true;

/**
 * Type of Data Provider
 * @type {string}
 * @default 'DataProvider'
 * @const
 */
DataProvider.prototype.type = 'DataProvider';

/**
 * Unique ID for reference (use Alphanumerics as auto generated ones are numbers)
 * @default null
 * @type{?number|string}  
 */
DataProvider.prototype.id = null;

/**
 *  List of Points + configurations to use
 *  @default null
 *  @type {?Array.<Object>}  
 */
DataProvider.prototype.pointConfigurations = null;

/** 
 * Listeners to send new data when load() completes
 * @default null
 * @type {?Array.<DataProviderListener>}
 */
DataProvider.prototype.listeners = null;

/**
 * Is this data provider enabled to request data from Mango?
 * @default true
 * @type {boolean}  
 */
DataProvider.prototype.enabled = true;


/**
 * This data provide will clear the displays prior to load()
 * @default true
 * @type {boolean} 
 */

DataProvider.prototype.clearOnLoad = true;

/**
 * Optionally manipulate data.
 * 
 *  Send in this method in the options during object creation.
 * 
 * @param data - data returned from load()
 * @param point - corresponding point
 * @return manipulated data
 */
DataProvider.prototype.manipulateData = function(data, point) {
    return data;
};
    
/**
 * Clear out our pointConfigurations if required
 * Signal to all Listeners to clear ALL their data
 * @param clearConfigurations - boolean to clear pointConfigurations too
 */
DataProvider.prototype.clear = function(clearConfigurations) {
    if (clearConfigurations) {
        while (this.pointConfigurations.length > 0) {
            this.pointConfigurations.pop();
        }
        // ensures that next load() call actually loads
        if (this.previousOptions)
            delete this.previousOptions;
    }
    for (var i = 0; i < this.listeners.length; i++) {
        this.listeners[i].onClear();
    }
};

DataProvider.prototype.needsToLoad = function(changedOptions) {
    return true;
};

/**
 * Load our data and publish to listeners
 * @param {Object} options - {from: date, to: date}
 * @return promise when done
 */
DataProvider.prototype.load = function(options) {
    if (!this.enabled) {
        return rejectedPromise({type: 'providerDisabled', description: 'Data provider is not enabled'});
    }
    
    // check if we should reload
    var changedOptions = this.changedOptions(this.previousOptions, options);
    if (!options.forceRefresh && !this.needsToLoad(changedOptions)) {
        return rejectedPromise({type: 'loadNotNeeded', description: 'Load is not needed'});
    }
    
    this.previousOptions = options;
    
    if (this.clearOnLoad) {
        this.clear();
    }
    
    if (this.pointConfigurations.length > 0)
        this.notifyLoading();
    
    if (this.cancelLastLoad)
        this.cancelLoad();
    
    var promises = [];

    var self = this;
    $.each(this.pointConfigurations, function(i, configuration) {
        var point = self.toPoint(configuration);
        var promise = self.loadPoint(point, options).then(function(data) {
            // filter promise so we supply point to promise.done
            return {data: data, point: point};
        }, function(errorObject) {
            errorObject.point = point;
            return errorObject;
        });
        promises.push(promise);
    });

    var combinedPromise = this.lastLoadPromise = MangoAPI.when(promises);
    
    // notify all listeners at once in order
    combinedPromise.done(function() {
        for (var i = 0; i < arguments.length; i++) {
            var resolved = arguments[i];
            self.notifyListeners(resolved.data, resolved.point);
        }
        self.redrawListeners();
    }).fail(function() {
        for (var i = 0; i < arguments.length; i++) {
            var errorObject = arguments[i];
            
            // trigger a jquery event
            $(self).trigger('loadPointFailed', errorObject);
            
            self.notifyLoadPointFailed(errorObject);
        }
        self.redrawListeners();
    });
    
    return combinedPromise;
};

/**
 * Load point, should always be overridden
 * @param {DataPoint} point - Point To load
 * @param {Object} options - options for load
 */
DataProvider.prototype.loadPoint = function(point, options) {
    // fail. need to override
    var deferred = $.Deferred();
    deferred.reject(null, "invalid", "loadPoint() should be overridden");
    return deferred.promise();
};

/**
 * Cancel the current load()
 */
DataProvider.prototype.cancelLoad = function() {
    if (this.lastLoadPromise &&
            this.lastLoadPromise.state() === 'pending' &&
            typeof this.lastLoadPromise.cancel === 'function') {
        this.lastLoadPromise.cancel();
    }
};

/**
 * Notifies the listeners of new data
 * @param {Object} data - the new data
 * @param {DataPoint} point - the point that the data came from
 */
DataProvider.prototype.notifyListeners = function(data, point) {
    // Optionally manipulate the data
    if (this.manipulateData !== null)
        data = this.manipulateData(data, point);

    // Inform our listeners of this new data
    for (var i=0; i<this.listeners.length; i++) {
        this.listeners[i].onLoad(data, point);
    }
};

/**
 * Tells listeners to redraw, if they support it
 */
DataProvider.prototype.redrawListeners = function() {
    for (var i=0; i<this.listeners.length; i++) {
        if (typeof this.listeners[i].redraw === 'function')
            this.listeners[i].redraw();
    }
};

/**
 * Notifies the listeners that data is loading
 */
DataProvider.prototype.notifyLoading = function() {
    for (var i=0; i<this.listeners.length; i++) {
        if (typeof this.listeners[i].loading === 'function')
            this.listeners[i].loading();
    }
};

/**
 * Notifies the listeners that a point failed to load
 */
DataProvider.prototype.notifyLoadPointFailed = function(errorObject) {
    for (var i=0; i<this.listeners.length; i++) {
        var listener = this.listeners[i];
        if (typeof listener.loadPointFailed === 'function')
            listener.loadPointFailed(errorObject);
    }
};

/**
 * Put Point Value 
 * @param {Object} options - {
 *                  refresh: boolean to refresh displays,
 *                  putAll: boolean, true if value is written to all points
 *                  value: PointValueTime Model if putAll is true, otherwise
 *                         an object with PVT model for each XID
 *                }
 * 
 * @return promise
 */
DataProvider.prototype.put = function(options) {
    if (!this.enabled) {
        return rejectedPromise("disabled", "Data provider is not enabled");
    }
            
    var promises = [];

    var self = this;
    $.each(this.pointConfigurations, function(i, configuration) {
        var point = self.toPoint(configuration);
        var value = options.putAll ? options.value : options.value[point.xid];
        if (value) {
            var promise = self.putPoint(point, value, options).then(function(data) {
                // filter promise so we supply point to promise.done
                return {data: data, point: point};
            });
            promises.push(promise);
        }
    });

    var combinedPromise = MangoAPI.when(promises);
    
    if (options.refresh) {
        // notify all listeners at once in order
        combinedPromise.done(function() {
            for (var i in arguments) {
                var resolved = arguments[i];
                self.notifyListeners(resolved.data, resolved.point);
            }
            self.redrawListeners();
        });
    }
    
    if (typeof error == 'function') combinedPromise.fail(error);
    
    return combinedPromise;
};

/**
 * Put value for point via PUT REST endpoint
 */
DataProvider.prototype.putPoint = function(point, value, options) {
    /**
     * TODO properly handle putting a rendered text string to REST endpoints
     * This should work for numeric/multistate/binary points
     * This is a workaround until then
     */
    var putOptions = $.extend({}, this.apiOptions);
    if (putOptions.rendered) {
        putOptions.rendered = false;
        if (point.pointLocator.dataType === 'NUMERIC') {
            putOptions.converted = true;
        }
    }
    
    return this.mangoApi.putValue(point.xid, value, putOptions);
};

/**
 * Add a listener who registers to know of our updates
 */
DataProvider.prototype.addListener = function(dataProviderListener) {
    this.listeners.push(dataProviderListener);
};

/**
 * Remove a listener
 */
DataProvider.prototype.removeListener = function(dataProviderListener) {
    var index = $.inArray(dataProviderListener, this.listeners);
    if (index >= 0) {
        this.listeners.splice(index, 1);
    }
};

/**
 * Remove all listeners
 */
DataProvider.prototype.removeAllListeners = function() {
    while(this.listeners.length > 0)
        this.listeners.pop();
};

/**
 * Disable the provider
 */
DataProvider.prototype.disable = function() {
    this.enabled = false;
};

/**
 * Enable the provider
 */
DataProvider.prototype.enable = function() {
    this.enabled = true;
};

/**
 * Add a data point configuration to our list
 * @return {boolean} true if point was added, false if it already existed
 */
DataProvider.prototype.addDataPoint = function(dataPointConfiguration) {
    if (!dataPointConfiguration)
        return false;
    var newPoint = this.toPoint(dataPointConfiguration);
    
    //We only allow adding a Data Point Configuration once
    for(var i=0; i<this.pointConfigurations.length; i++) {
        var point = this.toPoint(this.pointConfigurations[i]);
        
        if(point.xid == newPoint.xid)
            return false;
    }
    this.pointConfigurations.push(dataPointConfiguration);

    // ensures that next load() call actually loads
    delete this.previousOptions;

    return true;
};

/**
 * Remove a data point configuration from our list
 * @return {boolean} true if point was removed, false if it did not exist
 */
DataProvider.prototype.removeDataPoint = function(dataPointConfiguration) {
    if (!dataPointConfiguration)
        return false;
    var newPoint = this.toPoint(dataPointConfiguration);
    
    //We only allow adding a Data Point Configuration once
    for(var i=0; i<this.pointConfigurations.length; i++) {
        var point = this.toPoint(this.pointConfigurations[i]);
        
        if(point.xid == newPoint.xid){
        	this.pointConfigurations.splice(i, 1);
            // ensures that next load() call actually loads
            delete this.previousOptions;
            return true;
        }
    }

    return false;
};

/**
 * Enables data providers to use legacy pointConfigurations or just store plain points
 */
DataProvider.prototype.toPoint = function(pointConfig) {
    return typeof pointConfig.xid === 'undefined' ? pointConfig.point : pointConfig;
};

/**
 * Add data points to provider
 */
DataProvider.prototype.addDataPoints = function(dataPointConfiguration) {
    for (var i in dataPointConfigurations) {
        this.addDataPoint(dataPointConfigurations[i]);
    }
};

/**
 * 
 * Compare 2 Change Options and return the result as 
 * an object with members that have changed set to true 
 * and members that have not set to false
 * 
 * 
 * @param {Object} a - First Options
 * @param {Object} b - Second Options
 * @return {Object} Result of comparison with each member set to true or false
 */
DataProvider.prototype.changedOptions = function(a, b) {
    if (!a || !b) {
        return {
                from: true,
                to: true,
                rollup: true,
                timePeriodType: true,
                timePeriods: true
        };
    }
    var result = {
        from: false,
        to: false,
        rollup: false,
        timePeriodType: false,
        timePeriods: false
    };
    if (!a.from || !b.from || a.from.valueOf() !== b.from.valueOf()) {
        result.from = true;
        result.fromValue = b.from;
    }
    if (!a.to || !b.to || a.to.valueOf() !== b.to.valueOf()) {
        result.to = true;
        result.toValue = b.to;
    }
    if (a.rollup !== b.rollup) {
        result.rollup = true;
        result.rollupValue = b.rollup;
    }
    if (a.timePeriodType !== b.timePeriodType) {
        result.timePeriodType = true;
        result.timePeriodTypeValue = b.timePeriodType;
    }
    if (a.timePeriods !== b.timePeriods) {
        result.timePeriods = true;
        result.timePeriodsValue = b.timePeriods;
    }
    return result;
};

/**
 * Helper RejectedPromose
 */
function rejectedPromise(reason, description) {
    var deferred = $.Deferred();
    deferred.reject(null, reason, description);
    return deferred.promise();
}

/**
 * @member {Object} providers - Map of Available Providers
 */
var providers = {};

/**
 * Register New Data Provider
 * @param {DataProvider} provider - Data Provider to register
 */
DataProvider.registerProvider = function(provider) {
    providers[provider.prototype.type] = provider;
};

/**
 * Create a New Data Provider
 * @param {string|number} id - ID for provider
 * @param {string} type - Type of Data Provider
 * @param {options} options - Options for Provider
 */
DataProvider.newProvider = function(type, id, options) {
    return new providers[type](id, options);
};

return DataProvider;

}); // close define