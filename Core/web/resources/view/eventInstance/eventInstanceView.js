/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

var eventInstances;

require(["deltamation/StoreView", "dijit/form/Button", "dijit/form/DateTextBox","dijit/form/TimeTextBox", "dijit/form/MultiSelect", "dijit/form/Select", "dijit/form/ValidationTextBox","dijit/form/TextBox",
         "dojo/date", "dojo/dom-style","dojo/_base/html", "put-selector/put", "dojo/when", "dojo/on",
         "dojo/_base/fx", "dojo/fx","dojo/query", "dojox/layout/ContentPane","dojo/dom-construct",
         "dojo/domReady!"],
function(StoreView, Button, DateTextBox,TimeTextBox, MultiSelect, Select, ValidationTextBox,TextBox,
		date, domStyle,html,put,when,on,
		baseFx,coreFx,query,ContentPane,domConstruct) {

    //Funky auto-filtering for use from Alarms Toaster Widget
    var defaultEventInstanceQuery,defaultEventLevelValue;
    if(alarmLevelUrlParameter == 'none'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=0",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 0;
    }else if(alarmLevelUrlParameter == 'information'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=1",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 1;
    }else if(alarmLevelUrlParameter == 'important'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=2",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 2;
    }else if(alarmLevelUrlParameter == 'warning'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=3",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 3;
    }else if(alarmLevelUrlParameter == 'urgent'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=4",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 4;
    }else if(alarmLevelUrlParameter == 'critical'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=5",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 5;
    }else if(alarmLevelUrlParameter == 'lifeSafety'){
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=6",
                acknowledged: "NullCheck:true",
              };
        defaultEventLevelValue = 6;
    }else{
        defaultEventInstanceQuery = {
                alarmLevel: "Int:>=1",
                acknowledged: "NullCheck:true",
                //rtnApplicable: 'Y',
                //rtnTs: "NullCheck:true",
              };
        defaultEventLevelValue = 1;
    }   
    
    
eventInstances = new StoreView({
	
    prefix: 'EventInstance',
    varName: 'eventInstances',
    viewStore: stores.eventInstances,
    editStore: stores.eventInstances,
    editUpdatesView: true,
    gridId: 'eventInstanceTable',
    editId: 'editEventInstanceDiv',
    defaultSort: [{attribute: "activeTimestamp", descending: true}],
    defaultQuery: defaultEventInstanceQuery,
    minRowsPerPage: 200,
    maxRowsPerPage: 200,
    filters: {
    			alarmLevel: "Int:>=1",
    			acknowledged: "NullCheck:true",
				//rtnApplicable: 'Y',
				//rtnTs: "NullCheck:true",
    		 },
             
    
    
	sortMap: [
	          {attribute: "id", descending:true},
	          {attribute: "alarmLevel", descending:true},
	          {attribute: "activeTimestampString", descending:true},
	          {attribute: "messageString", descending:true},
	          {attribute: "rtnTimestampString", descending: true},
	          {attribute: "totalTimeString", descending: true},
	          {attribute: "acknowledged", descending: true},
	         ],
    
    columns: {
    	id: {
    		label: mangoMsg['events.id'],
    		sortable: false,
    		resizable: false,
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
    		resizable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				
				var input = new Select({
				    id: 'alarmLevelFilter',
					name: 'alarmLevelFilter',
					style: "width: 10em; color: gray",
					options: [
						{label : mangoMsg['common.alarmLevel.greaterthan.none'], value: '0'},
						{label : mangoMsg['common.alarmLevel.greaterthan.info'], value: '1'},
						{label : mangoMsg['common.alarmLevel.greaterthan.important'], value: '2'},
						{label : mangoMsg['common.alarmLevel.greaterthan.warning'], value: '3'},
						{label : mangoMsg['common.alarmLevel.greaterthan.urgent'], value: '4'},
						{label : mangoMsg['common.alarmLevel.greaterthan.critical'], value: '5'},
						{label : mangoMsg['common.alarmLevel.greaterthan.lifeSafety'], value: '6'},
					],
				});
				input.set('value', defaultEventLevelValue); //DEFAULT FOR PAGE
				//No Label, self explanitory?
//				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.alarmLevel']});
//				domConstruct.place(label,div);
				
				input.placeAt(div);
				
				input.watch("value",function(name,oldValue,value){
					//This is the same as "Int:>=0" but faster for this column
					if(value == '0')
						delete eventInstances.filters['alarmLevel'];
					else
						eventInstances.filters['alarmLevel'] = "Int:>=" + value;					
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
    			
    			var html = "";
    			if(eventInstance.active && eventInstance.alarmLevel == 0){
    				html = "<img src='/images/flag_grey.png' title='";
    				html += mangoMsg['common.alarmLevel.none'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 0){
    				html = "<img src='/images/flag_grey_off.png' title='";
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
    				html = "<img src='/images/flag_aqua.png' title='";
    				html += mangoMsg['common.alarmLevel.info'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 2){
    				html = "<img src='/images/flag_aqua_off.png' title='";
    				html += mangoMsg['common.alarmLevel.info.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 3){
    				html = "<img src='/images/flag_green.png' title='";
    				html += mangoMsg['common.alarmLevel.info'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 3){
    				html = "<img src='/images/flag_green_off.png' title='";
    				html += mangoMsg['common.alarmLevel.info.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 4){
    				html = "<img src='/images/flag_yellow.png' title='";
    				html += mangoMsg['common.alarmLevel.urgent'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 4){
    				html = "<img src='/images/flag_yellow_off.png' title='";
    				html += mangoMsg['common.alarmLevel.urgent.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 5){
    				html = "<img src='/images/flag_orange.png' title='";
    				html += mangoMsg['common.alarmLevel.critical'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 5){
    				html = "<img src='/images/flag_orange_off.png' title='";
    				html += mangoMsg['common.alarmLevel.critical.rtn'];
    				html += "'/>"
    			}else if(eventInstance.active && eventInstance.alarmLevel == 6){
    				html = "<img src='/images/flag_red.png' title='";
    				html += mangoMsg['common.alarmLevel.lifeSafety'];
    				html += "'/>"
    			}else if(eventInstance.alarmLevel == 6){
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
    		resizable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
//				var input = new DateTextBox({
//			        value: null,
//			        style: "width: 10em; color: gray",
//			    }, "sinceDate");
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.time']});
				domConstruct.place(label,div);
//				input.placeAt(div);
//				input.watch("value",function(name,oldValue,value){
//					if(value === null)
//						delete eventInstances.filters['activeTimestampString'];
//					else{
//						var date = new Date(value);
//						eventInstances.filters['activeTimestampString'] = "Long:>" + date.getTime();
//					}
//					eventInstances.grid.set('query',eventInstances.filters);
//				});
				
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
    		resizable: false,
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
				
				//Create sort link if the databaseType if right (This is set on the JSP Page)
				if(databaseType === 'MYSQL'){
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
				}
				return div;
    		},
    		renderCell: function(eventInstance, messageString, cell){
    			var div = document.createElement("div");

    			var html = messageString;
    			html += "<img src='/images/comment_add.png' onclick='eventInstances.openAlarmCommentDialog(" 
    				+ eventInstance.id + ")' title='"
    				+ mangoMsg['notes.addNote'] + "'/>";
        			//TODO Add comments too!
    			var comments = eventInstance.commentsHTML;
    			if(comments != '')
    				html += comments;
    			div.innerHTML = html;
    			
    			return div;
    		},
		},
		rtnTimestampString: {
			label: mangoMsg['common.status'],
    		sortable: false,
    		resizable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.status']});
				domConstruct.place(label,div);
				var input = new Select({
					name: 'status-filter',
					style: "width: 10em; color: gray",
					options: [
						{label : mangoMsg['common.all'], value: '0'},
						{label : mangoMsg['common.active'], value: '1'},
						{label : mangoMsg['event.rtn.rtn'], value: '2'},
						{label : mangoMsg['common.nortn'], value: '3'},
					],
				});
				input.set('value', '0'); //DEFAULT FOR PAGE				
				input.placeAt(div);
				
				input.watch("value",function(name,oldValue,value){
					if(value == '0'){
						delete eventInstances.filters['rtnApplicable'];
						delete eventInstances.filters['rtnTs'];
					}else if(value == '1'){
						eventInstances.filters['rtnApplicable'] = 'Y';	
						eventInstances.filters['rtnTs'] = "NullCheck:true";
					}else if(value == '2'){
						eventInstances.filters['rtnApplicable'] = 'Y';	
						eventInstances.filters['rtnTs'] = "NullCheck:false";
					}else if(value == '3'){
						eventInstances.filters['rtnApplicable'] = 'N';	
						delete eventInstances.filters['rtnTs'];	
					}
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
    		resizable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
//				var input = new TextBox({
//					name: 'inputText',
//					placeHolder: 'filter text',
//					style: "width: 10em",
//					intermediateChanges: true,
//				});
				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.duration']});
				domConstruct.place(label,div);
				//input.placeAt(div);
//				input.watch("value",function(name,oldValue,value){
//					if(value == '')
//						delete eventInstances.filters['totalTimeString'];
//					else
//						eventInstances.filters['totalTimeString'] = "Duration:>" + value;
//					eventInstances.grid.set('query',eventInstances.filters);
//				});
				
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
					options.sort = [{
							attribute: eventInstances.sortMap[i].attribute, 
							descending: eventInstances.sortMap[i].descending,
							local: true
							}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
		},
		acknowledged:{
			label: mangoMsg['events.acknowledged'],
    		sortable: false,
    		resizable: false,
    		renderHeaderCell: function(th){
				var div = domConstruct.create("div");
				var input = new Select({
					name: 'alarmLevelFilter',
					style: "width: 10em; color: gray",
					options: [
					    {label : mangoMsg['common.all'], value: '-1'},
					    {label : mangoMsg['events.acknowledged'], value: 'false'},
						{label : mangoMsg['events.unacknowledged'], value: 'true'},
					],
				});
				
				input.set('value', 'true'); //DEFAULT FOR PAGE
				//Disable input, its self explanitory
//				var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['events.acknowledged']});
//				domConstruct.place(label,div);
				input.placeAt(div);
				input.watch("value",function(name,oldValue,value){
					if(value === '-1')
						delete eventInstances.filters['acknowledged'];
					else
						eventInstances.filters['acknowledged'] = "NullCheck:" + value;
					eventInstances.grid.set('query',eventInstances.filters);
				});

				
				//Create sort link (NOT DOING As this is used in a join and doesn't work the way one would expect)
				var sortLink  = domConstruct.create("span",{style: "padding-right: 5px; float: right", innerHTML:  "sort",});
				on(sortLink,'click',function(event){
					
					//Flip through the list to see if we already have an order?
					for(var i =0; i<eventInstances.sortMap.length; i++){
						if(eventInstances.sortMap[i].attribute === "acknowledged"){
							eventInstances.sortMap[i].descending = !eventInstances.sortMap[i].descending;
							break;
						}
					}
					var options = {};
					options.sort = [{
							attribute: eventInstances.sortMap[i].attribute, 
							descending: eventInstances.sortMap[i].descending,
							local: true
							}];
					eventInstances.grid.set("query",eventInstances.filters,options);
				});
				domConstruct.place(sortLink,div);
				return div;
    		},
    		renderCell: function(eventInstance, userNotified, cell){
    			var div = document.createElement("div");
    			div.className = "dgrid-column-acknowledged";
    			div.id = "event-acknowledged-div-" + eventInstance.id;
    			var html = "";
    	    	if(eventInstance.userNotified){
    	    		if(eventInstance.acknowledged){
    	    			html = "<img src='images/tick_off.png' title='";
    	    			html += mangoMsg['events.acknowledged'];
    	    			html += "'/>";
    	    		}else{
    	    			html = "<img src='images/tick.png' id='ackImg";
    	    			html += eventInstance.id + "' title='";
    	    			html += mangoMsg['events.acknowledge'];
    	    			html += "' onclick='ackEvent(" + eventInstance.id;
    	    			html += ")'/>";
    	    			if(eventInstance.silenced){
    	        			html += "<img src='images/sound_mute.png' id='silenceImg";
    	        			html += eventInstance.id + "' title='";
    	        			html += mangoMsg['events.unsilence'];
    	        			html += "' onclick='toggleSilence(" + eventInstance.id;
    	        			html += ")'/>";
    	    			}else{
    	        			html += "<img src='images/sound_none.png' id='silenceImg";
    	        			html += eventInstance.id + "' title='";
    	        			html += mangoMsg['events.silence'];
    	        			html += "' onclick='toggleSilence(" + eventInstance.id;
    	        			html += ")'/>";
    	    			}
    	    		}// end if not ack
    	    	}//end if user notified
    	    	
    	    	//Add in the link for a given data type edit
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
    	        	}else if(eventInstance.eventType.systemEventType === constants_TYPE_UPGRADE_CHECK){
    	        		html += "<a href='modules.shtm'><img src='/images/puzzle.png' title='";
    	        		html += mangoMsg['modules.modules'] + "'/></a>";
    	        	}else if(eventInstance.eventType.systemEventType === constants_TYPE_SYSTEM_STARTUP){
    	        	    //No HTML Link for this
    	    	    }else if(eventInstance.eventType.systemEventType === constants_TYPE_SYSTEM_SHUTDOWN){
                        //No HTML Link for this
                    }else if(eventInstance.eventType.systemEventType === constants_TYPE_USER_LOGIN){
                        //No HTML Link for this
                    }else{
    	        		EventInstanceDwr.getSystemEventTypeLink(div.id, eventInstance.eventType.systemEventType, eventInstance.eventType.referenceId1, eventInstance.eventType.referenceId2,function(response){
                            if(response.data.link != null){
                                var toEdit = dojo.byId(response.data.divId);
                                toEdit.innerHTML += response.data.link;
                            }
    	        		});
    	        	}
    	    	}else if(eventInstance.eventType.eventType === constants_PUBLISHER){
    	    		html += "<a href='publisher_edit.shtm?pid="+ eventInstance.eventType.publisherId +"'><img src='/images/transmit_edit.png' title='";
    	    		html += mangoMsg['events.editPublisher'] + "'/></a>";
    	    	}else if(eventInstance.eventType.eventType === constants_AUDIT){
    		   		if(eventInstance.eventType.auditEventType === constants_AUDIT_TYPE_DATA_SOURCE){
    		    		html += "<a href='data_source_edit.shtm?dsid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_ds_edit.png' title='";
    		    		html += mangoMsg['events.editDataSource'];
    		    		html += "'/></a>";
    		    		
    		    	}else if(eventInstance.eventType.auditEventType === constants_AUDIT_TYPE_DATA_POINT){
    		    		html += "<a href='data_point_edit.shtm?dpid=" + eventInstance.eventType.referenceId1 + "'><img src='/images/icon_comp_edit.png' title='";
    		    		html += mangoMsg['events.pointEdit'];
    		    		html += "'/></a>";
    		    		
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
    		    		EventInstanceDwr.getAuditEventTypeLink(div.id, eventInstance.eventType.auditEventType, eventInstance.eventType.referenceId1, eventInstance.eventType.referenceId2, function(response){
                            if(response.data.link != null){
                                var toEdit = dojo.byId(response.data.divId);
                                toEdit.innerHTML += response.data.link;
                            }

    		    		});
    		    	}
    			}else{
    			    var def = new dojo.Deferred();
    			    def.then(function(id){
    			        
    			    });
    				EventInstanceDwr.getEventTypeLink(div.id, eventInstance.eventType.eventType, eventInstance.eventType.eventSubtype, eventInstance.eventType.referenceId1, eventInstance.eventType.referenceId2, function(response){
                        if(response.data.link != null){
                            var toEdit = dojo.byId(response.data.divId);
                            toEdit.innerHTML += response.data.link;
                        }
    				});
    			}
    	    	
    	    	//Add on the timestamp if it was acknowledged
    	    	html += " " + eventInstance.ackMessageString;
    	    	
    	    	div.innerHTML = html;
    	    	return div;
    		}//end render cell
    		},
			
    },
    
    //Not using these buttons, 
    buttons: [],
    
    preInit: function() {
    },
    
    postInit: function() {
    },
    
    setInputs: function(vo) {
    	
    	this.currentId = vo.id;
    	this.name.set('value',vo.name);
    	this.xid.set('value',vo.xid);
    	this.enabled.set('value',vo.enabled);
    },
    
    getInputs: function() {

    },

    openAlarmCommentDialog: function(referenceId){
    	openCommentDialog(constants_USER_COMMENT_TYPE_EVENT,referenceId,this.saveCommentCallback)
    },

    saveCommentCallback: function(comment){
        if (!comment)
            alert(mangoMsg['notes.enterComment']);
        else{
            closeCommentDialog();
            eventInstances.refresh();
        }
   
    },
    
    editXOffset: -380,
    editYOffset: 0,
    addXOffset: 18,
    addYOffset: -240,
    

    /**
     * Refresh the Grid
     */
    refresh: function(){
    	//this.grid.set('query', null);
    	this.grid.set("query",eventInstances.filters,null);
    },
	
    download: function() {
    	//The Export Query will be set every time a DOJO Query is run
    	// so we can export whenever
        //TODO Implement this in the data exporter servlet window.location = "/download.shtm?downloadFile=true&dataType=eventInstance"; 
        window.location = "eventExport/eventData.xlsx";	
    },
    
    /**
     * Acknowledge all events in the view that are unacknowleged
     */
    acknowledgeEventsInView: function(){
    	//Start flasher
    	startImageFader("ackEventsInViewImg", true);
        EventInstanceDwr.acknowledgeEvents(function(response){
            if(response.hasMessages){
                showDwrMessages(response.messages);
            }
            eventInstances.grid.refresh();
            //StopFlasher
            stopImageFader("ackEventsInViewImg");
        });
        
    },

    /**
     * Acknowledge all events in the view that are unacknowleged
     */
    silenceEventsInView: function(){
    	startImageFader("silenceEventsInViewImg", true);
        EventInstanceDwr.silenceEvents(function(response){
            if(response.hasMessages){
                showDwrMessages(response.messages);
            }
            eventInstances.grid.refresh();
            stopImageFader("silenceEventsInViewImg");
        });
        
    },
    
    acknowledgeAll: function(){
    	MiscDwr.acknowledgeAllPendingEvents(function(response){
    		eventInstances.grid.refresh();
    	});
    	
    },
    
    /**
     * Add a user to filter on,
     * should be called during page init.
     * Currently being done in EventInstanceDwr query call
     */
    filterUser: function(id){
    	this.filters['userId'] = id;
    }
    
    
    
	}); // Store View

	stores.eventInstances.dwr.queryCallback = function(response){
		var countDiv = dojo.byId("totalEventInstancesInView");
		countDiv.innerHTML = "<span class='smallTitle'>" + mangoMsg['common.totalResults'] + " " + response.data.total + "</span>";
	};
	
	
	
	
	//Setup the date pickers at the top of the table
	var eventInstanceReportFromDate,eventInstanceReportFromTime,eventInstanceReportToDate,eventInstanceReportToTime;
	var div = dojo.byId("eventDateSelectionDiv");
	var updateOnDateTimeChange = true; //Flag to disable the query update when we are programatically changing the date/times
	
	/* Clear Button */
	var clearButton = new Button({
		label: mangoMsg['common.clearDates'],
		class: "marR",
		onClick: function(){
			//Disable the query so we don't run it 2 times
			updateOnDateTimeChange = false;
			fromDate.set('value',null);
			fromTime.set('value',null);
			toDate.set('value',null);
			toTime.set('value',null);
			updateOnDateTimeChange = true;
			createLongRangeFilter(
					eventInstanceReportFromDate,
					eventInstanceReportFromTime,
					eventInstanceReportToDate,
					eventInstanceReportToTime );
			eventInstances.grid.set('query',eventInstances.filters); //Update the query
		},
	},"resetDatesButton");
	clearButton.placeAt(div);
	
	var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.dateRangeFrom']});
	domConstruct.place(label,div);
	var fromDate = new DateTextBox({
	    value: null,
	    style: "width: 10em; color: gray",
	}, "fromDate");
	//var label = domConstruct.create("span",{style: "padding-right: 5px", innerHTML:  mangoMsg['common.dateRangeFrom']});
	//domConstruct.place(label,div);
	fromDate.placeAt(div);
	fromDate.watch("value",function(name,oldValue,value){

		if(value === null){
			eventInstanceReportFromDate = null;
		}else{
			eventInstanceReportFromDate = value;
			
			if(eventInstanceReportFromTime == null){ //Update the time if there isn't one
				updateOnDateTimeChange = false;
				fromTime.set('value',
						new Date(value.getFullYear(), value.getMonth(), value.getDate(),0,0,0,0)); //Default to Midnight
				updateOnDateTimeChange = true;
			}
		}
		//If we are not supposed to run the query don't
		if(updateOnDateTimeChange === false)
			return;
		createLongRangeFilter(
				eventInstanceReportFromDate,
				eventInstanceReportFromTime,
				eventInstanceReportToDate,
				eventInstanceReportToTime );			
		eventInstances.grid.set('query',eventInstances.filters);
	});

	var fromTime = new TimeTextBox({
		value: null,
		style: "width: 10em; color: gray",
	}, "fromTime");
	fromTime.placeAt(div);
	fromTime.watch("value",function(name,oldValue,value){

		if(value === null){
			eventInstanceReportFromTime = null;
		}else{
			eventInstanceReportFromTime = value;
			if(eventInstanceReportFromDate == null){
				//Update Date to today if there isn't one
				updateOnDateTimeChange = false;
				var today = new Date();
				fromDate.set('value',
						new Date(today.getFullYear(), today.getMonth(), today.getDate(),0,0,0,0));
				updateOnDateTimeChange = true;
			}

		}		
		//If we are not supposed to run the query don't
		if(updateOnDateTimeChange === false)
			return;
		createLongRangeFilter(
				eventInstanceReportFromDate,
				eventInstanceReportFromTime,
				eventInstanceReportToDate,
				eventInstanceReportToTime );	
		eventInstances.grid.set('query',eventInstances.filters);
	});
	
	var toDate = new DateTextBox({
	    value: null,
	    style: "width: 10em; color: gray",
	}, "toDate");
	var label = domConstruct.create("span",{style: "padding-right: 5px; padding-left: 10px;", innerHTML:  mangoMsg['common.dateRangeTo']});
	domConstruct.place(label,div);
	toDate.placeAt(div);
	toDate.watch("value",function(name,oldValue,value){
		
		if(value === null){
			eventInstanceReportToDate = null;
		}else{
			eventInstanceReportToDate = value;
			if(eventInstanceReportToTime == null){
				updateOnDateTimeChange = false;
				//Update the time if there isn't one
				toTime.set('value',
						new Date(value.getFullYear(), value.getMonth(), value.getDate(),0,0,0,0)); //Default to Midnight
				updateOnDateTimeChange = true;
			}
		}		
		//If we are not supposed to run the query don't
		if(updateOnDateTimeChange === false)
			return;
		createLongRangeFilter(
				eventInstanceReportFromDate,
				eventInstanceReportFromTime,
				eventInstanceReportToDate,
				eventInstanceReportToTime );	
		eventInstances.grid.set('query',eventInstances.filters);
	});
	var toTime = new TimeTextBox({
		value: null,
		style: "width: 10em; color: gray",
	}, "toTime");
	toTime.placeAt(div);
	toTime.watch("value",function(name,oldValue,value){
		
		if(value === null){
			eventInstanceReportToTime = null;
		}else{
			eventInstanceReportToTime = value;
			if(eventInstanceReportToDate == null){
				//Update Date to today if there isn't one
				updateOnDateTimeChange = false;
				var today = new Date();
				toDate.set('value',
						new Date(today.getFullYear(), today.getMonth(), today.getDate(),0,0,0,0));
				updateOnDateTimeChange = true;
			}
		}
		//If we are not supposed to run the query don't
		if(updateOnDateTimeChange === false)
			return;
		createLongRangeFilter(
				eventInstanceReportFromDate,
				eventInstanceReportFromTime,
				eventInstanceReportToDate,
				eventInstanceReportToTime );	

		eventInstances.grid.set('query',eventInstances.filters);
	});

	/**
	 * Create a date from the 
	 */
	function createDateTime(date,time){
		
		if(date==null && time==null)
			return null;
		if(date == null)
			date = new Date(); //Default to today
		if(time == null)
			time = new Date(date.getFullYear(), date.getMonth(), date.getDate(),
					0,0,0,0); //Default to Midnight
		return new Date(
				date.getFullYear(), date.getMonth(), date.getDate(),
				time.getHours(), time.getMinutes(), time.getSeconds(),
				time.getMilliseconds()
				);
	}
	
	/**
	 * Create a filter for the date range from 4 Date objects
	 * 
	 * fromDate,toDate - Dates with Day/Month/Year set
	 * fromTime,toTime - Dates with Hour/Minute/Second/milli set
	 * 
	 */
	function createLongRangeFilter(fromDate,fromTime,toDate,toTime){
		
		var from = createDateTime(fromDate,fromTime);
		var to = createDateTime(toDate,toTime);

		if((from !== null)&&(to !== null))
			eventInstances.filters.activeTimestampString = "LongRange:>" + from.getTime() + ":<" + to.getTime();
		else if(from !== null)
			eventInstances.filters.activeTimestampString = "Long:>" + from.getTime();
		else if(to !== null)
			eventInstances.filters.activeTimestampString = "Long:<" + to.getTime();
		else if ((to === null)&&(from === null)){
			delete eventInstances.filters.activeTimestampString;
		}
	}
	
}); // require
