/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

define(["dojo/_base/declare", "dojo/_base/array"], function(declare, array) {
return declare("deltamation.ArrayTester", null, {
    constructor: function(data) {
        this.data = data;
    },
    data: null,
    
    test: function(value, object) {
        if (array.indexOf(this.data, value) >= 0)
            return true;
        else
            return false;
    }
}); // declare
}); //define
