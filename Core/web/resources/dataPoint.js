/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
var dataSources;
var dataPointsDataSourceId;

//TO Eventually be merged into Store View
var pointTableFilter = new Array();

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query","dojo/dom-construct","dijit/form/TextBox",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,
		domStyle,html,put,when,on,
		baseFx,coreFx,query,domConstruct,TextBox) {


	
dataPoints = new StoreView({
	
	
    prefix: 'DataPoint',
    varName: 'dataPoints',
    viewStore: stores.dataPoint,
    editStore: stores.dataPoint,
    editUpdatesView: true,
    gridId: 'dataPointTable',
    editId: 'pointDetails',
    defaultSort: [{attribute: "deviceName"},{attribute: "name"}],
    defaultQuery: {dataSourceId: [null]},
    columns: {
    	
        deviceName: {
    		label: mangoMsg['dsEdit.deviceName'],
    		renderHeaderCell: function(th){
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 8em",
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsEdit.deviceName'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete pointTableFilter['deviceName'];
    					else
    						pointTableFilter['deviceName'] = new RegExp("^.*"+value+".*$");
    					
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set('query',pointTableFilter);
    				});
    				
    				return div;
    		},
        },

        name: {
    		label: mangoMsg['dsList.name'],
    		renderHeaderCell: function(th){
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 10em",
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['dsList.name'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete pointTableFilter['name'];
    					else
    						pointTableFilter['name'] = new RegExp("^.*"+value+".*$");
    					
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set('query',pointTableFilter);
    				});
    				
    				return div;
    		},
        },
    	dataTypeString : mangoMsg['dsEdit.pointDataType'],
    	
    },
    
    buttons: ['toggle','edit','delete','copy','export'],
    
    preInit: function() {
    },
    
    postGridInit: function() {
    },
    
    setInputs: function(vo) {

    	this.currentId = vo.id;
    	this.name.set('value',vo.name);
    	this.xid.set('value',vo.xid);
    	dataPointsDataSourceId = vo.dataSourceId;
    	
    	//this.enabled.set('value',vo.enabled);
    	//Setup for the point Impl CB
    	currentPoint = vo;
    	if( typeof editPointCBImpl == 'function') editPointCBImpl(vo.pointLocator);

    },
    
    getInputs: function() {
        //Just return the global that the modules use
        return currentPoint;
 
    },

    name: new ValidationTextBox({}, "name"),
    xid: new ValidationTextBox({}, "xid"),
    //enabled: new CheckBox({}, "enabled"),
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240,
    
    /**
     * Override Toggle Method
     */
    toggle: function(id) {
    	DataPointDwr.toggle(id, function(result) {
            if(result.data.enabled){
                updateImg(
                        $("toggleDataPoint"+ result.data.id),
                        mangoImg("database_go.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
            }else{
                updateImg(
                        $("toggleDataPoint"+ result.data.id),
                        mangoImg("database_stop.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
            }
        });
    },    
    
    
    
    
    
    /**
     * Refresh the Grid
     */
    refresh: function(){
    	this.grid.set('query',{dataSourceId: [dataPointsDataSourceId]});
    },
    
    
    
});

}); // require
