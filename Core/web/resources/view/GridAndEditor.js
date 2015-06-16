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

GridAndEditor.prototype.documentReady = function() {
    ItemEditor.prototype.documentReady.apply(this, arguments);
    
    var self = this;
    
    this.grid.on('dgrid-select', function(event) {
        if (event.grid.disableEvent) return;

        self.confirmDiscard().done(function() {
            // deep copy data so we can edit the item without modifying original
            var item = $.extend(true, {}, event.rows[0].data);
            self.editItem(item);
        }).fail(function() {
            event.grid.clearSelection();
            
            // no built in way to inhibit event firing?
            event.grid.disableEvent = true;
            event.grid.select(self.currentItem);
            event.grid.disableEvent = false;
        });
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

GridAndEditor.createButtons = function(buttons, imgBase, imgSize) {
    var renderCell = function(object, value, node, options) {
        var $span = $('<span>');
        
        for (var i = 0; i < buttons.length; i++) {
            var button = buttons[i];
            
            var $img = $('<img>');
            
            for (var attr in button) {
                switch(attr) {
                case 'src':
                case 'height':
                case 'width':
                case 'alt':
                case 'onclick':
                    continue;
                }
                $img.attr(attr, button[attr]);
            }
            
            $img.addClass('ptr');
            $img.attr('src', imgBase + button.src);
            $img.attr('height', '' + imgSize);
            $img.attr('width', '' + imgSize);
            if (button.title) {
                $img.attr('alt', button.title);
            }
            $img.data('item', object);
            $img.click(button.onclick);
            // cancel mousedown events so dgrid doesn't select row
            $img.mousedown(false);
            
            $span.append($img);
        }
        
        return $span[0];
    };
    
    return renderCell;
};

return GridAndEditor;

});
