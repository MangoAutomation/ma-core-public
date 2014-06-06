/*
 * Basic Dojo Table
 *  Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
 *  @author Terry Packer
 */

define(["dojo/_base/declare", "dgrid/OnDemandGrid", "dgrid/extensions/ColumnResizer", "dgrid/extensions/DijitRegistry", "dojo/on"],

    function(declare, OnDemandGrid, ColumnResizer, DijitRegistry, on){
        return declare("infinite.InfiniteTable", null, {
            
            gridId: null, //my grid's div
            columns: null, //my columns
            grid: null, //my Table
            store: null, //my memory/remote store
            defaultSort: null,
            defaultQuery: null,
            renderRowHook: null,
            
            loadingMessage: '<div class="dgridLoadNoData"><img src="/images/hourglass.png" title="' + mango.i18n['common.loading'] + '" /></div>',
            noDataMessage: '<div class="dgridLoadNoData">' + mangoMsg['table.noData'] + '</div>',

            
            
            
            constructor: function(options){
                var _this = this;
                this.defaultQuery = {};

                for(var i in options) {
                    this[i] = options[i];
                }
                
                this.grid = dojo.declare([OnDemandGrid, ColumnResizer, DijitRegistry])({
                    adjustLastColumn: true, 
                    minWidth: 40, /* min width of adjustable columns */
                    store: _this.store,
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
            }
            
        });//end declare
    
    
    
    } //Function
); //Define
