/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

define(["deltamation/DwrStore",
         "dojo/_base/declare", "dojo/store/Cache", "dojo/store/Memory", "dojo/store/Observable"],
function(DwrStore, declare, Cache, Memory, Observable) {

return declare("deltamation.CachedDwrStore", null, {
    constructor: function(dwrObject, name) {
        this.name = name;
        dwrObject.name = name;
        this.memory = new Observable(new Memory({}));
        this.dwr = new Observable(new DwrStore({dwr: dwrObject}));
        this.cache = new Observable(new Cache(this.dwr, this.memory));
    },
    memory: null,
    dwr: null,
    cache: null,
    name: null
}); // declare
}); // define