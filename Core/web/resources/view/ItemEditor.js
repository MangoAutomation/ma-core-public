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
    
    this.$buttonScope = this.$buttonScope || this.$editor;
    
    if (!this.store) {
        throw 'store must be specified';
    }
    if (!this.$editor) {
        throw '$editor must be specified';
    }

    var self = this;
    this.setupTranslations().then(function() {
        $(document).ready(function() {
            self.pageSetup();
        });
    }, this.showError);
}

ItemEditor.prototype = Object.create(BaseUIComponent.prototype);

ItemEditor.prototype.store = null;
ItemEditor.prototype.$editor = null;
ItemEditor.prototype.$buttonScope = null;
ItemEditor.prototype.currentItem = null;
ItemEditor.prototype.currentItemDirty = false;
ItemEditor.prototype.propToInputMap = {};

ItemEditor.prototype.pageSetup = function() {
    var self = this;
    
    this.$buttonScope.find('.editor-new').click(this.newItemClick.bind(this));
    this.$buttonScope.find('.editor-delete').click(this.deleteItemClick.bind(this));
    this.$buttonScope.find('.editor-save').click(this.saveItemClick.bind(this));
    this.$buttonScope.find('.editor-cancel').click(this.cancelItemClick.bind(this));
    this.$buttonScope.find('.editor-copy').click(this.copyItemClick.bind(this));
    this.setupHelp(this.$buttonScope);
    
    self.$editor.find('input').on('change', function() {
        if (self.currentItem) {
            self.currentItemDirty = true;
        }
    });
};

ItemEditor.prototype.closeEditor = function() {
    this.$editor.fadeOut();
    this.currentItem = null;
};

ItemEditor.prototype.editItem = function(item) {
    this.$editor.hide();
    
    this.currentItem = item;
    this.currentItemDirty = false;
    
    this.setInputs(item);

    this.$editor.fadeIn();
    this.$editor.find('input:first').focus();
};

ItemEditor.prototype.newItemClick = function() {
    this.editItem(this.createNewItem());
};

ItemEditor.prototype.saveItemClick = function() {
    var self = this;
    this.getInputs(this.currentItem);
    
    this.store.put(this.currentItem).then(function() {
        self.closeEditor();
    }, this.showError);
};

ItemEditor.prototype.deleteItemClick = function() {
    var self = this;
    
    // TODO proper dialog
    if (confirm('Delete?')) {
        var idProp = this.store.idProperty;
        this.store.remove(this.currentItem[idProp]).then(function() {
            self.closeEditor();
        }, this.showError);
    }
};

ItemEditor.prototype.cancelItemClick = function() {
    var close = true;
    
    if (this.currentItem && this.currentItemDirty) {
        // TODO show proper dialog
        if (!confirm('Discard changes?')) {
            close = false;
        }
    }
    
    if (close)
        this.closeEditor();
};

ItemEditor.prototype.copyItemClick = function(event) {
    var item = $(event.target).data('item');
    if (!item)
        item = this.currentItem;
    
    item = $.extend({}, item);
    var idProp = this.store.idProperty;
    delete item[idProp];
    // TODO new XID etc
    
    this.editItem(item);
};

ItemEditor.prototype.createNewItem = function() {
    //return this.store.create();
    return {};
};

ItemEditor.prototype.setInputs = function(item) {
    for (var key in this.propToInputMap) {
        this.propToInputMap[key].val('');
    }
    this.$editor.find('input').val('');
    
    for (key in item) {
        var $input = this.propToInputMap[key];
        if (!$input) {
            $input = this.$editor.find('input[name=' + key + ']');
        }
        $input.val(item[key]);
    }
};

ItemEditor.prototype.getInputs = function(item) {
    for (var key in this.propToInputMap) {
        item[key] = this.propToInputMap[key].val();
    }
    this.$editor.find('input').each(function(i, input) {
        var $input = $(input);
        item[$input.attr('name')] = $input.val();
    });
    return item;
};

return ItemEditor;

});
