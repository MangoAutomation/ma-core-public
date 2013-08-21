/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
var dataSources;

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query", "dojox/layout/ContentPane",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,
		domStyle,html,put,when,on,
		baseFx,coreFx,query,ContentPane) {

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
    
    columns: {
    	name: mangoMsg['dsList.name'],
    	typeDescriptionString: mangoMsg['dsList.type'],
		connectionDescriptionString: mangoMsg['dsList.connection'],
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
     * Override Toggle Method
     */
    toggle: function(id) {
    	DataSourceDwr.toggle(id, function(result) {
            if(result.data.enabled){
                updateImg(
                        $("toggleDataSource"+ result.data.id),
                        mangoImg("database_go.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
            }else{
                updateImg(
                        $("toggleDataSource"+ result.data.id),
                        mangoImg("database_stop.png"),
                        mango.i18n["common.enabledToggle"],
                        true
                );
            }
        });
    },    
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240
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
    		when(DataSourceDwr.get(id, function(vo) {
	            _this.setInputs(vo); //TODO Not using this here
	            _this.loadView(vo.data.editPagePath,dataSourcePropertiesDiv,id)
	            //show(_this.edit);
	            coreFx.combine([baseFx.fadeIn({node: _this.edit}),
	                            coreFx.wipeIn({node: _this.edit})]).play();
	        }, function(message) {
	            // wrong id, dwr error
	            addErrorDiv(message);
	        }));
    	}else{
    		when(DataSourceDwr.getNew($get("dataSourceTypes"), function(vo) {
                _this.setInputs(vo); //TODO not using this here
                _this.loadView(vo.data.editPagePath,dataSourcePropertiesDiv);
                //show(_this.edit);
                coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                                coreFx.wipeIn({node: _this.edit})]).play();
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            }));
    	}
    }
};


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
	var dsType = $get("dataSourceTypes");
	var xhrUrl = "/data_source_properties.shtm?typeId=" + dsType;
	if(typeof id != 'undefined')
		xhrUrl = xhrUrl + "&dsid=" + id;
	if(typeof copy != 'undefined')
		xhrUrl = xhrUrl + "&copy=true";
	
	var deferred = targetContentPane.set('href',xhrUrl); //Content Pane get
	targetContentPane.set('class','borderDiv marB marR');
}

//Temp callback to editDataSourceDiv to replicate dojo.ready, 
// to be replaced with scriptHasHooks concept from dojox/dijit content pane
dojo.connect(dataSourcePropertiesDiv, "onDownloadEnd", function(){
	   initProperties();
	});

}); // require
