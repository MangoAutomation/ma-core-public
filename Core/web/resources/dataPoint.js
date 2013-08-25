/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
var dataSources;
var dataPointsDataSourceId;

require(["deltamation/StoreView", "dijit/form/CheckBox", "dijit/form/ValidationTextBox",
         "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query",
         "dojo/domReady!"],
function(StoreView, CheckBox, ValidationTextBox,
		domStyle,html,put,when,on,
		baseFx,coreFx,query) {


	
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
    	deviceName: mangoMsg['dsEdit.deviceName'],
    	name: mangoMsg['dsList.name'],
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

//    save: function() {
//        var _this = this;
//        var vo = this.getInputs();
//        //Wierd error messages if these are here?
//        delete vo.pointLocator.configurationDescription;
//        delete vo.pointLocator.dataTypeMessage;
//        savePointImpl(vo.pointLocator);
//        vo = this.getInputs(); //Get the filled out stuff
//        
//        when(this.editStore.cache.put(vo, {overwrite: true}), function(vo) {
//            // ok
//            _this.currentId = vo.id;
//            if (_this.closeEditOnSave)
//                _this.close();
//            
//            // get new row from view store
//            // TODO make this a push from the server side
//            if (_this.editUpdatesView) {
//                when(_this.viewStore.dwr.get(vo.id), function(viewVo) {
//                    _this.viewStore.cache.put(viewVo, {overwrite: true});
//                });
//            }
//        }, function(response) {
//            if (response.dwrError || typeof response.messages === 'undefined') {
//                // timeout, dwr error etc
//                addErrorDiv(response);
//                return;
//            }
//            // validation error
//            for (var i = 0 ; i < response.messages.length; i++) {
//                var m = response.messages[i];
//                var x = _this[m.contextKey] || _this[m.contextKey + 'Picker'];
//                if (x) {
//                    x.focus();
//                    x.displayMessage(m.contextualMessage);
//                    break;
//                }
//                else {
//                    addMessage(m);
//                }
//            }
//        });
//    },
    
    /**
     * Refresh the Grid
     */
    refresh: function(){
    	this.grid.set('query',{dataSourceId: [dataPointsDataSourceId]});
    },
    
    
    
});

}); // require
