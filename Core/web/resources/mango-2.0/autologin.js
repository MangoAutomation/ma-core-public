/**
 * Data Provider Base Class
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {autologin} mango/autologin
 */
define(['./api'], function(MangoAPI) {
"use strict";

/**
 * @function Perform Login
 * @param {Object} config - {username: string, password: string}
 * @returns {Promise} - Resolved when logged in
 */
var autologin = function(config) {
    if (typeof config.username === 'undefined') {
        config.username = 'guest';
    }
    if (typeof config.password === 'undefined') {
        config.password = '';
    }
    
    var mangoApi = config.mangoApi || MangoAPI.defaultApi;
    
    // login if not authenticated already
    var promise = mangoApi.login(config.username, config.password, false)
    .done(function(user) {
        if (config.mainScript) {
            require([config.mainScript]);
        }
        else if (config.redirectUrl) {
            window.location.href = config.redirectUrl;
        }
        else if (config.redirectToHome) {
            if (user.homeUrl) {
                window.location.href = user.homeUrl;
            }
            else {
                window.location.href = '/';
            }
        }
    });
    
    return promise;
};

return autologin;

}); // define
