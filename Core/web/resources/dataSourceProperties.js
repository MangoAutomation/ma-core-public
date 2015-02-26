/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
 //TODO Make new AMD format
 dojo.require("dijit.Dialog");
 dojo.require("dijit.form.Form");
 dojo.require("dijit.form.Button");
 dojo.require("dojo.dom-construct");
 dojo.require("dijit.layout.TabContainer");
 dojo.require("dijit.layout.ContentPane");
 
 
 var currentPoint;
 var pointListColumnFunctions;
 var pointListOptions;
 var currentDsId; 
 
 function setCurrentDsId(dsId){
	 currentDsId = dsId;
 }
 
 /**
  * Init the point properties for a given data source
  * 
  * @param dsId - ID of Datasource
  */
 function initProperties(dataSourceId,dataSourceEnabled) {
	 
	    var tc = new dijit.layout.TabContainer({
	        style: "height: auto",
	        doLayout: false,
	    }, "dataSourcePropertiesTabContainer");

	    var cp2 = new dijit.layout.ContentPane({
	         title: "Data Source",
	         style: "overflow-y: auto",
	         selected: true,
	         content: "<div id='dataSourceDetails-content'></div>",
	         id: 'dataSourceDetails-tab',
	    });
	    tc.addChild(cp2);
	    var pd = dojo.byId("dataSourcePropertiesTab");
	    dojo.place(pd,"dataSourceDetails-content");
	    
	    if(dataSourceId > 0){
	    	createDataPointsTab();
	    }
	    tc.startup();
	 
	 //For now set the current ID to use for toggle
	 currentDsId = dataSourceId;
	 
	 //Query the remote store
	 dataPointsDataSourceId = dataSourceId;
	 dataPoints.viewStore.dwr.loadData = true;
	 dataPoints.refresh(); //TODO Need to make page empty at start, not load 2x
	 
	 
	 changeDataSourcePurgeOverride();
     pointListColumnFunctions = new Array();
     var pointListColumnHeaders = new Array();

     pointListColumnHeaders.push("<fmt:message key='dsEdit.deviceName'/>");
     pointListColumnFunctions.push(function(p) { return "<b>"+ p.deviceName +"</b>"; });
     
     pointListColumnHeaders.push("<fmt:message key='dsEdit.name'/>");
     pointListColumnFunctions.push(function(p) { return "<b>"+ p.name +"</b>"; });
     
     pointListColumnHeaders.push("<fmt:message key='dsEdit.pointDataType'/>");
     pointListColumnFunctions.push(function(p) { return p.dataTypeMessage; });
     
     pointListColumnHeaders.push("<fmt:message key='dsEdit.status'/>");
     pointListColumnFunctions.push(function(p) {
             var id = "toggleImg"+ p.id;
             var onclick = "togglePoint("+ p.id +")";
             if (p.enabled)
                 return writeImage(id, null, "brick_go", "<fmt:message key='common.enabledToggle'/>", onclick);
             return writeImage(id, null, "brick_stop", "<fmt:message key='common.disabledToggle'/>", onclick);
     });

     if (typeof appendPointListColumnFunctions == 'function')
         appendPointListColumnFunctions(pointListColumnHeaders, pointListColumnFunctions);
     
       var dsStatus = $("dsStatusImg");
       setDataSourceStatusImg(dataSourceEnabled, dsStatus);
       show(dsStatus);
     
    if (typeof initImpl == 'function') initImpl();
     
	//Get any alarms
	getAlarms();
     showMessage("dataSourceMessage");
     showMessage("pointMessage");
     
     getStatusMessages();
     
     //Init the point settings
     initDataPointTemplates();
     textRendererEditor.init();
     pointEventDetectorEditor.init();
     
     
     
 }
 
 function writePointList(points){
	 dataPoints.refresh();
	 allDataPoints.refresh();
 }

 function saveDataSource() {
     startImageFader("dsSaveImg", true);
     hideContextualMessages($("dataSourceProperties"));
     
     saveDataSourceImpl({
         name: $get("dataSource.name"),
         xid: $get("dataSource.xid"),
         editPermission: $get("dataSource.editPermission"),
         enabled: $get("dataSource.enabled"),
         purgeOverride: $get("dataSource.purgeOverride"),
         purgePeriod: $get("dataSource.purgePeriod"),
         purgeType: $get("dataSource.purgeType")
     });
 }
 
 /**
  * Callback from Module DS Save
  * @param response
  */
 function saveDataSourceCB(response) {
     stopImageFader("dsSaveImg");
     if (response.hasMessages){
    	 //Prefix the common base DataSourceVO property  message with dataSource. so we can use on same page as other members with context key's of name and xid, etc.
    	 for(var i=0; i<response.messages.length; i++){
    		 if((response.messages[i].contextKey === 'name')||
    				 (response.messages[i].contextKey === 'purgeOverride')||
    				 (response.messages[i].contextKey === 'purgePeriod')||
    				 (response.messages[i].contextKey === 'purgeType')){
    		 response.messages[i].contextKey = 'dataSource.' + response.messages[i].contextKey;
    		 }
    	 }
    	 showDwrMessages(response.messages);
     }else {
    	 
    	 //Finish the copy if necessary
    	//Are we making a copy?
         if(dataSources.copyId != null){
             //Copy Permissions and Points across
             DataSourceDwr.finishCopy(
             		dataSources.copyId,
             		response.data.id,
             	function(response){
             		dataSources.copyId = null; //Done with copy
             		hide("copyDeviceName");
 
             		 showMessage("dataSourceMessage", mangoTranslate('dsEdit.saved'));
	       	    	 createDataPointsTab(); //For The saving of new sources
	       	         //Need to Update the DS Table for sure
	       	         if(typeof dataSources != 'undefined'){
	       	        	 dataSources.setInputs(response.data.vo);
	       	        	 dataSources.refresh();
	       	         }
	       	         if(typeof allDataPoints != 'undefined'){
	       	        	 allDataPoints.refresh();
	       	         }
	       	         if(typeof dataPoints != 'undefined'){
	       	        	 dataPointsDataSourceId = response.data.vo.id;
	       	        	 dataPoints.refresh();
	       	         }
             });
         }else{
	         showMessage("dataSourceMessage", mangoTranslate('dsEdit.saved'));
	    	 createDataPointsTab(); //For The saving of new sources
	         //Need to Update the DS Table for sure
	         if(typeof dataSources != 'undefined'){
	        	 dataSources.setInputs(response.data.vo);
	        	 dataSources.refresh();
	         }
	         if(typeof allDataPoints != 'undefined'){
	        	 allDataPoints.refresh();
	         }
	         if(typeof dataPoints != 'undefined'){
	        	 dataPointsDataSourceId = response.data.vo.id;
	        	 dataPoints.refresh();
	         }
	     }
     }
     getAlarms();
     
 }
 
 /**
  * Toggle data source from edit view
  */
 function toggleDataSource() {
     if (typeof toggleDataSourceImpl == 'function') toggleDataSourceImpl();
     
     var imgNode = $("dsStatusImg");
     if(typeof dataSources != 'undefined'){
	     if (!hasImageFader(imgNode)) {
	         startImageFader(imgNode);
	         var enabled = dataSources.toggle(dataSources.currentId);
	     }
     }else{
    	 //No DS Table defined (Is this possible?)
	     if (!hasImageFader(imgNode)) {
	    	 startImageFader(imgNode);
	         DataSourceDwr.toggle(currentDsId,function(result) {
	             var imgNode = $("dsStatusImg");
	             stopImageFader(imgNode);
	             setDataSourceStatusImg(result.data.enabled, imgNode);
	             getAlarms();
	             getStatusMessages();
	         });
	     }
     }
 }
 

// function togglePoint(pointId) {
//     startImageFader("toggleImg"+ pointId, true);
//     DataSourceEditDwr.togglePoint(pointId, function(response) {
//         stopImageFader("toggleImg"+ response.data.id);
//         writePointList(response.data.points);
//     });
// }
 
  
// function writePointList(points) {
//     if (typeof writePointListImpl == 'function') writePointListImpl(points);
//     
//     if (!points)
//         return;
//     show("pointProperties");
//     show("alarmsTable");
//     show("dsStatusImg");
//     
//     if (currentPoint)
//         stopImageFader("editImg"+ currentPoint.id);
//     dwr.util.removeAllRows("pointsList");
//     dwr.util.addRows("pointsList", points, pointListColumnFunctions, pointListOptions);
// }
 
 
 /**
  * Method for legacy modules to add a point
  */
 function addPoint(ref) {
	 
	 //Ensure data source is saved, by confirming that the 
	 // data points list tab is viewable (this is the same as legacy code for now)
	 // TODO replace pointProperties with the pointTable-content 
	 // @See createDataPointsTab() below
	 if (!isShowing("pointProperties")) {
         alert(mangoTranslate('dsEdit.saveWarning'));
         return;
     }

     addPointImpl(ref);
     
 }
 
 /**
  * Not sure if still being used
  * @param pointId
  */
 function editPoint(pointId) {
     hideContextualMessages("pointProperties");
     DataSourceEditDwr.getPoint(pointId, editPointCB);
 }
 
 // This method can be used by implementations to add a new point from e.g. a tool. See Modbus for an example.
 function editPointCB(point) {
	 
	 //Load in the proper values
	 var locator = point.pointLocator;
	 
	 //Notify that the Data Type may have been changed
	 dataTypeChanged(locator.dataTypeId);
	 
	 //dataPoints.setInputs(point);
	 dataPoints.open(point.id,{voToLoad: point});
	 
	 if (typeof editPointCBImpl == 'function') 
		 cancel = editPointCBImpl(locator, point);
	 if (!cancel) {
		 //Give focus to the tab
		 var myEdit = dijit.byId("dataSourcePropertiesTabContainer");
		 myEdit.selectChild('dataPointDetails-tab');
	 }


//     currentPoint = point;
//     display("pointDeleteImg", point.id != mango.newId);
//     var locator = currentPoint.pointLocator;
//     
//     $set("name", currentPoint.name);
//     $set("xid", currentPoint.xid);
//     var cancel;
//     if (typeof editPointCBImpl == 'function') 
//         cancel = editPointCBImpl(locator);
//     if (!cancel) {
//         var img = "editImg"+ point.id;
//         startImageFader(img);
//         show("pointDetails");
//         
//         require(["dojo/_base/html", "dojo/dom-style"], function(html, domStyle){
//             var position = html.position(img, true);
//             domStyle.set("pointDetails", "top", position.y +"px");
//         });
//     }
 }


/*
 * Methods for PointList Tag
 */
 
 /**
  * Delete the current editing point
  */
function deletePoint() {
	 if(currentPoint){
		 if(confirm(mangoMsg['table.confirmDelete.DataPoint'])){
			 //TODO May need to batch the DWR Calls to ensure that all Data Points doesn't refresh before the remove call
			//dwr.engine.beginBatch(); //Force batching of the next calls
			dataPoints.remove(currentPoint.id, true); //Force no showing of the confirm again
	    	closePoint(); //Close the tab
			 //Also refresh the all data points if it is in this view
	         if(typeof allDataPoints != 'undefined')
	        	 allDataPoints.refresh(); 
	         //dwr.engine.endBatch(); //Force batching of the next calls
		 }
	 }
}

 
 /**
  * Close out the point details tab
  */
 function closePoint() {
	 var tc = dijit.byId("dataSourcePropertiesTabContainer");
	 var cp1 = dijit.byId("dataPointDetails-tab");
	 //tc.removeChild(cp1);  (Hard to recreate tab later then)
	
	 tc.selectChild('pointTable-tab');
	 cp1.set('disabled',true);
 }

 /**
  * Show the point control icon
  * @param vo
  * @returns
  */
 function showPointStatus(enabled){
         if(enabled){
             updateImg(
                     $("toggleDataPoint"),
                     mangoImg("database_go.png"),
                     mango.i18n["common.enabledToggle"],
                     true
             );
         }else{
             updateImg(
                     $("toggleDataPoint"),
                     mangoImg("database_stop.png"),
                     mango.i18n["common.enabledToggle"],
                     true
             );
         }
 }
 
 function togglePoint(){
     //Call back to collect all inputs
     currentPoint = dataPoints.getInputs();
     if(currentPoint.id != -1){
    	 dataPoints.toggle(currentPoint.id);
     }

 }
 
 /**
  * Save Point method
  */
 function savePoint() {
     
     startImageFader("pointSaveImg", true);
     hideContextualMessages("pointDetails");

     //Call back to collect all inputs
     currentPoint = dataPoints.getInputs();
     
     //Perform check on point here
     DataPointEditDwr.ensureEditingPointMatch(currentPoint.id, function(response){
         
         if(response.data.match === true){
             if(currentPoint.id != -1){
                 //Point Properties
            	 getDataPointTemplate(currentPoint);
                 getPointProperties(currentPoint); //Set the values from the inputs
                 getLoggingProperties(currentPoint);
                 getTextRenderer(currentPoint);
                 getChartRenderer(currentPoint);
                 getEventDetectors(currentPoint,finishSavePoint); //
              }else{
                  //For now because values aren't set before DWR Call
                  delete currentPoint.discardLowLimit;
                  delete currentPoint.discardHighLimit;      
                  finishSavePoint();
              }
         }else{
             //Do not allow editing.
             alert(response.data.message);
         }
         
         
     });

     


 }
 
 function collectPointSettings(callback){
	 
 }
 /*
  * Ensure all is stored in the edit point and then call the save point impl from the module
  */
 function finishSavePoint(){
	 
	 currentPoint = dataPoints.getInputs();
     var locator = currentPoint.pointLocator;
     
     //Store the Edit Properties
     var myPoint = dojo.clone(currentPoint);
     delete myPoint.pointLocator; //For bug where we aren't mapping the subclasses via dwr appropriately

	 
     // Prevents DWR warnings. These properties are read-only. If sent back to the server
     // DWR will say as much. Deleting the properties saves a bit of logging.
     delete locator.configurationDescription;
     delete locator.dataTypeMessage;

     DataPointDwr.storeEditProperties(myPoint,function(){
         savePointImpl(locator, myPoint); //Edit to allow access to entire point this is a step towards removing server side state of edit point
     });
 }
 
 
 /**
  * Method CB from Dwr save in module
  * 
  * @param response
  */
 function savePointCB(response) {
	 stopImageFader("pointSaveImg");
     if(!response.hasMessages){
         showMessage("pointMessage", mangoTranslate('dsEdit.pointSaved'));
         dataPoints.setInputs(response.data.vo);
         dataPoints.refresh();
         
         //Also refresh the all data points if it is in this view
         if(typeof allDataPoints != 'undefined')
        	 allDataPoints.refresh(); 
         
     }else if (response.hasMessages)
         showDwrMessages(response.messages);
 }
 
 /* Listeners for Data Type Changed */
 var dataTypeChangedCallbacks = new Array(); //Push any listeners into this
 /**	
  * Method to be called from all Data Point pages when the data type is changed
  */
 function dataPointDataTypeChanged(newDataTypeId){
 	dataPoints.currentDataTypeId = newDataTypeId;
 	for(var i=0; i<dataTypeChangedCallbacks.length; i++){
 		if(typeof dataTypeChangedCallbacks[i] == 'function'){
 			dataTypeChangedCallbacks[i](newDataTypeId);
 		}
 	}
 }
 

 /**
  * Get the current alarms for a datasource
  */
 function getAlarms() {
     DataSourceDwr.getAlarms(currentDsId,writeAlarms);
 }
 
 /**
  * Create the alarms table
  * TODO make this a new data table
  * @param alarms
  */
 function writeAlarms(alarms) {
     dwr.util.removeAllRows("alarmsList");
     if (alarms.length == 0) {
         show("noAlarmsMsg");
         hide("alarmsList");
     }
     else {
         hide("noAlarmsMsg");
         show("alarmsList");
         dwr.util.addRows("alarmsList", alarms, [
                 function(alarm) {
                     var div = document.createElement("div");
                     var img = document.createElement("img");
                     setAlarmLevelImg(alarm.alarmLevel, img);
                     div.appendChild(img);
                     
                     var span = document.createElement("span");
                     span.innerHTML = alarm.prettyActiveTimestamp +": "+ alarm.message;
                     div.appendChild(span);
                     
                     return div; 
                 }],
                 {
                     cellCreator: function(options) {
                         var td = document.createElement("td");
                         td.className = "formError";
                         return td;
                     }
                 });
     }
 }

 
 function alarmLevelChanged(eventId) {
     var alarmLevel = $get("alarmLevel"+ eventId);
     DataSourceEditDwr.updateEventAlarmLevel(eventId, alarmLevel);
     setAlarmLevelImg(alarmLevel, "alarmLevelImg"+ eventId);
 }
 
 function exportDataSource() {
     DataSourceEditDwr.exportDataSource(function(json) {
         $set("exportData", json);
         exportDialog.show();
     });
 }
 
 function exportDataPoint() {
     DataSourceEditDwr.exportDataPoint(currentPoint.id, function(json) {
         $set("exportData", json);
         exportDialog.show();
     });
 }
 
 function changeDataSourcePurgeOverride() {
     var override = $get("dataSource.purgeOverride");
     if(typeof override != 'undefined'){
	     $("dataSource.purgePeriod").disabled = !override;
	     $("dataSource.purgeType").disabled = !override;
     }else{
    	 alert('No Purge Override Settings!');
     }
 }
 
 /**
  * Get the General Status of the Data Source
  */
 function getStatusMessages() {
	 
	 if(typeof dataSources != 'undefined'){
	     DataSourceDwr.getGeneralStatusMessages(dataSources.currentId,function(result) {
	         dwr.util.removeAllOptions("generalStatusMessages");
	         dwr.util.addOptions("generalStatusMessages", result.data.messages);
	         if (typeof getStatusMessagesImpl == 'function') getStatusMessagesImpl();
	     });
	 }else{
	     DataSourceDwr.getGeneralStatusMessages(currentDsId,function(result) {
	         dwr.util.removeAllOptions("generalStatusMessages");
	         dwr.util.addOptions("generalStatusMessages", result.data.messages);
	         if (typeof getStatusMessagesImpl == 'function') getStatusMessagesImpl();
	     });
	 }
 }
 
 /**
  * Will only create the tab if it doesn't already exist
  */
 	function createDataPointsTab(){
 		if(dojo.byId("pointTable-tab") == null){
	 		var tc = dijit.byId("dataSourcePropertiesTabContainer");
		    //Setup Data Points List
		    var cp3 = new dijit.layout.ContentPane({
		         title: mangoTranslate('header.dataPoints'),
		         style: "overflow-y: auto",
		         content: "<div id='pointTable-content'></div>",
		         id: 'pointTable-tab',
		       
		    });
		    tc.addChild(cp3);
		    var pd = dojo.byId("pointTableDiv");
		    dojo.place(pd,"pointTable-content");
		    
		    //Now to trick the legacy modules to think the pointProperties div is viewable
		    var ppDiv = dojo.byId("pointProperties");
		    show("pointProperties");
		    dojo.place(ppDiv,"pointTable-content");
		    show("pointTableDiv"); //Show the hidden table
 		}else{
 			show("pointTableDiv"); //Show the hidden table
 		}
 	}//# sourceURL=/resources/dataSourceProperties_ajaxLoaded.js