/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */

define(['jquery',
         './ItemEditor',
],
function($, ItemEditor) {
'use strict';

function GridAndEditor(options) {
    if (!options.grid) {
        throw 'grid must be specified';
    }
    
    if (typeof options.grid.select !== 'function') {
        throw 'grid must be selectable';
    }
    
    ItemEditor.apply(this, arguments);
}

GridAndEditor.prototype = Object.create(ItemEditor.prototype);

GridAndEditor.prototype.grid = null;
GridAndEditor.prototype.gridSelectable = false;

GridAndEditor.prototype.pageSetup = function() {
    ItemEditor.prototype.pageSetup.apply(this, arguments);
    
    var self = this;
    
    this.grid.on('dgrid-select', function(event) {
        if (event.grid.disableEvent) return;
        
        if (self.currentItem && self.currentItemDirty) {
            // TODO show proper dialog
            if (!confirm('Discard changes?')) {
                event.grid.clearSelection();
                
                // no built in way to inhibit event firing?
                event.grid.disableEvent = true;
                event.grid.select(self.currentItem);
                event.grid.disableEvent = false;
                
                return;
            }
        }
        
        self.editItem(event.rows[0].data);
    });
};

GridAndEditor.prototype.editItem = function() {
    ItemEditor.prototype.editItem.apply(this, arguments);
    if (!this.grid.isSelected(this.currentItem)) {
        this.grid.clearSelection();
        this.grid.select(this.currentItem);
    }
};

GridAndEditor.prototype.closeEditor = function() {
    ItemEditor.prototype.closeEditor.apply(this, arguments);
    this.grid.clearSelection();
};

return GridAndEditor;

});
