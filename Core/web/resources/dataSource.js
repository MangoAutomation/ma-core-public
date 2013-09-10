/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
var dataSources;

var filters = new Array();


require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox","dijit/form/TextBox",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query", "dojox/layout/ContentPane","dojo/dom-construct",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,TextBox,
		domStyle,html,put,when,on,
		baseFx,coreFx,query,ContentPane,domConstruct) {

	//Content Div for loading in DS Pages
dataSourcePropertiesDiv = new ContentPane({
		executeScripts: true,
		parseOnLoad: true,
		onDownloadError: function(error){
			addErrorDiv(error);
		}
}, "editDataSourceDiv");		


dataSources = new StoreView({
	
    prefix: 'DataSource',
    varName: 'dataSources',
    viewStore: stores.dataSource,
    editStore: stores.dataSource,
    editUpdatesView: true,
    gridId: 'dataSourceTable',
    editId: 'editDataSourceDiv',
    defaultSort: [{attribute: "name"}],
    
    filters: new Array(),
	sortMap: [
	          {attribute: "name", descending:true},
	          {attribute: "typeDescriptionString", descending:true},
	          {attribute: "connectionDescriptionString", descending:true},
	         ],
    
    columns: {
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
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsList.name']});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					if(value == '')
    						delete dataSources.filters['name'];
    					else
    						dataSources.filters['name'] = new RegExp("^.*"+value+".*$","i");
    					dataSources.grid.set('query',dataSources.filters);
    				});
    				
    				//Create sort link
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<dataSources.sortMap.length; i++){
    						if(dataSources.sortMap[i].attribute === "name"){
    							dataSources.sortMap[i].descending = !dataSources.sortMap[i].descending;
    							break;
    						}
    					}
    					var options = {};
    					options.sort = [{attribute: dataSources.sortMap[i].attribute, descending: dataSources.sortMap[i].descending}];
    					dataSources.grid.set("query",dataSources.filters,options);
    				});
    				domConstruct.place(sortLink,div);
    				return div;
    		},
    	},
    	typeDescriptionString: {
    		label: mangoMsg['dsList.type'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsList.type']});
				domConstruct.place(label,div);
				input.placeAt(div);
				
				input.watch("value",function(name,oldValue,value){
					if(value == '')
						delete dataSources.filters['typeDescriptionString'];
					else
						dataSources.filters['typeDescriptionString'] = new RegExp("^.*"+value+".*$");
					dataSources.grid.set('query',dataSources.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<dataSources.sortMap.length; i++){
						if(dataSources.sortMap[i].attribute === "typeDescriptionString"){
							dataSources.sortMap[i].descending = !dataSources.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: dataSources.sortMap[i].attribute, descending: dataSources.sortMap[i].descending}];
					dataSources.grid.set("query",dataSources.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
		},
    	},
    	
		connectionDescriptionString: {
			label: mangoMsg['dsList.connection'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['dsList.connection']});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value == '')
						delete dataSources.filters['connectionDescriptionString'];
					else
						dataSources.filters['connectionDescriptionString'] = new RegExp("^.*"+value+".*$","i");
					dataSources.grid.set('query',dataSources.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<dataSources.sortMap.length; i++){
						if(dataSources.sortMap[i].attribute === "connectionDescriptionString"){
							dataSources.sortMap[i].descending = !dataSources.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: dataSources.sortMap[i].attribute, descending: dataSources.sortMap[i].descending}];
					dataSources.grid.set("query",dataSources.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
		},
		}
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
    	this.enabled.set('value',vo.enabled);
    },
    
    getInputs: function() {
        var vo = new DataSourceVO();
        vo.id = this.currentId;
        vo.name = this.name.get('value');
        vo.xid = this.xid.get('value');
        if (this.enabled.get('value')) // sometimes value is returned as "on" rather than true
            vo.enabled = true;
        else
            vo.enabled = false;
        return vo;
 
    },

    name: new ValidationTextBox({}, "dataSourceName"),
    xid: new ValidationTextBox({}, "dataSourceXid"),
    enabled: new CheckBox({}, "dataSourceEnabled"),

    /**
     * Override Toggle Method and return state for use in window
     */
    toggle: function(id,callback) {
    	DataSourceDwr.toggle(id, function(result) {
            if(result.data.enabled){
                updateImg(
                        $("toggleDataSource"+ result.data.id),
                        mangoImg("database_go.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
                if(typeof callback == 'function') callback(true);
            }else{
                updateImg(
                        $("toggleDataSource"+ result.data.id),
                        mangoImg("database_stop.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
                if(typeof callback == 'function') callback(false);
            }
        });

    },    
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240,
    

    /**
     * Refresh the Grid
     */
    refresh: function(){
    	this.grid.set('query', null);
    },
    
    
    
});

//TODO Can most likely remove when done debugging
dataSources.openImpl = dataSources.open;

/**
 * Override Open Method
 */
dataSources.open = function(id,options){
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
    
    if (options.voToLoad) {
    	//Copy
    	when(DataSourceDwr.get(options.voToLoad.id, function(vo) {
            _this.setInputs(options.voToLoad);
            _this.loadView(vo.data.editPagePath,dataSourcePropertiesDiv,options.voToLoad.id)
            //show(_this.edit);
            coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                            coreFx.wipeIn({node: _this.edit})]).play();
        }, function(message) {
            // wrong id, dwr error
            addErrorDiv(message);
        }));
    	
    }
    else {
        // always load from dwr
        // TODO use push from the server side
        // so cache is always up to date
    	if(id > 0){
    		//Need to overload the get Method or add get new to the DwrStore object to use this
	        //when(this.editStore.dwr.get($get("dataSourceTypes")), function(vo) {
    		when(DataSourceDwr.get(id, function(response) {
	            _this.setInputs(response.data.vo);
	            //Due to some issue with content pane need to play fx before loading pane.
	            coreFx.combine([baseFx.fadeIn({node: _this.edit}),
	                            coreFx.wipeIn({node: _this.edit})]).play();
	            _this.loadView(response.data.editPagePath,dataSourcePropertiesDiv,id);
	        }, function(message) {
	            addErrorDiv(message);
	        }));
    	}else{
    		when(DataSourceDwr.getNew($get("dataSourceTypes"), function(response) {
                _this.setInputs(response.data.vo);
	            //Due to some issue with content pane need to play fx before loading pane.
                coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                                coreFx.wipeIn({node: _this.edit})]).play();
                _this.loadView(response.data.editPagePath,dataSourcePropertiesDiv);

            }, function(message) {
                addErrorDiv(message);
            }));
    	}
    }
};


/**
 * Copy Override
 */
dataSources.copy = function(id) {
    var _this = this;
    when(DataSourceDwr.getNew($get("dataSourceTypes"), function(vo) {
        _this.loadView(vo.data.editPagePath,dataSourcePropertiesDiv,id,true)
    }, function(message) {
        // wrong id, dwr error
        addErrorDiv(message);
    }));
},


/**
 * Method to get Data Source Edit Page from Module
 */
dataSources.loadView = function loadDataSourceView(editPagePath,targetContentPane,id,copy){
	//Create the base URL
	var xhrUrl = "/data_source_properties.shtm?"
		
	//Do we have an ID To pass in
	if(typeof id != 'undefined')
		xhrUrl = xhrUrl + "dsid=" + id;
	else{
		//No id so it must be a new one, get the type of new one
		var dsType = $get("dataSourceTypes");
		xhrUrl+= "typeId=" + dsType;
	}
		
	if(typeof copy != 'undefined')
		xhrUrl = xhrUrl + "&copy=true";
	
	var deferred = targetContentPane.set('href',xhrUrl); //Content Pane get
	targetContentPane.set('class','borderDiv marB marR');
	deferred.then(function(value){
		//When Done, Could put callback here
	},function(err){
		addErrorDiv(err);
	},function(update){
		//Progress Info
	});
}

//Temp callback to editDataSourceDiv to replicate dojo.ready, 
// to be replaced with scriptHasHooks concept from dojox/dijit content pane
dojo.connect(dataSourcePropertiesDiv, "onDownloadEnd", function(){
	   init();
	   dataSourcePropertiesDiv.startup();
	   dataSourcePropertiesDiv.resize();
	});

}); // require
