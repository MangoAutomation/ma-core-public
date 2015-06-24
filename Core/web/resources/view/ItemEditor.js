/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */

define(['jquery',
         './BaseUIComponent',
         'dijit/registry'
],
function($, BaseUIComponent, registry) {
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
ItemEditor.prototype.currentItemModified = false;
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
    
    this.$scope.find('input, select, textarea').on('change keydown', self.setItemModified.bind(self));
    this.$scope.find('.dgrid').each(function(i, node) {
        var grid = registry.byNode(node);
        if (grid && grid.collection) {
            grid.collection.on('add, update, delete', self.setItemModified.bind(self));
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
    
    this.currentItem = null;
    this.setInputs(item);
    
    this.currentItem = item;
    this.currentItemModified = false;
    this.$editor.removeClass('editor-item-modified');
    
    this.$editor.fadeIn();
    $(this).trigger('editorShown');
    
    // resize any dgrids inside editor
    this.dgridResize(this.$editor);
    
    this.$scope.find('input:first').focus();
};

ItemEditor.prototype.setItemModified = function(event) {
    if (this.currentItem) {
        this.currentItemModified = true;
        this.$editor.addClass('editor-item-modified');
        $(this).trigger('currentItemModified');
    }
};

ItemEditor.prototype.confirmDiscard = function(event) {
    var self = this;
    var confirmed = true;
    
    if (this.currentItem && this.currentItemModified) {
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
        self.$editor.addClass('editor-item-modified');
    });
};

ItemEditor.prototype.saveItemClick = function(event) {
    var self = this;
    this.getInputs(this.currentItem);
    
    this.store.put(this.currentItem).then(function() {
        self.closeEditor();
        self.showSuccess(tr('common.success'));
    }, self.dstoreErrorHandler.bind(self));
};

ItemEditor.prototype.deleteItemClick = function(event) {
    var self = this;
    var item = event.target && $(event.target).data('item') || self.currentItem;
    
    var confirmTitle = this.tr('common.confirmDelete');
    var confirmMessage = this.tr('common.confirmDeleteLong', item[this.nameAttr]);
    
    this.confirm(confirmTitle, confirmMessage).done(function() {
        var idProp = self.store.idProperty;
        self.store.remove(item[idProp]).then(function() {
            if (self.store.getIdentity(item) == self.store.getIdentity(self.currentItem)) {
                self.closeEditor();
            }
        }, self.dstoreErrorHandler.bind(self));
    });
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
        var copy = self.copyItem(item);
        self.editItem(copy);
    });
};

ItemEditor.prototype.copyItem = function(item) {
    var copy = $.extend(true, {}, item);
    copy.name = this.tr('common.copyPrefix', copy.name);
    delete copy[this.store.idProperty];
    delete copy.xid;
    return copy;
};

ItemEditor.prototype.createNewItem = function() {
    //return this.store.create();
    return {};
};

return ItemEditor;

});
