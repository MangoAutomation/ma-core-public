/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */

define(['jquery',
         './BaseUIComponent',
],
function($, BaseUIComponent) {
'use strict';

function ItemEditor(options) {
    BaseUIComponent.apply(this, arguments);
    
    if (!this.$scope) {
        throw '$scope must be specified';
    }
    
    this.$editor = this.$editor || this.$scope;
    
    if (!this.store) {
        throw 'store must be specified';
    }
}

ItemEditor.prototype = Object.create(BaseUIComponent.prototype);

ItemEditor.prototype.store = null;
ItemEditor.prototype.$editor = null;
ItemEditor.prototype.currentItem = null;
ItemEditor.prototype.currentItemDirty = false;
ItemEditor.prototype.nameAttr = 'name';

ItemEditor.prototype.documentReady = function() {
    BaseUIComponent.prototype.documentReady.apply(this, arguments);
    
    var self = this;
    
    this.$scope.find('.editor-new').click(this.newItemClick.bind(this));
    this.$scope.find('.editor-delete').mousedown(false);
    this.$scope.find('.editor-delete').click(this.deleteItemClick.bind(this));
    this.$scope.find('.editor-save').click(this.saveItemClick.bind(this));
    this.$scope.find('.editor-cancel').click(this.cancelItemClick.bind(this));
    this.$scope.find('.editor-copy').mousedown(false);
    this.$scope.find('.editor-copy').click(this.copyItemClick.bind(this));
    
    self.$scope.find('input').on('change keydown', function() {
        if (self.currentItem) {
            self.currentItemDirty = true;
        }
    });
};

ItemEditor.prototype.closeEditor = function() {
    this.$editor.fadeOut();
    $(this).trigger('editorHidden');
    this.currentItem = null;
};

ItemEditor.prototype.editItem = function(item) {
    this.$editor.hide();
    
    this.currentItem = item;
    this.currentItemDirty = false;
    
    this.setInputs(item);

    this.$editor.fadeIn();
    $(this).trigger('editorShown');
    
    // resize any dgrids inside editor
    this.dgridResize(this.$editor);
    
    this.$scope.find('input:first').focus();
};

ItemEditor.prototype.confirmDiscard = function(event) {
    var self = this;
    var confirmed = true;
    
    if (this.currentItem && this.currentItemDirty) {
        var confirmTitle = this.tr('common.discardChanges');
        var confirmMessage = self.tr('common.discardChangesLong', self.currentItem[self.nameAttr] || '');
        
        confirmed = this.confirm(confirmTitle, confirmMessage);
    }
    
    return $.when(confirmed);
};

ItemEditor.prototype.newItemClick = function(event) {
    var self = this;
    this.confirmDiscard().done(function() {
        self.editItem(self.createNewItem());
    });
};

ItemEditor.prototype.saveItemClick = function(event) {
    var self = this;
    this.getInputs(this.currentItem);
    
    this.store.put(this.currentItem).then(function() {
        self.closeEditor();
    }, this.showError);
};

ItemEditor.prototype.deleteItemClick = function(event) {
    var self = this;
    var item = event.target && $(event.target).data('item') || self.currentItem;
    
    var confirmTitle = this.tr('common.confirmDelete');
    var confirmMessage = this.tr('common.confirmDeleteLong', item[this.nameAttr]);
    
    this.confirm(confirmTitle, confirmMessage).done(function() {
        var idProp = self.store.idProperty;
        self.store.remove(item[idProp]).then(function() {
            if (item === self.currentItem) {
                self.closeEditor();
            }
        }, self.showError);
    });
    
    event.stopPropagation();
};

ItemEditor.prototype.cancelItemClick = function(event) {
    var self = this;
    this.confirmDiscard().done(function() {
        self.closeEditor();
    });
};

ItemEditor.prototype.copyItemClick = function(event) {
    var item = event.target && $(event.target).data('item') || this.currentItem;
    
    var self = this;
    this.confirmDiscard().done(function() {
        item = $.extend({}, item);
        var idProp = self.store.idProperty;
        delete item[idProp];
        delete item.xid;
        
        self.editItem(item);
    });
    
    event.stopPropagation();
};

ItemEditor.prototype.createNewItem = function() {
    //return this.store.create();
    return {};
};

return ItemEditor;

});
