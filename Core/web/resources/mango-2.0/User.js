/**
 * Access to the Mango Rest API
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {User} mango/User
 */
define(['jquery'], function($) {

function User(options) {
    $.extend(this, options);
}

/**
 * Check if a user has any of the given permissions
 */
User.prototype.hasPermission = function(desiredPerms) {
    if (this.admin) return true;
    
    if (typeof desiredPerms === 'string') {
        desiredPerms = desiredPerms.split(',');
    }
    var userPerms = this.permissions.split(',');
    
    for (var i = 0; i < desiredPerms.length; i++) {
        if ($.inArray(desiredPerms[i], userPerms) > -1)
        	return true;
    }
    
    return false;
};

return User;

}); // define