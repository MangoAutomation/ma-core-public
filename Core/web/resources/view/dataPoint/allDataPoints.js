/*
 * This Table works in conjunction with the dataPoint.js view
 * for editing.  That view restricts points to the editing data source
 * this view shows all points.
 * 
 * 
 * 
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox","dojox/layout/ContentPane","dojo/Deferred",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query","dojo/dom-construct","dijit/form/TextBox",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,ContentPane,Deferred,
		domStyle,html,put,when,on,
		baseFx,coreFx,query,domConstruct,TextBox) {


	
allDataPoints = new StoreView({
	
	
    prefix: 'DataPoint',
    varName: 'allDataPoints',
    viewStore: stores.allDataPoints,
    editStore: stores.allDataPoints,
    editUpdatesView: true,
    gridId: 'allDataPointsTable',
    editId: 'pointDetails',
    defaultSort: [{attribute: "deviceName"},{attribute: "name"}],
    filter: new Array(),

    sortMap: [
              {attribute: "deviceName", descending:true},
              {attribute: "name", descending:true},
              {attribute: "dataTypeString", descending: true},
              ],
    
    columns: {
    	
        deviceName: {
    		label: mangoMsg['dsEdit.deviceName'],
    		sortable: false,
    		renderHeaderCell: function(th){
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 8em",
    					intermediateChanges: true,
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsEdit.deviceName'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete allDataPoints.filter['deviceName'];
    					else
    						allDataPoints.filter['deviceName'] = new RegExp("^.*"+value+".*$");
    					
    					allDataPoints.grid.set('query',allDataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<allDataPoints.sortMap.length; i++){
    						if(allDataPoints.sortMap[i].attribute === "deviceName"){
    							allDataPoints.sortMap[i].descending = !allDataPoints.sortMap[i].descending;
    							break;
    						}
    					}
    					var options = {};
    					options.sort = [{attribute: allDataPoints.sortMap[i].attribute, descending: allDataPoints.sortMap[i].descending}];
    					allDataPoints.grid.set("query",allDataPoints.filter,options);
    				});
    				domConstruct.place(sortLink,div);
    				return div;
    		},
        },

        name: {
    		label: mangoMsg['dsList.name'],
    		sortable: false,
    		renderHeaderCell: function(th){
    			
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 10em",
    					intermediateChanges: true,
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['dsList.name'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete allDataPoints.filter['name'];
    					else
    						allDataPoints.filter['name'] = new RegExp("^.*"+value+".*$");
    					
    					allDataPoints.grid.set('query',allDataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<allDataPoints.sortMap.length; i++){
    						if(allDataPoints.sortMap[i].attribute === "name"){
    							allDataPoints.sortMap[i].descending = !allDataPoints.sortMap[i].descending;
    							break;
    						}
    					}
    					var options = {};
    					options.sort = [{attribute: allDataPoints.sortMap[i].attribute, descending: allDataPoints.sortMap[i].descending}];
    					
    					allDataPoints.grid.set("query",allDataPoints.filter,options);
    				});
       				domConstruct.place(sortLink,div);

    				return div;
    		},
        },
    	dataTypeString: {
    		label: mangoMsg['dsEdit.pointDataType'],
    		sortable: false,
    		renderHeaderCell: function(th){
    			
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['dsEdit.pointDataType'],});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					
					if(value == '')
						delete allDataPoints.filter['dataTypeString'];
					else
						allDataPoints.filter['dataTypeString'] = new RegExp("^.*"+value+".*$");
					
					allDataPoints.grid.set('query',allDataPoints.filter);
				});
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<allDataPoints.sortMap.length; i++){
						if(allDataPoints.sortMap[i].attribute === "dataTypeString"){
							allDataPoints.sortMap[i].descending = !allDataPoints.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: allDataPoints.sortMap[i].attribute, descending: allDataPoints.sortMap[i].descending}];
					
					allDataPoints.grid.set("query",allDataPoints.filter,options);
				});
   				domConstruct.place(sortLink,div);

				return div;
    		},
    	 },
    	
    },
    
    buttons: ['toggle','edit','delete','copy','export'],
    
    preInit: function() {
    },
    
    postGridInit: function() {
    },
    
    setInputs: function(vo) {

    	dataPoints.setInputs(vo);

    },
    
    getInputs: function() {

       dataPoints.getInputs();
 
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
    	this.grid.set('query',{});
    },
    
    open: function(id, options) {

        this.currentId = id;
        var _this = this;
        options = options || {};
        var posX = options.posX;
        var posY = options.posY;
        
        if (options.voToLoad) {
        	
            //First open the data sources tabs
            dataSources.open(options.voToLoad.dataSourceId,{},function(value){
                _this.setInputs(options.voToLoad);
            	hideContextualMessages(_this.edit);

            	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
            	myEdit.selectChild('dataPointDetails-tab');            	
            });
            

        }
        else {

        	dwr.engine.beginBatch();
            when(this.editStore.dwr.get(id), function(vo) {
            	dataSources.open(vo.dataSourceId,{},function(){
                // ok
                _this.setInputs(vo);
                //Hide contextual messages
            	hideContextualMessages(_this.edit);
            	            	
            	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
            	myEdit.selectChild('dataPointDetails-tab');
            	});
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            });
            dwr.engine.endBatch();
        }
    },
    
    
    download: function() {
    },
    
    
    loadSettings: function(id,targetContentPane){
    	//Create the base URL
    	var xhrUrl = "/data_point_settings.shtm?"
    		
    	//Do we have an ID To pass in
    	if(typeof id != 'undefined')
    		xhrUrl = xhrUrl + "dpid=" + id; //could also use pedid
    	else{
    		//For now don't allow new points settings as old page didn't
    	}
    		
    	var deferred = targetContentPane.set('href',xhrUrl); //Content Pane get
 
    	deferred.then(function(value){
    		//When Done, Could put callback here
    	},function(err){
    		addErrorDiv(err);
    	},function(update){
    		//Progress Info
    	});
    	
    	
    }
    
    
});


}); // require
