/* Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */


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
	
	
    prefix: 'PointValue',
    varName: 'pointValues',
    viewStore: stores.dataPoint,
    editStore: stores.dataPoint,
    editUpdatesView: true,
    gridId: 'pointValueTable',
    editId: 'editPointValue',
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
    
    open: function(id, options) {
    	//TODO remove delete option on new Vos
        //display("pointDeleteImg", point.id != <c:out value="<%= Common.NEW_ID %>"/>);

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
            _this.setInputs(options.voToLoad);
        	hideContextualMessages(_this.edit);
        	var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
        	myEdit.selectChild('pointDetails');

//            coreFx.combine([baseFx.fadeIn({node: _this.edit}),
//                            coreFx.wipeIn({node: _this.edit})]).play();
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
            	myEdit.selectChild('pointDetails');
                //coreFx.combine([baseFx.fadeIn({node: _this.edit}),
                //                coreFx.wipeIn({node: _this.edit})]).play();
            }, function(message) {
                // wrong id, dwr error
                addErrorDiv(message);
            });
        }
    },
    
    
    download: function() {
        window.location = "/download.shtm?downloadFile=true&dataType=pointValue&pointId=1"; 
        	
    },
    
    
});

}); // require