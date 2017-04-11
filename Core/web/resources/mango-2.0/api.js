/**
 * Access to the Mango Rest API
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire, Terry Packer
 * @module {MangoAPI} mango/api
 * @see MangoAPI
 */
define(['jquery', 'moment-timezone', 'jstz', './User'], function($, moment, jstz, User) {
"use strict";

/* Setup Global XHR  */
$.ajaxSetup({
    beforeSend: function(xhr, settings) {
        if (settings.type == 'POST' || settings.type == 'PUT' || settings.type == 'DELETE') {
            if (!(/^http:.*/.test(settings.url) || /^https:.*/.test(settings.url))) {
                // Only send the token to relative URLs i.e. locally.
                xhr.setRequestHeader("X-XSRF-TOKEN", getCookie('XSRF-TOKEN'));
            }
        }
    }
});

/**
 * @function getCookie 
 *
 * @param {string} name - Name of Cookie to retrieve
 * @returns {string} - Value of Cookie
 */
function getCookie(name) {
    var cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        var cookies = document.cookie.split(';');
        for (var i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i]);
            // Does this cookie string begin with the name we want?
            if (cookie.substring(0, name.length + 1) == (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

/**
 * @function toISOString
 *
 * @param {date} now - Date to convert
 * @returns {string} - Converted Date
 */
function toISOString(now) {
    return encodeURIComponent(moment(now).toISOString());
}


/**
 * Mango Rest API Object
 * 
 * @constructs MangoAPI
 * @param {Object} options - Options for api
 */
function MangoAPI(options){
    this.baseUrl = '';
    $.extend(this, options);
}

/**
 * Base URL to use for Requests. Leave alone unless using CORS
 * @type {string}
 * @default ''
 */
MangoAPI.prototype.baseUrl = null;

/**
 * Login via GET
 * 
 * @param {string} username
 * @param {string} password
 * @param {boolean} logout - optional, logout existing user
 * @return {Promise} promise, resolved with data when done
 */
MangoAPI.prototype.login = function(username, password, logout) {
    if (logout === undefined)
        logout = true;
    logout = logout ? true : false; // coerce to actual boolean
    
    return this.ajax({
        url : "/rest/v1/login/" + encodeURIComponent(username),
        headers: {
            password: password,
            logout: logout
        }
    });
};
        
/**
 * Logout via GET
 * 
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.logout = function() {
    return this.ajax({
        url : "/rest/v1/logout/"
    });
};
        
/**
 * Save Existing User
 * 
 * @param user - user to save
 * @param username - optional, if updating the username this is required.
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.putUser = function(user, username) {

	if(typeof username == 'undefined')
		username = user.username;
	
    return this.ajax({
        type: "PUT",
        url : "/rest/v1/users/" + encodeURIComponent(username),
        contentType: "application/json",
        data: JSON.stringify(user)
    });
};

/**
 * Save New User
 * 
 * @param user - user to add
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.postUser = function(user) {
    return this.ajax({
        type: "POST",
        url : "/rest/v1/users",
        contentType: "application/json",
        data: JSON.stringify(user)
    });
};

/**
 * Save New User
 * 
 * @param user - user to add
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.deleteUser = function(username) {
    return this.ajax({
        type: "DELETE",
        url : "/rest/v1/users/" + encodeURIComponent(username)
    });
};

/**
 * Get New User with defaults set
 * 
 * @param user - user to add
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.newUser = function(user) {
    return this.ajax({
        url : "/rest/v1/users/new/user"
    });
};

/**
 * Toggle mute setting
 * 
 * @param username - user to toggle
 * @param mute - optional boolean if not provided the current setting is toggled
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.toggleUserMute = function(username, mute) {
	var url = "/rest/v1/users/";
	url += encodeURIComponent(username) + "/mute";
	
	if(mute)
		url += "?mute=" + mute;
    return this.ajax({
        type: "PUT",
        url : url
    });
};

/**
 * Set Home URL
 * 
 * @param username - user to set
 * @param url - url to set to
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.setHomeURL = function(username, homeUrl) {
	var url = "/rest/v1/users/";
	url += encodeURIComponent(username) + "/homepage?url=";
	url += encodeURIComponent(homeUrl);
    return this.ajax({
        type: "PUT",
        url : url
    });
};

/**
 * Get All Permissions Information
 * 
 * @param query - comma separated String of permissions that are returned as already added to user
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getAllPermissionsInformation = function(query) {
    return this.ajax({
        url : "/rest/v1/users/permissions/"+ encodeURIComponent(query)
    });
};

/**
 * Get User Groups
 * 
 * @param exclude - comma separated String of groups
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getAllUserGroups = function(exclude) {
    return this.ajax({
        url : "/rest/v1/users/permissions-groups/"+ encodeURIComponent(exclude)
    });
};

/**
 * Get Help
 * 
 * @param helpId - help id to load
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getHelp = function(helpId) {
    return this.ajax({
        url : '/rest/v1/help/by-id/' + encodeURIComponent(helpId)
    });
};


/**
 * Make a request for any JSON data
 * 
 * @param url
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getJson = function(url) {
    return this.ajax({
        url : url
    });
};
            
/**
 * Force Data Point Refresh
 * @return promise, resolved with empty response when done
 */
MangoAPI.prototype.forcePointRefresh = function(xid){
	return this.ajax({
        url: "/rest/v1/runtime-manager/force-refresh/" + encodeURIComponent(xid),
        contentType: 'application/json',
        type: 'PUT'
    });
};

/**
 * Get All Data Points 
 * 
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getAllPoints = function() {
    return this.ajax({
        url: "/rest/v1/data-points"
    });
};
        
/**
 * Get One Data Point
 * 
 * @param xid
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getPoint = function(xid) {
    return this.ajax({
        url: "/rest/v1/data-points/" + encodeURIComponent(xid)
    });
};

/**
 * Bulk apply set permissions
 * 
 * @param permissions {String} set permissions 
 * @param query {String} rql query
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.applyBulkPointSetPermissions = function(permissions, query) {
	var data = JSON.stringify(permissions);
	var extraUrl = '';
	if(query !== '')
		extraUrl = '?' + query;
	return this.ajax({
    	type: 'POST',
        url: "/rest/v1/data-points/bulk-apply-set-permissions" + extraUrl,
        contentType: 'application/json',
        data: data
    });
};

/**
 * Bulk apply read permissions
 * 
 * @param permissions {String} read permissions 
 * @param query {String} rql query
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.applyBulkPointReadPermissions = function(permissions, query) {
	var data = JSON.stringify(permissions);
	var extraUrl = '';
	if(query !== '')
		extraUrl = '?' + query;
	return this.ajax({
    	type: 'POST',
        url: "/rest/v1/data-points/bulk-apply-read-permissions" + extraUrl,
        contentType: 'application/json',
        data: data
    });
};

/**
 * Bulk clear set permissions
 * 
 * @param query {String} rql query
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.clearBulkPointSetPermissions = function(query) {
	var extraUrl = '';
	if(query !== '')
		extraUrl = '?' + query;
	return this.ajax({
    	type: 'POST',
    	contentType: 'application/json',
        url: "/rest/v1/data-points/bulk-clear-set-permissions" + extraUrl
    });
};

/**
 * Bulk clear read permissions
 * 
 * @param query {String} rql query
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.clearBulkPointReadPermissions = function(query) {
	var extraUrl = '';
	if(query !== '')
		extraUrl = '?' + query;
	return this.ajax({
    	type: 'POST',
    	contentType: 'application/json',
        url: "/rest/v1/data-points/bulk-clear-read-permissions" + extraUrl
    });
};

/**
 * Query the Data Points Table
 * @see MangoAPI.createQueryComparison()
 * 
 * @param {QueryModel|string} query - Query Model:
 * { 
 *	offset: start position (can  be null)
 *	limit: limit results to this Number (can be null)
 *	andComparisons: Array.<QueryComparison>
 *  orComparisons: Array.<QueryComparison>,
 *  sort: {
 *  	attribute: String name
 *  	desc: true or false to order by descending
 *  }
 *  useOr: true or false to apply query conditions with OR or AND
 *  
 * 
 *  
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.queryPoints = function(query){
	
	if(typeof query == 'string'){
        return this.ajax({
            url: "/rest/v1/data-points?" + query
        });
	}else{
		var url = '/rest/v1/data-points/query';
        var data = JSON.stringify(query);
        
        return this.ajax({
            type: 'POST',
            url: url,
            contentType: 'application/json',
            data: data
        });        		
	}

};

/**
 * Save Data Point
 * 
 * @param dataPoint - point to save
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.putPoint = function(dataPoint) {
    return this.ajax({
        type: "PUT",
        url : "/rest/v1/data-points/" + encodeURIComponent(dataPoint.xid),
        contentType: "application/json",
        data: JSON.stringify(dataPoint)
    });
};

/**
 * Save Data Point
 * 
 * @param xid - xid of point to save
 * @param csvData - point CSV data to save
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.putCSVPoint = function(xid, csvData) {
    return this.ajax({
        type: "PUT",
        url : "/rest/v1/data-points/" + encodeURIComponent(xid) + ".csv",
        contentType: "text/csv",
        data: csvData
    });
};

/**
 * Get values based on date ranges with optional rollup
 * 
 * @param {string} xid - for point desired
 * @param {date} from - date from
 * @param {date} to - date to
 * @param {Object} options - optional object
 *        {
 *            rollup: one of ['AVERAGE', 'MAXIMUM', 'MINIMUM', 'SUM', 'FIRST', 'LAST', 'COUNT'],
 *            timePeriodType: one of ['MILLISECONS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS', 'WEEKS', 'MONTHS', 'YEARS'],
 *            timePeriods: integer number of periods to use,
 *            rendered: boolean (default false), function returns a rendered string instead of numeric value,
 *            converted: boolean (default false), function returns the point value converted to the chosen display unit (as a number),
 *        }
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getValues = function(xid, from, to, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid) + "?from=" + toISOString(from) + "&to=" +
        toISOString(to);
    
    if (options.rollup)
        url += "&rollup=" + encodeURIComponent(options.rollup);
    if (options.timePeriodType)
        url += "&timePeriodType=" + encodeURIComponent(options.timePeriodType);
    if (options.timePeriods)
        url += "&timePeriods=" + encodeURIComponent(options.timePeriods);
    if (typeof options.rendered !== 'undefined')
        url += "&useRendered=" + encodeURIComponent(options.rendered);
    if (typeof options.converted !== 'undefined')
        url += "&unitConversion=" + encodeURIComponent(options.converted);
    
    return this.ajax({
        url: url
    });
};

/**
 * Count values based on date ranges with optional rollup
 * 
 * @param {string} xid - for point desired
 * @param {date} from - date from
 * @param {date} to - date to
 * @param {Object} options - optional object
 *        {
 *            rollup: one of ['AVERAGE', 'MAXIMUM', 'MINIMUM', 'SUM', 'FIRST', 'LAST', 'COUNT'],
 *            timePeriodType: one of ['MILLISECONS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS', 'WEEKS', 'MONTHS', 'YEARS'],
 *            timePeriods: integer number of periods to use,
 *        }
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.countValues = function(xid, from, to, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid) + "/count?from=" + toISOString(from) + "&to=" +
        toISOString(to);
    
    if (options.rollup)
        url += "&rollup=" + encodeURIComponent(options.rollup);
    if (options.timePeriodType)
        url += "&timePeriodType=" + encodeURIComponent(options.timePeriodType);
    if (options.timePeriods)
        url += "&timePeriods=" + encodeURIComponent(options.timePeriods);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get first and last point values for a date range
 * 
 * @param {string} xid - for point desired
 * @param {date} from - date from
 * @param {date} to - date to
 * @param {Object} options - optional object
 *        {
 *            rendered: boolean (default false), function returns a rendered string instead of numeric value,
 *            converted: boolean (default false), function returns the point value converted to the chosen display unit (as a number)
 *        }
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getFirstLastValues = function(xid, from, to, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid) + "/firstLast?from=" +
        toISOString(from) + "&to=" + toISOString(to);
    
    if (typeof options.rendered !== 'undefined')
        url += "&useRendered=" + encodeURIComponent(options.rendered);
    if (typeof options.converted !== 'undefined')
        url += "&unitConversion=" + encodeURIComponent(options.converted);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get the latest limit number of values
 * 
 * @param {string} xid - for point desired
 * @param {Number} limit - number of results
 * @param {Object} options - optional object
 *        {
 *            rendered: boolean (default false), function returns a rendered string instead of numeric value,
 *            converted: boolean (default false), function returns the point value converted to the chosen display unit (as a number),
 *            useCache: boolean (default true), determines if values should be retrieved from cache
 *        }
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getLatestValues = function(xid, limit, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid) + "/latest?limit=" +
        encodeURIComponent(limit);
    
    if (typeof options.rendered !== 'undefined')
        url += "&useRendered=" + encodeURIComponent(options.rendered);
    if (typeof options.converted !== 'undefined')
        url += "&unitConversion=" + encodeURIComponent(options.converted);
    if (typeof options.useCache !== 'undefined')
        url += "&useCache=" + encodeURIComponent(options.useCache);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get the point statistics
 * 
 * @param {string} xid - for point desired
 * @param {date} from - date from
 * @param {date} to - date to
 * @param {Object} - optional object
 *        {
 *            rendered: boolean (default false), function returns a rendered string instead of numeric value,
 *            converted: boolean (default false), function returns the point value converted to the chosen display unit (as a number)
 *        }
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getStatistics = function(xid, from, to, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid) + "/statistics?from=" +
        toISOString(from) + "&to=" + toISOString(to);
    
    if (typeof options.rendered !== 'undefined')
        url += "&useRendered=" + encodeURIComponent(options.rendered);
    if (typeof options.converted !== 'undefined')
        url += "&unitConversion=" + encodeURIComponent(options.converted);
    
    return this.ajax({
        url: url
    });
};

/**
 * 
 * Save Point Value
 * @param {string} xid - for data point to save to
 * @param {PointValueTimeModel} value - PointValueTimeModel Number, boolean or String
 * @param {Object} options - optional object
 *        {
 *            converted: boolean (default false), numeric value to save is in display units,
 *                       convert to original units before saving to database
 *        }
 * @return promise when done
 */
MangoAPI.prototype.putValue = function(xid, pointValue, options) {
    options = options || {};
    
    var url = "/rest/v1/point-values/" + encodeURIComponent(xid);
    var data = JSON.stringify(pointValue);
    
    if (typeof options.converted !== 'undefined')
        url += "?unitConversion=" + encodeURIComponent(options.converted);
    
    return this.ajax({
        type: "PUT",
        url: url,
        contentType: "application/json",
        data: data
    });
};

/**
 * 
 * Register for point value events, once a socket is returned it can be used to 
 * register for additional points.
 * 
 * @tutorial pointValueWebSocket
 * 
 * @param xid - xid of data point
 * @param {Array} events - ['INITIALIZE', 'UPDATE', 'CHANGE', 'SET', 'BACKDATE', 'TERMINATE']
 * @param onMessage(message) - method to call on message received evt.data
 * @param onError(message) - method to call on error
 * @param onOpen - method to call on Socket Connection, XID parameter is passed in
 * @param onClose - method to call on Close
 * @returns webSocket
 */
MangoAPI.prototype.registerForPointEvents = function(xid, events, onMessage, onError, onOpen, onClose) {
    var socket = this.openSocket('/rest/v1/websocket/point-value');
    socket.onopen = function() {
        //Register for recieving point values
        // using a PointValueRegistrationModel
        socket.send(JSON.stringify({
            xid: xid,
            eventTypes: events
        }));
        onOpen(xid);
    };
    socket.onclose = onClose;
    socket.onmessage = function(event) {
        onMessage(JSON.parse(event.data));
    };
    return socket;
};

/**
 * Modify the existing events for a point on a socket
 * @param socket - web socket
 * @param xid - xid of data point
 * @param {Array} events - ['INITIALIZE', 'UPDATE', 'CHANGE', 'SET', 'BACKDATE', 'TERMINATE']
 * 
 */
MangoAPI.prototype.modifyRegisteredPointEvents = function(socket, xid, events) {
    socket.send(JSON.stringify({
        xid: xid,
        eventTypes: events
    }));
};

/**
 * Open a web socket to the host and port from where the 
 * page was loaded.
 * @param {string} path to web socket on host
 * @return {WebSocket}
 */
MangoAPI.prototype.openSocket = function(path) {
    if (!('WebSocket' in window)) {
        throw new Error('WebSocket not supported');
    }
    
    var host = document.location.host;
    var protocol = document.location.protocol;
    
    if (this.baseUrl) {
        var i = this.baseUrl.indexOf('//');
        if (i >= 0) {
            protocol = this.baseUrl.substring(0, i);
            host = this.baseUrl.substring(i+2);
        }
        else {
            host = this.baseUrl;
        }
    }
    
    protocol = protocol === 'https:' ? 'wss:' : 'ws:';
    
    return new WebSocket(protocol + '//' + host + path);
};

/**
 * Opens a WebSocket to a DaoNotificationWebSocketHandler and emits notifications
 * to a dstore about VO creates, updates and deletes
 * 
 * @param {string} path to web socket on host
 * @param {object} dstore object, must be trackable
 * @param {string} initiatorId, if the received message matches this id the store will not be notified
 */
MangoAPI.prototype.registerForDaoNotifications = function(path, store, initiatorId) {
    var socket = this.openSocket(path);
    var idProperty = store.idProperty;
    socket.onmessage = function(event) {
        var data = $.parseJSON(event.data);
        if (data.status === 'OK') {
            var action = data.payload.action;
            var object = data.payload.object;
            if (initiatorId && initiatorId === data.payload.initiatorId)
                return;
            store.emit(action, {target: object, id: object[idProperty]});
        }
    };
};

/**
 * Generate a random initiatorId string
 */
MangoAPI.prototype.generateInitiatorId = function() {
    return Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
};

/**
 * Get Current Value of Data Point
 * 
 * @param xid - for point desired
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getCurrentValue = function(xid) {
    var url = "/rest/v1/realtime/by-xid/" + encodeURIComponent(xid);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get All Current Values for running points
 * 
 * @param limit results too this
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getAllCurrentValues = function(limit) {
    var url = "/rest/v1/realtime/list?limit=" + encodeURIComponent(limit);
    
    return this.ajax({
        url: url
    });
};

/**
 * Returns the point hierarchy root folder
 * @return promise when done
 */
MangoAPI.prototype.getHierarchy = function() {
    var url = "/rest/v1/hierarchy/full";
    
    return this.ajax({
        url: url
    });
};

/**
 * Get Contents of a given folder
 * 
 * @param name of folder
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getFolderByName = function(name) {
    var url = "/rest/v1/hierarchy/by-name/" + encodeURIComponent(name);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get Contents of a given folder
 * 
 * @param id of folder
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getFolderById = function(id) {
    var url = "/rest/v1/hierarchy/by-id/" + encodeURIComponent(id);
    
    return this.ajax({
        url: url
    });
};

/**
 * Get the current user
 * 
 * @param {boolean} ignoreCache - ignore the cached user
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getCurrentUser = function(ignoreCache) {
    var url = "/rest/v1/users/current";
    
    if (this._cachedUser && !ignoreCache) return this._cachedUser;
    
    this._cachedUser = this.ajax({
        url: url
    }).then(function(_user) {
        return new User(_user);
    });
    
    return this._cachedUser;
};

/**
 * Query the Events Table
 * @see MangoAPI.createQueryComparison()
 * 
 * @param query - Query Model:
 * { 
 *	offset: start position (can  be null)
 *	limit: limit results to this Number (can be null)
 *	andComparisons: Array.<QueryComparison>
 *  orComparisons: Array.<QueryComparison>,
 *  sort: {
 *  	attribute: String name
 *  	desc: true or false to order by descending
 *  }
 *  useOr: true or false to apply query conditions with OR or AND
 *  
 * 
 *  
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.queryEvents = function(query){
	if(typeof query == 'string'){
        return this.ajax({
            url: "/rest/v1/events?" + query
        });
	}else{
    	var url = "/rest/v1/events/query";
        var data = JSON.stringify(query);
        
        return this.ajax({
            type: "POST",
            url: url,
            contentType: "application/json",
            data: data
        });
	}
};

/**
 * Get an event Summary of Active Events
 * @returns {Promise} resolved with summary when done
 */
MangoAPI.prototype.getEventsActiveSummary = function() {
    var url = '/rest/v1/events/active-summary';
    
    return this.ajax({
        url: url
    });
};

/**
 * Acknowledge an Event
 * @param {Object} event
 * @param {Object|string} message
 */
MangoAPI.prototype.acknowledgeEvent = function(event, message) {
    var url = '/rest/v1/events/acknowledge/' + encodeURIComponent(event.id);
    
    var data;
    if (typeof message === 'string') {
        data = {
            key: 'literal',
            args: [message]
        };
    }
    else {
        data = message;
    }
    data = JSON.stringify(data);
    
    return this.ajax({
        type: 'PUT',
        url: url,
        contentType: 'application/json',
        data: data
    });
};

/**
 * Register for alarm events
 * 
 * TODO change the callbacks to events that are emitted
 * 
 * @param events - ['ACKNOWLEGED', 'RAISED', 'RETURN_TO_NORMAL', 'DEACTIVATED']
 * @param levels - ['DO_NOT_LOG', ''....]
 * @param onMessage(message) - method to call on message received evt.data
 * @param onError(message) - method to call on error
 * @param onOpen - method to call on Socket 
 * @param onClose - method to call on Close
 * @returns webSocket
 */
MangoAPI.prototype.registerForAlarmEvents = function(events, levels, onMessage, onError, onOpen, onClose) {
    var socket = this.openSocket('/rest/v1/websocket/events');
    socket.onopen = function() {
        //Register for recieving point values
        // using a PointValueRegistrationModel
        socket.send(JSON.stringify({
            eventTypes: events,
            levels: levels
        }));
        onOpen();
    };
    socket.onclose = onClose;
    socket.onmessage = function(event) {
        onMessage(JSON.parse(event.data));
    };
    return socket;
};

/**
 * Modify the existing events for the logged in user
 * @param {Object} web socket 
 * @param {Array} - ['ACKNOWLEGED', 'RAISED', 'RETURN_TO_NORMAL', 'DEACTIVATED']
 * @param {Array} - ['DO_NOT_LOG', ''....]
 *  
 */
MangoAPI.prototype.modifyRegisteredAlarmEvents = function(socket, events, levels) {
    socket.send(JSON.stringify({
        eventTypes: events,
        levels: levels
    }));
};

/**
 * Get Mango translations for use with the Globalize JS library
 * 
 * @param {string} namespace - (optional) limits results to the given namespace, i.e. the part of the key
 * before the first period
 * @param {string} language - (optional) returns translations for the given language, otherwise returns
 * the language specified by the browser or otherwise specified by the user
 * @returns {Object} { locale: "languageCode-countryCode",
 *       translations: {
 *         root: {
 *           "namespace.key": "translation"
 *         },
 *         "languageCode-countryCode": {
 *           "namespace.key": "translation"
 *         }
 *       }
 *     }
 */
MangoAPI.prototype.getTranslations = function(namespace, language) {
    var url = '/rest/v1/translations';
    
    if (typeof namespace !== 'undefined')
        url += '/' + encodeURIComponent(namespace);
    if (typeof language !== 'undefined')
        url += '?language=' + encodeURIComponent(language);
    
    return this.ajax({
        url: url
    });
};

/**
 * Save Existing JSON Data
 * 
 * @param data - data to save
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.putJsonData = function(xid, path, readPermissions, editPermissions, name, jsonData) {
    var url = "/rest/v1/json-data/" + encodeURIComponent(xid);
    
    if((typeof path != 'undefined') && (path !== ''))
    	url += "/" + encodeURIComponent(path);
    
    var hasQuestion = false;
    if((typeof readPermissions != 'undefined') && (readPermissions !== "")){
    	if(hasQuestion === false){
    		url += "?readPermissions=";
    		hasQuestion = true;
    	}else{
    		url += "&readPermissions=";
    	}
    	url += readPermissions;
    }
	
    if((typeof editPermissions != 'undefined') && (editPermissions !== "")){
    	if(hasQuestion === false){
    		url += "?editPermissions=";
    		hasQuestion = true;
    	}else{
    		url += "&editPermissions=";
    	}
    	url += editPermissions;
    }
    
    if((typeof name != 'undefined')&& (name !== "")){
    	if(hasQuestion === false){
    		url += "?name=";
    		hasQuestion = true;
    	}else{
    		url += "&name=";
    	}
    	url += name;
    }
    
	return this.ajax({
        type: "PUT",
        url : url,
        contentType: "application/json",
        data: JSON.stringify(jsonData)
    });
};

/**
 * Save JSON Data
 * 
 * @param user - user to add
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.postJsonData = function(xid, path, readPermissions, editPermissions, name, jsonData) {
    var url = "/rest/v1/json-data/" + encodeURIComponent(xid);
    
    if((typeof path != 'undefined') && (path !== ''))
    	url += "/" + encodeURIComponent(path);
    
    var hasQuestion = false;
    if((typeof readPermissions != 'undefined') && (readPermissions !== "")){
    	if(hasQuestion === false){
    		url += "?readPermissions=";
    		hasQuestion = true;
    	}else{
    		url += "&readPermissions=";
    	}
    	url += readPermissions;
    }
	
    if((typeof editPermissions != 'undefined') && (editPermissions !== "")){
    	if(hasQuestion === false){
    		url += "?editPermissions=";
    		hasQuestion = true;
    	}else{
    		url += "&editPermissions=";
    	}
    	url += editPermissions;
    }
    
    if((typeof name != 'undefined')&& (name !== "")){
    	if(hasQuestion === false){
    		url += "?name=";
    		hasQuestion = true;
    	}else{
    		url += "&name=";
    	}
    	url += name;
    }
    
	return this.ajax({
        type: "POST",
        url : url,
        contentType: "application/json",
        data: JSON.stringify(jsonData)
    });
};

/**
 * Delete JSON Data
 * 
 * @param xid - xid to remove
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.deleteJsonData = function(xid, path) {
	
	var url = "/rest/v1/json-data/" + encodeURIComponent(xid);
	if((typeof path != 'undefined')&&( path !== ''))
		url += "/" + encodeURIComponent(path);
	
    return this.ajax({
        type: "DELETE",
        url : url
    });
};

/**
 * Get JSON Data
 * 
 * @param path - path to data
 * @return promise, resolved with data when done
 */
MangoAPI.prototype.getJsonData = function(xid, path) {
	
	 var url = "/rest/v1/json-data/" + encodeURIComponent(xid);
	 if((typeof path != 'undefined')&&( path !== ''))
		 url += "/" + path;
	 
    return this.ajax({
        url : url
    });
};

/**
 * Default Options for Ajax
 * @type {Object} 
 */
MangoAPI.prototype.defaultAjaxOptions = {
    dataType: 'json'
};

/**
 * Create Ajax Request
 * @param {Object} ajaxOptions - Ajax Options
 * @return {Promise} resolved when Ajax request complete
 */
MangoAPI.prototype.ajax = function(ajaxOptions) {
    ajaxOptions = $.extend({}, this.defaultAjaxOptions, ajaxOptions);
    ajaxOptions.url = this.baseUrl + ajaxOptions.url;
    
    var deferred = $.Deferred();
    var ajax = $.ajax(ajaxOptions).done(function() {
        deferred.resolve.apply(deferred, arguments);
    }).fail(function(jqXHR, textStatus, errorThrown) {
        var failObject = {
            type: 'jqXHR',
            jqXHR: jqXHR,
            url: this.url,
            mangoMessage: jqXHR.getResponseHeader("errors"),
            textStatus: textStatus,
            errorThrown: errorThrown
        };
        deferred.reject(failObject);
    });
    
    var promise = deferred.promise();
    MangoAPI.makeCancellable(promise, ajax.abort.bind(ajax));
    
    return promise;
};

/**
 * Load JSON 
 * @return {Promise} resolved when done
 */
MangoAPI.prototype.loadJson = function() {
    var promiseArray = [];
    for (var i = 0; i < arguments.length; i++) {
        promiseArray.push(this.ajax({
            url: arguments[i],
            dataType: 'json'
        }));
    }
    return MangoAPI.when(promiseArray);
};

/**
 * Stores promises for translation namespaces
 */
MangoAPI.prototype.loadedTranslationNamespaces = {};

/**
 * Stores promise for globalize
 */
MangoAPI.prototype.globalizePromise = null;

/**
 * Locale to use for translations, empty for server default
 */
MangoAPI.prototype.locale = '';

/**
 * Sets up Globalize by retrieving translations from Mango
 * 
 * @param namespace... - zero or more namespace strings
 * @returns promise, resolved when Globalize is ready
 */
MangoAPI.prototype.setupGlobalize = function() {
    var self = this;
    
    // Load globalize using RequireJS and load likelySubtags
    if (!this.globalizePromise) {
        this.globalizePromise = MangoAPI.when([
                MangoAPI.requirePromise(['globalize', 'globalize/message', 'cldr/unresolved']),
                self.loadJson('/resources/cldr-data/supplemental/likelySubtags.json')])
            .then(MangoAPI.firstArrayArg)
            .then(function(Globalize, likelySubtags) {
                Globalize.load(likelySubtags);
                return Globalize;
            });
    }
    
    var locale = null;
    
    var promiseArray = [this.globalizePromise];
    var loadMessages = function(Globalize, translations) {
    	Globalize.loadMessages(translations.translations);
    	return translations;
    };
    
    // request each namespace (if we havent already fetched it) and call
    // Globalize.loadMessages()
    for (var i = 0; i < arguments.length; i++) {
        var namespace = arguments[i];
        var nsPromise = this.loadedTranslationNamespaces[namespace];
        if (!nsPromise) {
            var trRequest = self.locale ? self.getTranslations(namespace, self.locale) :
                self.getTranslations(namespace);
            nsPromise = MangoAPI.when([this.globalizePromise, trRequest])
            .then(MangoAPI.firstArrayArg)
            .then(loadMessages);
            this.loadedTranslationNamespaces[namespace] = nsPromise;
        }
        promiseArray.push(nsPromise);
    }
    
    return MangoAPI.when(promiseArray).then(MangoAPI.firstArrayArg)
        .then(function(Globalize) {
            // check a locale is set on Globalize
            if (!Globalize.locale()) {
                if (self.locale) {
                    Globalize.locale(self.locale);
                } else if (arguments.length > 1) {
                    // use whatever locale the server returned
                    Globalize.locale(arguments[1].locale);
                }
            }
            
            // easy access to formatMessage which catches errors
            Globalize.tr = function() {
                try {
                    return this.formatMessage.apply(this, arguments);
                }
                catch(error) {
                    return 'Error translating "' + arguments[0] + '"';
                }
            };
            
            return Globalize;
    });
};

/**
 * Retrieves the client (browser), server or user (set on the users page) timezone
 * 
 * @param {string} source - 'client', 'server' or 'user'
 * @param {Object} user - required for 'server' or 'user'
 */
MangoAPI.getTimezone = function(source, user) {
    var timezone;
    if (source === 'client') {
        timezone = jstz.determine().name();
    } else if (source === 'server') {
        timezone = user.systemTimezone;
    } else if (source === 'user') {
        timezone = user.timezone || user.systemTimezone;
    }
    return timezone;
};

/**
 * Sets the default timezone for moment-timezone
 * 
 * @param {string} source - 'client', 'server' or 'user'
 * @param {Object} user - required for 'server' or 'user'
 */
MangoAPI.setDefaultTimezone = function(source, user) {
    var timezone = MangoAPI.getTimezone(source, user);
    if (timezone) moment.tz.setDefault(timezone);
    return timezone;
};

MangoAPI.prototype.getTimezone = function(source) {
    if (source === 'client')
        return MangoAPI.resolvedPromise(MangoAPI.getTimezone(source));
    
    return this.getCurrentUser().then(function(user) {
        return MangoAPI.getTimezone(source, user);
    });
};

MangoAPI.prototype.setDefaultTimezone = function(source) {
    return this.getTimezone(source).then(function(timezone) {
        if (timezone) moment.tz.setDefault(timezone);
        return timezone;
    });
};

/**
 * The default MangoAPI instance - i.e. no baseUrl, but can be replaced
 */
MangoAPI.defaultApi = new MangoAPI();

/**
 * Make a promise cancellable.
 * 
 * The cancel function should stop the action which the promise represents then
 * reject the deferred
 * 
 * @param {Promise} promise - promise to cancel
 * @param {method} cancel - function to call to cancel the promise
 */
MangoAPI.makeCancellable = function(promise, cancel) {
    // assume promise.then has been replaced if cancel exists
    if (!promise.cancel) {
        promise.cancel = cancel;
        MangoAPI.replaceThen(promise);
    }
    
    return promise;
};

/**
 * Replace Then On a Promise with the Mango Promise
 * 
 * @param {Promise} promise - promise to replace then on
 * @returns {Promise} promise with new Mango Then in place
 */
MangoAPI.replaceThen = function(promise) {
    var originalThen = promise.then;
    promise.then = function() {
        var newPromise = originalThen.apply(this, arguments);
        newPromise.cancel = this.cancel;
        MangoAPI.replaceThen(newPromise);
        return newPromise;
    };
};

/**
 * An extension of $.when that preserves the cancel method and cancels all the promises
 * when any individual promise fails
 * 
 * @param promises - array of promises
 */
MangoAPI.when = function(promises) {
    var cancelling = false;
    var promise = $.when.apply($, promises);
    
    // assume promise.then has been replaced
    if (!promise.cancel) {
        promise.cancel = function() {
            cancelling = true;
            for (var i in promises) {
                promises[i].cancel();
            }
        };
        
        // triggered when at least one promise has failed, ensure all other promises are cancelled
        promise.fail(function() {
            // prevents cancel from running twice as fail will be triggered when
            // the first promise is cancelled
            if (!cancelling) {
                this.cancel();
            }
        });
        
        MangoAPI.replaceThen(promise);
    }
    return promise;
};

/**
 * Create a Rejected Promise
 * @returns {Promise} promise that has been rejected
 */
MangoAPI.rejectedPromise = function() {
    var deferred = $.Deferred();
    return deferred.reject.apply(deferred, arguments).promise();
};

/**
 * Create a Resolved Promise
 * 
 * @returns {Promise} promise that has been resolved
 */
MangoAPI.resolvedPromise = function() {
    var deferred = $.Deferred();
    return deferred.resolve.apply(deferred, arguments).promise();
};

/**
 * Create a Promise with a set of Require Dependencies
 * @param {Array} dependencyArray - Array of required dependencies
 * @returns {Promise}
 */
MangoAPI.requirePromise = function(dependencyArray) {
    var deferred = $.Deferred();
    
    require(dependencyArray, function() {
        deferred.resolve.apply(deferred, arguments);
    }, function() {
        deferred.reject.apply(deferred, arguments);
    });
    
    return deferred.promise();
};

/**
 * Get the User Language
 * @returns {Object} user language
 */
MangoAPI.userLanguage = function() {
    return navigator.languages ? navigator.languages[0] : (navigator.language || navigator.userLanguage);
};

/**
 * Not sure what this does
 */
MangoAPI.firstArrayArg = function() {
    var firstArgs = [];
    for (var i = 0; i < arguments.length; i++) {
        var argsI = arguments[i];
        if ($.isArray(argsI) && argsI.length > 0)
            firstArgs.push(argsI[0]);
        else
            firstArgs.push(argsI);
    }
    
    var deferred = $.Deferred();
    return deferred.resolve.apply(deferred, firstArgs).promise();
};

/**
 * Returns an array of all points contained in a folder and its subfolders.
 * Only the folder parameter is necessary
 */
MangoAPI.pointsInFolder = function(folder, path, points) {
    if (typeof path == 'undefined') {
        path = '';
    }
    
    if (path === '') {
        if (folder.name != 'root') {
            path = folder.name;
        }
    }
    else {
        path += '/' + folder.name;
    }
    
    if (typeof points == 'undefined') {
        points = [];
    }
    
    $.each(folder.points, function(id, point) {
        point.path = path;
        points.push(point);
    });
    
    $.each(folder.subfolders, function(id, subfolder) {
        MangoAPI.pointsInFolder(subfolder, path, points);
    });
    
    return points;
};

/**
 * Parse up some folder paths?
 * @param {Folder} folder - Folder Object
 * @param {Array.<string>} path - Path array
 * @param {Object} result - final result
 */
MangoAPI.folderPaths = function(folder, path, result) {
    if (typeof path === 'undefined')
        path = [];
    if (typeof result === 'undefined')
        result = {};

    if (folder.name != 'Root')
        path.push(folder.name);
    result[folder.id] = path.slice();
    
    for (var i in folder.subfolders) {
        MangoAPI.folderPaths(folder.subfolders[i], path, result);
        path.pop();
    }

    return result;
};

/**
 * Logs an error to the console
 * @param {Object} jqXHR JQuery XHR Object
 * @param {string} textStatus response status
 * @param {Object} error response error
 * @param {string} mangoMessage Message from Mango REST API
 */
MangoAPI.logError = function(errorObject) {
    if (!console || !errorObject)
        return;
    
    var logLevel = 'error';
    var message;
    switch(errorObject.type) {
    case 'loadNotNeeded':
    case 'tooMuchData':
    case 'noData':
        // dont log these messages
        return;
    case 'providerDisabled':
        message = errorObject.description;
        logLevel = 'warn';
        break;
    case 'jqXHR':
        if (errorObject.textStatus === 'abort') {
            message = 'Mango XHR request was cancelled';
            logLevel = 'warn';
        } else {
            message = "Mango XHR request failed";
            if (errorObject.textStatus)
                message += ", status=" + errorObject.textStatus;
            if (errorObject.errorThrown)
                message += ", error=" + errorObject.errorThrown;
            if (errorObject.mangoMessage)
                message += ", message=" + errorObject.mangoMessage;
        }
        message += ", url=" + errorObject.url;
        break;
    default:
        message = "Generic error: " + errorObject.type;
    }
    
    // default to console.log() if console.error() etc aren't available 
    logLevel = console[logLevel] ? logLevel : 'log';
    
    // log the message
    console[logLevel](message);
};

/**
 * Retrieves a url parameter
 * @param {string} name parameter name
 */
MangoAPI.urlParameter = function(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[undefined,""])[1].replace(/\+/g, '%20'))||null;
};

/**
 * Create Query Comparison
 * @param {string} attribute - Attribute of item to compare
 * @param {string} ['EQUAL_TO', 'NOT_EQUAL_TO', 'LESS_THAN'] type - Type of comparison
 * @param {number|string} condition - value to compare to 
 * @returns {QueryComparison}
 */
MangoAPI.createQueryComparison = function(attribute, type, condition){
	return {
		attribute: attribute,
		comparisonType: type,
		condition: condition
	};
};

return MangoAPI;

}); // close define
