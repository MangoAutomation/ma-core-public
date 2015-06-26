/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */
 
 define(['dojo/_base/declare',
         'dstore/Memory'
], function(declare, Memory) {
'use strict';

/**
 * Declares a memory dstore which automatically gives added items an automatic ID property
 */
return declare(Memory, {
    nextAutoId: 0,
    idProperty: 'autoId',
    
    putSync: function (object, options) {
        if (this.idProperty === 'autoId' && !(options && options.overwrite) &&
                typeof object.autoId === 'undefined') {
            object.autoId = this.nextAutoId++;
        }
        return Memory.prototype.putSync.apply(this, arguments);
    },
    
    setData: function (data) {
        if (this.idProperty === 'autoId') {
            for (var i = 0; i < data.length; i++) {
                data[i].autoId = i;
            }
            this.nextAutoId = i;
        }
        return Memory.prototype.setData.apply(this, arguments);
    }
});

});