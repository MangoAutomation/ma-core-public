/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

//@ sourceURL=dataPoint_ajaxLoaded.js
var dataSources;
var dataPointsDataSourceId;

//TO Eventually be merged into Store View
var pointTableFilter = new Array();

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox","dojox/layout/ContentPane",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query","dojo/dom-construct","dijit/form/TextBox",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,ContentPane,
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
    defaultQuery: {dataSourceId: [dataPointsDataSourceId]},
    
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
    						delete pointTableFilter['deviceName'];
    					else
    						pointTableFilter['deviceName'] = new RegExp("^.*"+value+".*$");
    					
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set('query',pointTableFilter);
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
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set("query",pointTableFilter,options);
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
    						delete pointTableFilter['name'];
    					else
    						pointTableFilter['name'] = new RegExp("^.*"+value+".*$");
    					
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					dataPoints.grid.set('query',pointTableFilter);
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
    					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
    					var options = {};
    					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
    					
    					dataPoints.grid.set("query",pointTableFilter,options);
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
						delete pointTableFilter['dataTypeString'];
					else
						pointTableFilter['dataTypeString'] = new RegExp("^.*"+value+".*$");
					
					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
					dataPoints.grid.set('query',pointTableFilter);
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
					pointTableFilter['dataSourceId'] = dataPointsDataSourceId;
					var options = {};
					options.sort = [{attribute: dataPoints.sortMap[i].attribute, descending: dataPoints.sortMap[i].descending}];
					
					dataPoints.grid.set("query",pointTableFilter,options);
					//dataPoints.grid.updateSortArrow(dataPoints.sortMap);
				});
   				domConstruct.place(sortLink,div);

				return div;
    		},
    	 },
    	
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
            
            var action = this.varName + '.' + this.fnMap[button] + '(' + id + ');';
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
        	div.innerHTML = response.data.pointValue;
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
		    //Set the point details div to be visible
		    show("pointDetails");
		    
    	}

    	
    	//For new data points don't bother with the settings
    	if(vo.id != -1){
	    	//Point Properties
    		showPointStatus(vo.enabled); //Show Disable/Enable toggle
	    	setPointProperties(vo);
	    	setLoggingProperties(vo);
	    	setTextRenderer(vo);
	    	setChartRenderer(vo);
	    	setEventDetectors(vo);
	    	show("extraPointSettings");
    	}else{
    		//Disable the point settings inputs
    		hide("extraPointSettings");
    	}
    	//this.enabled.set('value',vo.enabled);
    	//Setup for the point Impl CB
    	currentPoint = vo;
    	if( typeof editPointCBImpl == 'function') editPointCBImpl(vo.pointLocator);
		
    	

    },
    
    getInputs: function() {
    	currentPoint.deviceName = this.deviceName.get('value');
        //Just return the global that the modules use
    	// We actually collect the values in savePoint() in dataSourceProperties.js
        return currentPoint;
 
    },
    
    
    name: new ValidationTextBox({}, "name"),
    xid: new ValidationTextBox({}, "xid"),
    deviceName: new ValidationTextBox({},"deviceName"),
    
    //enabled: new CheckBox({}, "enabled"),
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240,
    
    /**
     * Override Toggle Method
     */
    toggle: function(id) {
    	var _this = this;
    	DataPointDwr.toggle(id, function(result) {
    		_this.updateStatus(result.data.id,result.data.enabled);
    		
            //If the data points view is enabled, update that too
            if(typeof(allDataPoints) != 'undefined')
            	allDataPoints.updateStatus(result.data.id,result.data.enabled);

        });
    },    
    
    /**
     * Used to update the image
     */
    updateStatus: function(id,enabled){
    	var _this = this;
    	var node = $("toggleDataPoint"+ id);
    	//Check to see if the node is visible
    	if(node == null)
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
        //Show on details page only if this one is being viewed
        if(id == _this.currentId)
        	showPointStatus(enabled);
        
    },
    
    /**
     * Refresh the Grid
     */
    refresh: function(){
    	this.grid.set('query',{dataSourceId: [dataPointsDataSourceId]});
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
