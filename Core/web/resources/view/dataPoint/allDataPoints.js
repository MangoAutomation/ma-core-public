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
	
	
    prefix: 'AllDataPoints',
    varName: 'allDataPoints',
    viewStore: stores.allDataPoints,
    editStore: stores.allDataPoints,
    editUpdatesView: false, //Is this really what we want?  The editstore and viewstore are the same for data points.
    gridId: 'allDataPointsTable',
    editId: 'pointDetails',
    defaultSort: [{attribute: "deviceName"},{attribute: "name"}],
    filter: new Array(),
    minRowsPerPage: 100,
    maxRowsPerPage: 100,
    sortMap: [
              {attribute: "dataSourceTypeName", descending:true},
              {attribute: "deviceName", descending:true},
              {attribute: "name", descending:true},
              {attribute: "xid", descending:true},              
              {attribute: "dataTypeString", descending: true},
              {attribute: "loggingTypeString", descending: true},
              {attribute: "loggingIntervalString", descending: true},
              {attribute: "templateName", descending: true},
              
              ],
    
    columns: {
    	dataSourceTypeName:{
    		label: "Type",
    		sortable: false,
    		renderHeaderCell: function(th){
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 8em",
    					intermediateChanges: true,
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsEdit.dataSourceType'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete allDataPoints.filter['dataSourceTypeName'];
    					else
    						allDataPoints.filter['dataSourceTypeName'] = new RegExp("^.*"+value+".*$");
    					
    					allDataPoints.grid.set('query',allDataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<allDataPoints.sortMap.length; i++){
    						if(allDataPoints.sortMap[i].attribute === "dataSourceTypeName"){
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
        
        xid: {
    		label: mangoMsg['common.xid'],
    		sortable: false,
    		renderHeaderCell: function(th){
    			
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 10em",
    					intermediateChanges: true,
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['common.xid'],});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					
    					if(value == '')
    						delete allDataPoints.filter['xid'];
    					else
    						allDataPoints.filter['xid'] = new RegExp("^.*"+value+".*$");
    					
    					allDataPoints.grid.set('query',allDataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<allDataPoints.sortMap.length; i++){
    						if(allDataPoints.sortMap[i].attribute === "xid"){
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
 
    	 
     	loggingTypeString: {
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['pointEdit.logging.type'],});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					
					if(value == '')
						delete allDataPoints.filter['loggingTypeString'];
					else
						allDataPoints.filter['loggingTypeString'] = new RegExp("^.*"+value+".*$");
					
					allDataPoints.grid.set('query',allDataPoints.filter);
				});
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<allDataPoints.sortMap.length; i++){
						if(allDataPoints.sortMap[i].attribute === "loggingTypeString"){
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
    	 
      	loggingIntervalString: {
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['pointEdit.logging.period'],});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					
					if(value == '')
						delete allDataPoints.filter['loggingIntervalString'];
					else
						allDataPoints.filter['loggingIntervalString'] = new RegExp("^.*"+value+".*$");
					
					allDataPoints.grid.set('query',allDataPoints.filter);
				});
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<allDataPoints.sortMap.length; i++){
						if(allDataPoints.sortMap[i].attribute === "loggingIntervalString"){
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
    	 
      	templateName: {
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['pointEdit.template.templateName'],});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					
					if(value == '')
						delete allDataPoints.filter['templateName'];
					else
						allDataPoints.filter['templateName'] = new RegExp("^.*"+value+".*$");
					
					allDataPoints.grid.set('query',allDataPoints.filter);
				});
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<allDataPoints.sortMap.length; i++){
						if(allDataPoints.sortMap[i].attribute === "templateName"){
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
    
    imgMap: {'delete': 'delete', edit: 'pencil', 'export': 'emport', copy: 'add', toggleOn: 'database_go', toggleOff: 'database_stop', run: 'control_play_blue', pointDetails: 'icon_comp'},
    fnMap: {'delete': 'remove', edit: 'open', 'export': 'showExport', copy: 'copy', toggleOn: 'toggle', toggleOff: 'toggle', run: 'run', pointDetails: 'view'},

    buttons: ['toggle','edit','delete','copy','export','pointDetails'],
    
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
            
            var action = this.varName + '.' + this.fnMap[button] + '(' + id + ', this.src.indexOf("_go") == -1);';
            if(button === 'pointDetails'){
            	var over = this.varName + ".showPointValue(event," + id + ")";
            	var out = this.varName + ".hidePointValue(" + id + ")";
            	var divId = "pointValue" + this.prefix + id;
            	
            	var img = put(span, 'img.ptr#$[src=$][title=$][onclick=$][onmouseover=$][onmouseout=$]', elementId, src, title, action,over,out);
            	var div = put(span, 'div#$[style=$]',divId,"display:none;top:10px;left:1px;z-index:1000");
            }
            else
            	var img = put(span, 'img.ptr#$[src=$][title=$][onclick=$]', elementId, src, title, action);
        }
        return span;
    },
    
    
    showPointValue: function(event,id){
    	var divId = "pointValue" + this.prefix + id;
    	var div = $(divId);

    	DataPointDwr.getMostRecentValue(id,function(response){
        	div.innerHTML = response.data.pointValue;
        	show(divId);
    	});

    },
    hidePointValue: function(id){
    	hide("pointValue" + this.prefix + id);
    },
    /**
     * Redirect the user to the point details view
     */
    view: function(id){
        window.location = "/data_point_details.shtm?dpid=" +id; 
    },    
    
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
    toggle: function(id, enabled) {
    	var _this = this;
        
    	//Are we also editing this point?
    	var dpInEditView = dojo.byId("toggleDataPoint");
        if(dpInEditView !== null){
        	startImageFader("toggleDataPoint", true);
        }    	
        //Is this point in the data source list
        var dpInView = dojo.byId("toggleDataPoint" + id);
        if(dpInView !== null)
        	startImageFader("toggleDataPoint" + id, true);
        
    	startImageFader("toggleAllDataPoints" + id, true);
    	
    	DataPointDwr.toggle(id, enabled, function(result) {
    		_this.updateStatus(result.data.id,result.data.enabled);
    		stopImageFader("toggleAllDataPoints" + id);
    		
    		dpInView = dojo.byId("toggleDataPoint" + id);
    		if(dpInView !== null)
    			stopImageFader("toggleDataPoint" + id);
    		
    		dpInEditView = dojo.byId("toggleDataPoint");
    		if(dpInEditView !== null)
    			stopImageFader("toggleDataPoint");
            //If the data points view is enabled, update that too
            if(typeof(dataPoints) != 'undefined')
            	dataPoints.updateStatus(result.data.id,result.data.enabled);
        });
    },    
    
    /**
     * Used to update the image
     */
    updateStatus: function(id,enabled){
    	//Check to see if the point is loaded into the table yet? (Lazy loading may keep some points out of view)
    	var node = $("toggleAllDataPoints" + id);
    	if(node){
	        if(enabled){
	            updateImg(
	                    node,
	                    mangoImg("database_go.png"),
	                    mango.i18n["common.enabledToggle"],
	                    true
	            );
	        }else{
	            updateImg(
	                    node,
	                    mangoImg("database_stop.png"),
	                    mango.i18n["common.enabledToggle"],
	                    true
	            );
	        }
    	}

    },
    
    
    /**
     * Refresh the Grid
     */
    refresh: function(){
        
        //Not bothering with the sort options:
    	this.grid.set("query",allDataPoints.filter,null);
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
            	
            	//The open the data point with DwrCall to set the edit point in the User (Need to fix this)
            	dataPoints.open(options.voToLoad.id,options);
                //_this.setInputs(options.voToLoad);
            	hideContextualMessages(_this.edit);
            	
            	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
            	myEdit.selectChild('dataPointDetails-tab');            	
            });
            

        }
        else {

        	dwr.engine.beginBatch(); //Force batching of the next calls
            when(this.editStore.dwr.get(id), function(vo) {
            	dataSources.open(vo.dataSourceId,{},function(){
                // Load the data point on the dataPoints view
                //
                dataPoints.open(vo.id); //Very inefficient but we need to reset the User.editDataPoint
                //Hide contextual messages
            	hideContextualMessages(_this.edit);
            	            	
            	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
            	myEdit.selectChild('dataPointDetails-tab');
            	});
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            });
            dwr.engine.endBatch(); //Stop batching
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
    	
    	
    },
    
    /**
     * Export data points using the filter
     */
    showExportUsingFilter: function() {
        
        var query = this.filter;
        var options = {}; //We don't need any
        
        var sortArray, op;
        if (typeof options.sort === 'string') {
            op = new SortOption();
            op.attribute = options.sort;
            op.desc = false;
            sortArray = [op];
        }
        else if (options.sort && typeof options.sort.length === 'number') {
            sortArray = [];
         
            for (var i = 0; i < options.sort.length; i++) {
                op = new SortOption();
                op.attribute = options.sort[i].attribute;
                op.desc = options.sort[i].descending || false;
                sortArray.push(op);
            }
        }
        
        var filterMap = {};
        for (var prop in query) {
            var conditions = query[prop];
            // allow specific regex queries
            if (conditions instanceof RegExp) {
                // anything
                //if (conditions.source === '^.*$')
                //    continue;
                filterMap[prop] = 'RegExp:' + conditions.source;
                //break;
            }else{
                if (conditions instanceof ArrayTester) {
                    conditions = conditions.data;
                }
                if (typeof conditions === 'string' || typeof conditions === 'number')
                    conditions = [conditions];
                filterMap[prop] = conditions.join();
            }
        }
        
        var start = (isFinite(options.start)) ? options.start : null;
        var count = (isFinite(options.count)) ? options.count : null;
        
        // controls whether sql is done using or or and
        //var or = true;
        
        
        DataPointDwr.jsonExportUsingFilter(filterMap, sortArray, start, count, this.viewStore.dwr.or, function(json){
            $set("exportData", json);
            exportDialog.show();
        });

    },
    
    
});


}); // require
