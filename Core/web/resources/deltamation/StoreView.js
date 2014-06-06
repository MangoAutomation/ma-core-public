/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

define(["dojo/_base/declare", "dgrid/OnDemandGrid", "dgrid/extensions/ColumnResizer", "dgrid/extensions/DijitRegistry",
         "dijit/form/FilteringSelect", "dijit/form/Button", "dijit/form/ValidationTextBox",
         "dojo/dom-style", "dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dijit/Dialog", "dojo/_base/lang", "dojo/_base/fx", "dojo/fx"],
function(declare, OnDemandGrid, ColumnResizer, DijitRegistry,
        FilteringSelect, Button, ValidationTextBox,
        domStyle, html, put, when, on, Dialog, lang, baseFx, coreFx) {

return declare("deltamation.StoreView", null, {
    prefix: '',
    varName: '',
    viewStore: null,
    editStore: null,
    editUpdatesView: false,
    preInit: null,
    postInit: null,
    postGridInit: null,
    postEditInit: null,
    closeEditOnSave: true,
   
    
    constructor: function(options) {
        this.defaultQuery = {};
        this.columns = {};
        this.buttons = [];
        
        for(var i in options) {
            this[i] = options[i];
        }
        
        if (typeof this.preInit === 'function')
            this.preInit();
        
        var _this = this;
        
        this.grid = dojo.byId(this.gridId);
        this.edit = dojo.byId(this.editId);
        
        if (this.grid) {
            this.initGrid();
        }
        if (this.edit) {
            this.initEdit();
        }
        
        if (typeof this.postInit === 'function')
            this.postInit();
    },
    
    // grid/table properties
    defaultSort: null,
    defaultQuery: null,
    columns: null,
    buttons: null,
    grid: null,
    gridId: null,
    loadingMessage: '<div class="dgridLoadNoData"><img src="/images/hourglass.png" title="' + mango.i18n['common.loading'] + '" /></div>',
    noDataMessage: '<div class="dgridLoadNoData">' + mangoMsg['table.noData'] + '</div>',
    renderRowHook: null,
    addColumns: null, //Method to allow adding of additional columns programatically
    
    initGrid: function() {
        var _this = this;
        
        if (typeof _this.addColumns === 'function')
        	_this.addColumns(); //Add any columns before init-ing grid
        
        if (this.buttons.length > 0)
            this.columns.buttons = {
                label: ' ',
                sortable: false,
                resizable: false,// resizable doesnt seem to work, do in css as well
                renderHeaderCell: function(th){
                	return _this.renderButtonsHeader(th);
                },
                renderCell: function(object, value, node, options) {
                    return _this.renderButtons(object, value, node, options);
                }
            };
        
        
        
        this.grid = dojo.declare([OnDemandGrid, ColumnResizer, DijitRegistry])({
        	adjustLastColumn: false, /* Don't expand last row to fill */
        	minWidth: 40, /* min width of adjustable columns */
            store: _this.viewStore.cache,
            columns: _this.columns,
            loadingMessage: _this.loadingMessage,
            noDataMessage: _this.noDataMessage,
            query: _this.defaultQuery,
            sort: _this.defaultSort,
            renderRow: function(object, options) {
                var row = this.inherited(arguments);
                if (typeof _this.renderRowHook === 'function')
                    _this.renderRowHook(row, object, options);
                return row;
            }
        }, _this.gridId);
        
        on(this.grid, 'dgrid-error', function(event) {
            addErrorDiv(event.error.message);
        });
        
        if (typeof this.postGridInit === 'function')
            this.postGridInit();
    },
    
    imgMap: {'delete': 'delete', edit: 'pencil', 'export': 'emport', copy: 'add', toggleOn: 'database_go', toggleOff: 'database_stop', run: 'control_play_blue'},
    fnMap: {'delete': 'remove', edit: 'open', 'export': 'showExport', copy: 'copy', toggleOn: 'toggle', toggleOff: 'toggle', run: 'run'},
    
    renderButtonsHeader: function(th){
    	
    },
    
    renderButtons: function(object, value, node, options) {
        var id = object.id;
        
        var span = put('span');
        for (var i = 0; i < this.buttons.length; i++) {
            var button = this.buttons[i];
            
            var elementId = button + this.prefix + id;
            var title =  mangoMsg['table.' + button];
            if (!title)
                title = mangoTranslate('table.missingKey',"table."+button);
            
            if (button === 'toggle') {
                if (object.enabled) {
                    button = 'toggleOn';
                }
                else {
                    button = 'toggleOff';
                }
            }
            
            var src = this.imgMap[button];
            if (src.substring(0,1) !== '/')
                src = '/images/' + src + '.png';
            
            var action = this.varName + '.' + this.fnMap[button] + '(' + id + ');';
            
            var img = put(span, 'img.ptr#$[src=$][title=$][onclick=$]', elementId, src, title, action);
        }
        return span;
    },
    
    preRemove: null, //Performed Prior to Dwr Remove Method
    removeCallback: null, //Performed after Dwr Remove Method
    
    remove: function(id, confirmed) {
        var _this = this;
        id = id || _this.currentId;
        if (id < 0)
            return false;
        
        confirmed = confirmed || false;
        if (confirmed || confirm(mangoMsg['table.confirmDelete.' + this.prefix])) {
        	 if(_this.preRemove)
             	_this.preRemove();
        	
        	when(this.editStore.cache.remove(id), function(response) {
                // ok
                //Close the edit pane if any is open
                if (_this.closeEditOnSave) {
                    //hide(_this.edit);
                    coreFx.combine([baseFx.fadeOut({node: _this.edit}),
                                    coreFx.wipeOut({node: _this.edit})]).play();
                }
                
                // remove the row from the view store
                // TODO make this a push from the server side
                if (_this.editUpdatesView)
                    _this.viewStore.cache.remove(id);
                
                if(_this.removeCallback)
                	_this.removeCallback(id);
            }, function(response) {
                //Close the edit pane if any is open
                //hide(_this.edit);
                coreFx.combine([baseFx.fadeOut({node: _this.edit}),
                                coreFx.wipeOut({node: _this.edit})]).play();
                
                if (response.dwrError || typeof response.messages === 'undefined') {
                    // timeout, dwr error etc
                    addErrorDiv(response);
                    return false;
                }
                // error deleting
                addMessages(response.messages);
            });
        }
        return confirmed;
    },
    
    // edit dialog properties

    editId: null,
    edit: null,
    currentId: -1,
    setInputs: null,
    getInputs: null,
    
    initEdit: function() {
        if (typeof this.postEditInit === 'function')
            this.postEditInit();
    },
    
    editXOffset: 18,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: 0,
    
    preOpen: null, //Performed Prior to Dwr Get Method
    openCallback: null, //Performed after Dwr Get Method
    
    /**
     * id of item to open
     * options - { posX: posY: voToLoad: }
     */
    open: function(id, options) {
 
        this.currentId = id;
        var _this = this;
        options = options || {};
        var posX = options.posX;
        var posY = options.posY;
        
        // firstly position the div
        if (typeof posY == 'undefined' || typeof posX == 'undefined') {
            //Get the img for the edit of this entry and key off of it for position
            var img, offsetX, offsetY;
            if (id > 0) {
                img = "edit" + this.prefix + id;
                offsetX = this.editXOffset;
                offsetY = this.editYOffset;
            }
            else {
                img = "add" + this.prefix;
                offsetX = this.addXOffset;
                offsetY = this.addYOffset;
            }
            var position = html.position(img, true);
            posX = position.x + offsetX;
            posY = position.y + offsetY;
        }
        domStyle.set(this.edit, "top", posY + "px");
        domStyle.set(this.edit, "left", posX + "px");
        
        if(_this.preOpen)
        	_this.preOpen();
        
        if (options.voToLoad) {
            _this.setInputs(options.voToLoad);
        	hideContextualMessages(_this.edit);
            //show(this.edit);
            coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                            coreFx.wipeIn({node: _this.edit})]).play();
            if(_this.openCallback)
            	_this.openCallback();
        }
        else {
            // always load from dwr
            // TODO use push from the server side
            // so cache is always up to date
            when(this.editStore.dwr.get(id), function(vo) {
                // ok
                _this.setInputs(vo);
                //Hide contextual messages
            	hideContextualMessages(_this.edit);
                //show(_this.edit);
                coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                                coreFx.wipeIn({node: _this.edit})]).play();
                if(_this.openCallback)
                	_this.openCallback();
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            });
        }
    },

    closeCallback: null, //Performed post close
    // close the window
    close: function() {
        //hide(this.edit);
        coreFx.combine([baseFx.fadeOut({node: this.edit}),
                        coreFx.wipeOut({node: this.edit})]).play();
        if(this.closeCallback)
        	this.closeCallback();
    },
    
    preSave: null, //Preformed pre save Dwr before Get Inputs
    saveCallback: null, //Performed post save Dwr on successful saves only
    save: function() {
        var _this = this;
        if(_this.preSave)
        	_this.preSave();
        
        var vo = this.getInputs();
        
        when(this.editStore.cache.put(vo, {overwrite: true}), function(vo) {
            // ok
            _this.currentId = vo.id;
            if (_this.closeEditOnSave)
                _this.close();
            
            // get new row from view store
            // TODO make this a push from the server side
            if (_this.editUpdatesView) {
                when(_this.viewStore.dwr.get(vo.id), function(viewVo) {
                    _this.viewStore.cache.put(viewVo, {overwrite: true});
                });
            }
            if(_this.saveCallback)
            	_this.saveCallback();
        }, function(response) {
            if (response.dwrError || typeof response.messages === 'undefined') {
                // timeout, dwr error etc
                addErrorDiv(response);
                return;
            }
            // validation error
            //TODO fix this up to use _this.prefix when searching for the context key
            for (var i = 0 ; i < response.messages.length; i++) {
                var m = response.messages[i];
                var x = _this[m.contextKey] || _this[m.contextKey + 'Picker'];
                if (x) {
                    x.focus();
                    x.displayMessage(m.contextualMessage);
                    break;
                }
                else {
                    addMessage(m);
                }
            }
        });
    },
    
    showExport: function(id) {
        this.editStore.dwr.dwr.jsonExport(id, function(json) {
            $set("exportData", json);
            exportDialog.show();
        });
    },
    
    copy: function(id) {
        var _this = this;
        _this.copyFromId = id;
        this.editStore.dwr.dwr.getCopy(id, function(response) {
            _this.open(-1, {voToLoad: response.data.vo});
        });
    },
    
    run: function(id) {
        if (id <= 0)
            return;
        this.editStore.dwr.dwr.run(id);
    },
    
    /**
     * Add a row to the table by getting from DB
     */
    addRow: function(id){
        var _this = this;
        
        when(_this.viewStore.dwr.get(id), function(viewVo) {
            _this.viewStore.cache.put(viewVo,{overwrite: true}); //Will either update or add
        });
    },
    
    addRows: function(rows){
    	var _this = this;
        for (var i = 0 ; i < rows.length; i++) {
        	_this.viewStore.cache.put(rows[i], {overwrite: true});
    	}
    }
    
    
    
}); // declare
}); // require
