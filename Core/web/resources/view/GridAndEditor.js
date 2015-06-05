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
    if (options.grid && typeof options.grid.select === 'function') {
        this.gridSelectable = true;
    }
    
    ItemEditor.apply(this, arguments);
    
    if (!this.grid) {
        throw 'grid must be specified';
    }
}

GridAndEditor.prototype = Object.create(ItemEditor.prototype);

GridAndEditor.prototype.grid = null;
GridAndEditor.prototype.gridSelectable = false;

GridAndEditor.prototype.pageSetup = function() {
    ItemEditor.prototype.pageSetup.apply(this, arguments);
    
    var self = this;
    
    if (this.gridSelectable) {
        this.grid.on('dgrid-select', function(event) {
            if (self.currentItem && self.currentItemDirty) {
                // TODO show proper dialog
                if (!confirm('Discard changes?')) {
                    self.grid.clearSelection();
                    // TODO reselect old template, this method triggers event again
                    // reportTemplatesGrid.select(currentTemplate);
                }
            }
            
            self.editItem(event.rows[0].data);
        });
    }
};

GridAndEditor.prototype.newItemClick = function() {
    if (this.gridSelectable) {
        this.grid.clearSelection();
    }
    ItemEditor.prototype.newItemClick.apply(this, arguments);
};

return GridAndEditor;

});
