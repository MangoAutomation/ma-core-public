/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

var dataSources;
var dataPointsDataSourceId;

//TO Eventually be merged into Store View
//var pointTableFilter = new Array();

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox","dojox/layout/ContentPane",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query","dojo/dom-construct","dijit/form/TextBox",
         "deltamation/ArrayTester", "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,ContentPane,
		domStyle,html,put,when,on,
		baseFx,coreFx,query,domConstruct,TextBox, ArrayTester) {


	
dataPoints = new StoreView({
	
	
    prefix: 'DataPoint',
    varName: 'dataPoints',
    viewStore: stores.dataPoint,
    editStore: stores.dataPoint,
    editUpdatesView: false, //Is this really what we want?  The editstore and viewstore are the same for data points.
    gridId: 'dataPointTable',
    editId: 'pointDetails',
    defaultSort: [{attribute: "deviceName"},{attribute: "name"}],
    defaultQuery: {dataSourceId: [dataPointsDataSourceId]},
    closeEditOnSave: false,  /* We are managing this ourselves */
    filter: [],
    minRowsPerPage: 100,
    maxRowsPerPage: 100,
    sortMap: [
              {attribute: "deviceName", descending:true},
              {attribute: "name", descending:true},
              {attribute: "dataTypeString", descending: true},
              {attribute: "setPermission", descending: true},
              {attribute: "readPermission", descending: true}
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
    					
    					if(value === '')
    						delete dataPoints.filter.deviceName;
    					else
    						dataPoints.filter.deviceName = new RegExp("^.*"+value+".*$");
    					
    					dataPoints.filter.dataSourceId = dataPointsDataSourceId;
    					dataPoints.grid.set('query',dataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<dataPoints.sortMap.length; i++){
    						if(dataPoints.sortMap[i].attribute === "deviceName"){
    							dataPoints.sortMap[i].descending = !dataPoints.sortMap[i].descending;
    							break;
    						}
    					}
    					var options = {};
    					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
    					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set("query",dataPoints.filter,options);
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
    						delete dataPoints.filter['name'];
    					else
    						dataPoints.filter['name'] = new RegExp("^.*"+value+".*$");
    					
    					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set('query',dataPoints.filter);
    				});
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<dataPoints.sortMap.length; i++){
    						if(dataPoints.sortMap[i].attribute === "name"){
    							dataPoints.sortMap[i].descending = !dataPoints.sortMap[i].descending;
    							break;
    						}
    					}
    					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
    					var options = {};
    					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
    					
    					dataPoints.grid.set("query",dataPoints.filter,options);
    					//dataPoints.grid.updateSortArrow(dataPoints.sortMap);
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
						delete dataPoints.filter['dataTypeString'];
					else
						dataPoints.filter['dataTypeString'] = new RegExp("^.*"+value+".*$");
					
					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
					dataPoints.grid.set('query',dataPoints.filter);
				});
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<dataPoints.sortMap.length; i++){
						if(dataPoints.sortMap[i].attribute === "dataTypeString"){
							dataPoints.sortMap[i].descending = !dataPoints.sortMap[i].descending;
							break;
						}
					}
					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
					var options = {};
					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
					
					dataPoints.grid.set("query",dataPoints.filter,options);
					//dataPoints.grid.updateSortArrow(dataPoints.sortMap);
				});
   				domConstruct.place(sortLink,div);

				return div;
    		},
    	 },
    	 setPermission: {
     		label: mangoMsg['filter.bySetPermissions'],
      		sortable: false,
      		renderHeaderCell: function(th){
      			
  				var div = domConstruct.create("div");
  				var input = new TextBox({
  					name: 'inputText',
  					placeHolder: 'filter text',
  					style: "width: 10em",
  					intermediateChanges: true,
  				});
  				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['filter.bySetPermissions'],});
  				domConstruct.place(label,div);
  				input.placeAt(div);
  				input.watch("value",function(name,oldValue,value){
  					
  					if(value == '')
  						delete dataPoints.filter['setPermission'];
  					else
  						dataPoints.filter['setPermission'] = new RegExp("^.*"+value+".*$");
  					
  					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
  					dataPoints.grid.set('query',dataPoints.filter);
  				});
  				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
  				on(sortLink,'click',function(event){
  					
  					//Flip through the list to see if we already have an order?
  					for(var i =0; i<dataPoints.sortMap.length; i++){
  						if(dataPoints.sortMap[i].attribute === "setPermission"){
  							dataPoints.sortMap[i].descending = !dataPoints.sortMap[i].descending;
  							break;
  						}
  					}
  					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
  					var options = {};
  					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
  					
  					dataPoints.grid.set("query",dataPoints.filter,options);
  				});
     				domConstruct.place(sortLink,div);

  				return div;
      		},
     	 },
    	 readPermission: {
    		label: mangoMsg['filter.byReadPermissions'],
      		sortable: false,
      		renderHeaderCell: function(th){
      			
  				var div = domConstruct.create("div");
  				var input = new TextBox({
  					name: 'inputText',
  					placeHolder: 'filter text',
  					style: "width: 10em",
  					intermediateChanges: true,
  				});
  				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML: mangoMsg['filter.byReadPermissions'],});
  				domConstruct.place(label,div);
  				input.placeAt(div);
  				input.watch("value",function(name,oldValue,value){
  					
  					if(value == '')
  						delete dataPoints.filter['readPermission'];
  					else
  						dataPoints.filter['readPermission'] = new RegExp("^.*"+value+".*$");
  					
  					dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
  					dataPoints.grid.set('query',dataPoints.filter);
  				});
  				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
  				on(sortLink,'click',function(event){
  					
  					//Flip through the list to see if we already have an order?
  					for(var i =0; i<dataPoints.sortMap.length; i++){
  						if(dataPoints.sortMap[i].attribute === "readPermission"){
  							dataPoints.sortMap[i].descending = !dataPoints.sortMap[i].descending;
  							break;
  						}
  					}
  					dataPoints.filter.dataSourceId = dataPointsDataSourceId;
  					var options = {};
  					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
  					
  					dataPoints.grid.set("query",dataPoints.filter,options);
  				});
     				domConstruct.place(sortLink,div);

  				return div;
      		},
    	 },
    },
    
    //Add any columns from the data source
    addColumns: function(){
    	var _this = this;
    	//Globals for legacy column support
    	var pointListColumnHeaders = new Array();
    	var pointListColumnFunctions = new Array();
    	
    	
    	if(typeof appendPointListColumnFunctions === 'function'){
    		appendPointListColumnFunctions(pointListColumnHeaders, pointListColumnFunctions);
    	
    		for(var i=0; i<pointListColumnHeaders.length; i++){
    			//_this.columns[pointListColumnHeaders[i]] = pointListColumnFunctions[i];
    			var index = "dataPoints"+i;
    			var header = pointListColumnHeaders[i];
    			var content = pointListColumnFunctions[i];
    			_this.columns[index] = _this.createColumn(header,content)();
    				
    		}
    	}
    },
    
    createColumn: function(header,content){
    	return function(){
    		return {
    			sortable: false,
				renderHeaderCell: function(th){
	                var div = dojo.create("div");
	                div.innerHTML = header;
	                return div;
	            },
	            renderCell: function(point, alarmLevel, cell){
	                var div = document.createElement("div");
	                div.innerHTML = content(point);
	                cell.appendChild(div);
	                return div;
	            }
    		};
    	}
    },
    
    imgMap: {'delete': 'delete', edit: 'pencil', 'export': 'emport', copy: 'add', toggleOn: 'database_go', toggleOff: 'database_stop', run: 'control_play_blue', pointDetails: 'icon_comp'},
    fnMap: {'delete': 'remove', edit: 'open', 'export': 'showExport', copy: 'copy', toggleOn: 'toggle', toggleOff: 'toggle', run: 'run', pointDetails: 'view'},

    buttons: ['toggle','edit','delete','copy','export','pointDetails'],
    /**
     * Override to show point value on mouseover
     */
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
            
            if(this.fnMap[button] !== 'toggle')
            	var action = this.varName + '.' + this.fnMap[button] + '(' + id + ');';
            else
            	var action = this.varName + '.toggle(' + id + ', this.src.indexOf("_go") == -1);';
            if(button === 'pointDetails'){
            	var over = this.varName + ".showPointValue(" + id + ")";
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
    
    
    showPointValue: function(id){
    	var divId = "pointValue" + this.prefix + id;
    	var div = $(divId);
    	DataPointDwr.getMostRecentValue(id,function(response){
        	div.innerHTML = response.data.pointValue.replace("<", "&lt;");
        	show(divId);
    	});

    },
    hidePointValue: function(id){
    	hide("pointValue" + this.prefix + id);
    },
    
    
    preInit: function() {
    },
    
    postGridInit: function() {
    },
    
    setInputs: function(vo) {

    	this.currentId = vo.id;
    	this.name.set('value',vo.name);
    	this.xid.set('value',vo.xid);
    	this.deviceName.set('value',vo.deviceName);
////    	//Signal a new data type?
//    	if(this.currentDataTypeId != vo.pointLocator.dataTypeId)
//    		dataPointDataTypeChanged(vo.pointLocator.dataTypeId);
    	
    	this.currentDataTypeId = vo.pointLocator.dataTypeId;
        $set("readPermission", vo.readPermission);
        $set("setPermission", vo.setPermission);
    	
    	dataPointsDataSourceId = vo.dataSourceId;
    	
    	//The first open call will create a tab for the point details
    	var cp1 = dojo.byId("dataPointDetails-tab");
    	if(cp1 == null){
    		var tc = dijit.byId("dataSourcePropertiesTabContainer");
    		
		    //Setup the Point Details Tab
		    var cp1 = new ContentPane({
		         title: mangoTranslate('dsEdit.points.details'),
		         content: "<div id='pointDetails-content'></div>",
		         id: 'dataPointDetails-tab',
		    });
		    tc.addChild(cp1);
		    var pd = dojo.byId("pointDetails");
		    domConstruct.place(pd,"pointDetails-content");
    	}

    	//Setup for the point Impl CB and make the call prior to setting the inputs
    	currentPoint = vo;
    	if( typeof editPointCBImpl == 'function') editPointCBImpl(vo.pointLocator, vo);
    	
	    //Set the point details div to be visible
	    show("pointDetails");
    	
    	//For new data points don't bother with the settings
    	if(vo.id != -1){
	    	//Point Properties
    		showPointStatus(vo.enabled); //Show Disable/Enable toggle
	    	setPointProperties(vo);
	    	setLoggingProperties(vo);
	    	setTextRenderer(vo);
	    	setChartRenderer(vo);
	    	setEventDetectors(vo);
	    	setDataPointTemplate(vo); //Load last to disable inputs if necessary
	    	show("extraPointSettings");
    	}else{
    		//Disable the point settings inputs
    		hide("extraPointSettings");
    	}

    	//Set the enabled value
    	this.updateStatus(vo.id,vo.enabled);


    },
    
    getInputs: function() {
    	currentPoint.deviceName = this.deviceName.get('value');
    	currentPoint.enabled = this.enabled;
 	    currentPoint.readPermission = $get("readPermission");
	    currentPoint.setPermission = $get("setPermission");
        //Just return the global that the modules use
    	// We actually collect the values in savePoint() in dataSourceProperties.js
        return currentPoint;
 
    },
    
    
    name: new ValidationTextBox({}, "name"),
    xid: new ValidationTextBox({}, "xid"),
    deviceName: new ValidationTextBox({},"deviceName"),
    enabled: false, //Internal value, only toggle-able
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240,
    
    /**
     * Override Toggle Method
     */
    toggle: function(id, enabled) {
    	//Don't allow toggle of new ids
    	if(id<0)
    		return;
    	
    	//Are we also editing this point?
    	var dpInEditView = dojo.byId("toggleDataPoint");
        if(dpInEditView !== null){
        	startImageFader("toggleDataPoint", true);
        }    	
        //Is this point in the data source list view 
        var dpInEditListView = dojo.byId("toggleDataPoint" + id);
        if(dpInEditListView !== null)
        	startImageFader("toggleDataPoint" + id, true);
        
        var dpInAllListView = dojo.byId("toggleAllDataPoints" + id);
        if(dpInAllListView !== null)
        	startImageFader("toggleAllDataPoints" + id, true);
    	
    	var _this = this;
    	DataPointDwr.enableDisable(id, enabled, function(result) {
    		//TODO there is a known bug here that will change the color of the editing points light even 
    		// if the data point that was toggled is no longer in the edit view.  
    		_this.updateStatus(result.data.id,result.data.enabled);
            //If the data points view is enabled, update that too
            if(typeof(allDataPoints) != 'undefined')
            	allDataPoints.updateStatus(result.data.id,result.data.enabled);
            
            dpInAllListView = dojo.byId("toggleAllDataPoints" + id);
            if(dpInAllListView !== null)
            	stopImageFader("toggleAllDataPoints" + id);
            
    		dpInEditListView = dojo.byId("toggleDataPoint" + id);
            if(dpInEditListView !== null)
            	stopImageFader("toggleDataPoint" + id);
    		
    		dpInEditView = dojo.byId("toggleDataPoint");
            if(dpInEditView !== null)
    			stopImageFader("toggleDataPoint");


        });
    },
    
    /**
     * Used to update the image
     */
    updateStatus: function(id,enabled){
    	var _this = this;
        
    	//Show on details page only if this one is being viewed
        if(id == _this.currentId){
        	showPointStatus(enabled);
        	_this.enabled = enabled;
        }
        
    	var node = $("toggleDataPoint"+ id);
    	//Check to see if the node is visible
    	if(node === null)
    		return;
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

        
    },
    
    /**
     * Refresh the Grid
     */
    refresh: function(){
        dataPoints.filter['dataSourceId'] = dataPointsDataSourceId;
        this.grid.set("query",dataPoints.filter,null);
    	//this.grid.set('query',{dataSourceId: [dataPointsDataSourceId]});
    },
    
    open: function(id, options) {
    	//TODO remove delete option on new Vos
        //display("pointDeleteImg", point.id != <c:out value="<%= Common.NEW_ID %>"/>);

        this.currentId = id;
        var _this = this;
        options = options || {};
        
        if (options.voToLoad) {
            _this.setInputs(options.voToLoad);
        	hideContextualMessages(_this.edit);

        	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
        	myEdit.selectChild('dataPointDetails-tab');
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
            	            	
            	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
            	myEdit.selectChild('dataPointDetails-tab');
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            });
        }
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
    
    
    /**
     * Redirect the user to the point details view
     */
    view: function(id){
        window.location = "/data_point_details.shtm?dpid=" +id; 
    },
    
    download: function() {
        window.location = "/download.shtm?downloadFile=true&dataType=dataPoint&dsId=" +dataPointsDataSourceId; 
        	
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
