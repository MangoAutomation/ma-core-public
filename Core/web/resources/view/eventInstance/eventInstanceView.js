/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

var eventInstances;

require(["deltamation/StoreView", "dijit/form/DateTextBox", "dijit/form/Select", "dijit/form/ValidationTextBox","dijit/form/TextBox",
         "dojo/date", "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query", "dojox/layout/ContentPane","dojo/dom-construct",
         "dojo/domReady!"],
function(StoreView, DateTextBox, Select, ValidationTextBox,TextBox,
		date, domStyle,html,put,when,on,
		baseFx,coreFx,query,ContentPane,domConstruct) {

eventInstances = new StoreView({
	
    prefix: 'EventInstance',
    varName: 'eventInstances',
    viewStore: stores.eventInstances,
    editStore: stores.eventInstances,
    editUpdatesView: true,
    gridId: 'eventInstanceTable',
    editId: 'editEventInstanceDiv',
    defaultSort: [{attribute: "activeTimestamp", descending: true}],
    defaultQuery: {alarmLevel: [1,2,3,4]},
    
    filters: {
    			alarmLevel: [1,2,3,4]
    		 },
             
    
    
	sortMap: [
	          {attribute: "id", descending:true},
	          {attribute: "alarmLevel", descending:true},
	          {attribute: "activeTimestampString", descending:true},
	          {attribute: "messageString", descending:true},
	          {attribute: "rtnTimestampString", descending: true},
	          {attribute: "totalTimeString", descending: true},
	         ],
    
    columns: {
    	id: {
    		label: mangoMsg['events.id'],
    		sortable: false,
    		renderHeaderCell: function(th){
    				var div = domConstruct.create("div");
    				var input = new TextBox({
    					name: 'inputText',
    					placeHolder: 'filter text',
    					style: "width: 4em",
    					intermediateChanges: true,
    				});
    				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['events.id']});
    				domConstruct.place(label,div);
    				input.placeAt(div);
    				input.watch("value",function(name,oldValue,value){
    					if(value == '')
    						delete eventInstances.filters['id'];
    					else
    						eventInstances.filters['id'] = new RegExp("^.*"+value+".*$","i");
    					eventInstances.grid.set('query',eventInstances.filters);
    				});
    				
    				//Create sort link
    				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
    				on(sortLink,'click',function(event){
    					
    					//Flip through the list to see if we already have an order?
    					for(var i =0; i<eventInstances.sortMap.length; i++){
    						if(eventInstances.sortMap[i].attribute === "id"){
    							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
    							break;
    						}
    					}
    					var options = {};
    					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
    					eventInstances.grid.set("query",eventInstances.filters,options);
    				});
    				domConstruct.place(sortLink,div);
    				return div;
    		},
    	},
    	alarmLevel: {
    		label: mangoMsg['common.alarmLevel'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new Select({
					name: 'alarmLevelFilter',
					style: "width: 10em; color: gray",
					options: [
					    {label : mangoMsg['common.all'], value: ''},
						{label : mangoMsg['common.alarmLevel.none'], value: '0'},
						{label : mangoMsg['common.alarmLevel.info'], value: '1'},
						{label : mangoMsg['common.alarmLevel.urgent'], value: '2'},
						{label : mangoMsg['common.alarmLevel.critical'], value: '3'},
						{label : mangoMsg['common.alarmLevel.lifeSafety'], value: '4'},
					],
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.alarmLevel']});
				domConstruct.place(label,div);
				
				input.placeAt(div);
				
				input.watch("value",function(name,oldValue,value){
					if(value === '')
						delete eventInstances.filters['alarmLevel'];
					else
						eventInstances.filters['alarmLevel'] = value; //new RegExp("^.*"+value+".*$");
					eventInstances.grid.set('query',eventInstances.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "alarmLevel"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
    		renderCell: function(eventInstance, alarmLevel, cell){
    			var div = document.createElement("div");
    			div.style.textAlign = "center";
    			
    			var html = "";
    			if(eventInstance.active && eventInstance.alarmLevel == 0){
    				html = "<img src='/images/flag_green.png' title='";
    				html += mangoMsg['common.alarmLevel.none'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 0){
    				html = "<img src='/images/flag_green_off.png' title='";
    				html += mangoMsg['common.alarmLevel.none.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 1){
    				html = "<img src='/images/flag_blue.png' title='";
    				html += mangoMsg['common.alarmLevel.info'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 1){
    				html = "<img src='/images/flag_blue_off.png' title='";
    				html += mangoMsg['common.alarmLevel.info.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 2){
    				html = "<img src='/images/flag_yellow.png' title='";
    				html += mangoMsg['common.alarmLevel.urgent'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 2){
    				html = "<img src='/images/flag_yellow_off.png' title='";
    				html += mangoMsg['common.alarmLevel.urgent.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 3){
    				html = "<img src='/images/flag_orange.png' title='";
    				html += mangoMsg['common.alarmLevel.critical'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 3){
    				html = "<img src='/images/flag_orange_off.png' title='";
    				html += mangoMsg['common.alarmLevel.critical.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 4){
    				html = "<img src='/images/flag_red.png' title='";
    				html += mangoMsg['common.alarmLevel.lifeSafety'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 4){
    				html = "<img src='/images/flag_red_off.png' title='";
    				html += mangoMsg['common.alarmLevel.lifeSafety.rtn'];
    				html += "'/>"
    			}else{
    				html = mangoMsg['common.alarmLevel.unknown'];
    			}
    			
    			div.innerHTML = html;
    			cell.appendChild(div);
    			return div;
    		},
    	},
    	
		activeTimestampString: {
			label: mangoMsg['common.time'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new DateTextBox({
			        value: null,
			        style: "width: 10em; color: gray",
			    }, "sinceDate");
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.time']});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value === null)
						delete eventInstances.filters['activeTimestampString'];
					else{
						var date = new Date(value);
						eventInstances.filters['activeTimestampString'] = "Long:>" + date.getTime();
					}
					eventInstances.grid.set('query',eventInstances.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "activeTimestampString"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
		},
		
		messageString: {
			label: mangoMsg['events.msg'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['events.msg']});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value == '')
						delete eventInstances.filters['messageString'];
					else
						eventInstances.filters['messageString'] = new RegExp("^.*"+value+".*$","i");
					eventInstances.grid.set('query',eventInstances.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "messageString"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
    		renderCell: function(eventInstance, messageString, cell){
    			var div = document.createElement("div");
    			div.style.textAlign = "center";
    			//TODO Add comments too!
    			div.innerHTML = messageString;
    			
    			return div;
    		},
		},
		rtnTimestampString: {
			label: mangoMsg['common.inactiveTime'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.inactiveTime']});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value == '')
						delete eventInstances.filters['rtnTimestampString'];
					else
						eventInstances.filters['rtnTimestampString'] = new RegExp("^.*"+value+".*$","i");
					eventInstances.grid.set('query',eventInstances.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "rtnTimestampString"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
    		renderCell: function(eventInstance, inactiveTimeString, cell){
    			var div = document.createElement("div");
    			div.style.textAlign = "center";
    			var html = "";
    			if(eventInstance.active){
    				html = mangoMsg['common.active'];
    			}else if(!eventInstance.rtnApplicable){
    				html = mangoMsg['common.nortn'];
    			}else{
    				html = inactiveTimeString + " - " + eventInstance.rtnMessageString;
    			}
    			div.innerHTML = html;
    			
    			return div;
    		},
		},
		totalTimeString: {
			label: mangoMsg['common.duration'],
    		sortable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new TextBox({
					name: 'inputText',
					placeHolder: 'filter text',
					style: "width: 10em",
					intermediateChanges: true,
				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.duration']});
				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value == '')
						delete eventInstances.filters['totalTimeString'];
					else
						eventInstances.filters['totalTimeString'] = "Duration:>" + value;
					eventInstances.grid.set('query',eventInstances.filters);
				});
				
				//Create sort link
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "totalTimeString"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{attribute: eventInstances.sortMap[i].attribute, descending: eventInstances.sortMap[i].descending}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
		},	
    },
    
    //Not using these buttons, but just keep them here to ensure the renderButtons method happens
    buttons: ['toggle','edit','delete','copy','export'],
    
    /**
     * Override the renderButtons method
     */
    renderButtons: function(eventInstance, value, node, options) {
    	var div = document.createElement("div");
		div.style.textAlign = "center";
		var html = "";
    	if(eventInstance.userNotified){
    		if(eventInstance.acknowledged){
    			html = "<img src='images/tick_off.png' title='";
    			html += mangoMsg['events.acknowledged'];
    			html += "'/>";
    		}else{
    			html = "<img src='images/tick.png' title='";
    			html += mangoMsg['events.acknowledge'];
    			html += "' onclick='ackEvent(" + eventInstance.id;
    			html += ")'/>";
    			if(eventInstance.silenced){
        			html += "<img src='images/sound_mute.png' title='";
        			html += mangoMsg['events.unsilence'];
        			html += "' onclick='toggleSilence(" + eventInstance.id;
        			html += ")'/>";
    			}else{
        			html += "<img src='images/sound_none.png' title='";
        			html += mangoMsg['events.silence'];
        			html += "' onclick='toggleSilence(" + eventInstance.id;
        			html += ")'/>";
    			}
    		}// end if not ack
    	}//end if user notified
    	
    	//TODO Add in the link for a given data type edit
    	if(eventInstance.eventType.eventType === constants_DATA_POINT){
    		html += "<a href='data_point_details.shtm?dpid=" + eventInstance.eventType.dataPointId + "'><img src='/images/icon_comp.png' title='";
    		html += mangoMsg['events.pointDetails'];
    		html += "'/></a>";
    		
    	}else if(eventInstance.eventType.eventType === constants_DATA_SOURCE){
    		html += "<a href='data_source_edit.shtm?dsid=" + eventInstance.eventType.dataSourceId + "'><img src='/images/icon_ds_edit.png' title='";
    		html += mangoMsg['events.editDataSource'];
    		html += "'/></a>";
    		
    	}else if(eventInstance.eventType.eventType === constants_SYSTEM){
    		if(eventInstance.eventType.systemEentType === constants_TYPE_SET_POINT_HANDLER_FAILURE){
        		html += "<a href='event_handlers.shtm?ehid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/cog.png' title='";
        		html += mangoMsg['events.editEventHandler'];
        		html += "'/></a>";
        		
        	}else if(eventInstance.eventType.systemEventType === constants_TYPE_LICENSE_CHECK){
        		html += "<a href='modules.shtm'><img src='/images/puzzle.png' title='";
        		html += mangoMsg['modules.modules'] + "'/></a>";
        	}else{
        		//TODO Add systemEventTypeLinkHere
        	}
    	}else if(eventInstance.eventType.eventType === constants_PUBLISHER){
    		html += "<a href='publisher_edit.shtm?pid='>"+ eventInstance.eventType.publisherId +"<img src='/images/transmit_edit.png' title='";
    		html += mangoMsg['events.editPublisher'] + "'/></a>";
    	}else if(eventInstance.eventType.eventType === constants_AUDIT){
	   		if(eventInstance.eventType.systemEentType === constants_AUDIT_TYPE_DATA_SOURCE){
	    		html += "<a href='data_source_edit.shtm?dsid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_ds_edit.png' title='";
	    		html += mangoMsg['events.editDataSource'];
	    		html += "'/></a>";
	    		
	    	}else if(eventInstance.eventType.auditEventType === constants_AUDIT_TYPE_DATA_POINT){
	    		html += "<a href='data_point_edit.shtm?dpid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_comp_edit.png' title='";
	    		html += mangoMsg['events.pointEdit'];
	    		html += "'/></a>";
	    		//TODO This is probably wrong refid 2 maybe?
	    		html += "<a href='data_source_edit.shtm?pid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_ds_edit.png' title='";
	    		html += mangoMsg['events.editDataSource'];
	    		html += "'/></a>";
	    		
	    	}else if(eventInstance.eventType.auditEventType === constants_AUDIT_TYPE_POINT_EVENT_DETECTOR){
	    		html += "<a href='data_point_edit.shtm?pedid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_comp_edit.png' title='";
	    		html += mangoMsg['events.pointEdit'];
	    		html += "'/></a>";
	    	}else if(eventInstance.eventType.auditEventType === constants_AUDIT_TYPE_EVENT_HANDLER){
	    		html += "<a href='event_handlers.shtm?ehid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/cog.png' title='";
	    		html += mangoMsg['events.editHandler'];
	    		html += "'/></a>";
	    	}else{
	    		//TODO auditEvenTypeLink here
	    	}
		}else{
			//TODO eventTypeLink here
		}

    	
    	
    	
    	
    	div.innerHTML = html;
		return div;
    },
    
    
    
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

    },


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
    
    
    
	}); // Store View


}); // require
