/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
 //TODO Make new AMD format
 dojo.require("dijit.Dialog");
 dojo.require("dijit.form.Form");
 dojo.require("dijit.form.Button");

 
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
	 
	 //For now set the current ID to use for toggle
	 currentDsId = dataSourceId;
	 
	 //Query the remote store
	 dataPointsDataSourceId = dataSourceId;
	 dataPoints.viewStore.dwr.loadData = true;
	 dataPoints.refresh(); //TODO Need to make page empty at start, not load 2x
	 
	 
     changePurgeOverride();
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
     
//     pointListColumnHeaders.push("");
//     pointListColumnFunctions.push(function(p) {
//         var html = "<a href='/data_point_edit.shtm?dpid=" + p.id + "'>";
//         html += writeImage("editImg"+ p.id, null, "icon_comp_edit", "<fmt:message key='pointEdit.props.props'/>",null);
//         html += "</a>";
//         html += writeImage("editImg"+ p.id, null, "pencil", "<fmt:message key='common.edit'/>", "editPoint("+ p.id +")");
//         
//         return html;
//     });
//     
//     var headers = $("pointListHeaders");
//     var td;
//     for (var i=0; i<pointListColumnHeaders.length; i++) {
//         td = document.createElement("td");
//         if (typeof(pointListColumnHeaders[i]) == "string")
//             td.innerHTML = pointListColumnHeaders[i];
//         else
//             pointListColumnHeaders[i](td);
//         headers.appendChild(td);
//     }
//     
//     pointListOptions = {
//             rowCreator: function(options) {
//                 var tr = document.createElement("tr");
//                 tr.mangoId = "p"+ options.rowData.id;
//                 tr.className = "row"+ (options.rowIndex % 2 == 0 ? "" : "Alt");
//                 return tr;
//             },
//             cellCreator: function(options) {
//                 var td = document.createElement("td");
//                 if (options.cellNum == 2)
//                     td.align = "center";
//                 return td;
//             }
//     };
     
       var dsStatus = $("dsStatusImg");
       setDataSourceStatusImg(dataSourceEnabled, dsStatus);
       show(dsStatus);
     
    if (typeof initImpl == 'function') initImpl();
     
	//Get any alarms
	getAlarms();
     showMessage("dataSourceMessage");
     showMessage("pointMessage");
     
     getStatusMessages();
 }
 

 function saveDataSource() {
     startImageFader("dsSaveImg", true);
     hideContextualMessages($("dataSourceProperties"));
     saveDataSourceImpl({
         name: $get("dataSourceName"),
         xid: $get("dataSourceXid"),
         purgeOverride: $get("dataSourcePurgeOverride"),
         purgePeriod: $get("dataSourcePurgePeriod"),
         purgeType: $get("dataSourcePurgeType")
     });
 }
 
 /**
  * Callback from Module DS Save
  * @param response
  */
 function saveDataSourceCB(response) {
     stopImageFader("dsSaveImg");
     if (response.hasMessages)
         showDwrMessages(response.messages, "dataSourceGenericMessages");
     else {
         showMessage("dataSourceMessage", mangoTranslate('dsEdit.saved'));
         //Need to Update the DS Table for sure
         if(typeof dataSources != 'undefined'){
        	 dataSources.setInputs(response.data.vo);
        	 dataSources.refresh();
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
	         var enabled = dataSources.toggle(dataSources.currentId, function(enabled){
		    	 setDataSourceStatusImg(enabled, imgNode);
		    	 getAlarms();
		    	 getStatusMessages();
		         stopImageFader(imgNode);
	         });
	     }
     }else{
    	 //No DS Table defined (Is this possible?)
	     if (!hasImageFader(imgNode)) {
	         DataSourceDwr.toggle(currentDsId,function(result) {
	             var imgNode = $("dsStatusImg");
	             stopImageFader(imgNode);
	             setDataSourceStatusImg(result.data.enabled, imgNode);
	             getAlarms();
	             getStatusMessages();
	         });
	         startImageFader(imgNode);
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
 
 function addPoint(ref) {
     if (!isShowing("pointProperties")) {
         alert(mangoTranslate('dsEdit.saveWarning'));
         return;
     }
     
     if (currentPoint)
         stopImageFader("editImg"+ currentPoint.id);
     
     startImageFader("editImg"+ mango.newId);
     hideContextualMessages("pointProperties");
     
     addPointImpl(ref);
 }
 
// function editPoint(pointId) {
//     hideContextualMessages("pointProperties");
//     DataSourceEditDwr.getPoint(pointId, editPointCB);
// }
 
// // This method can be used by implementations to add a new point from e.g. a tool. See Modbus for an example.
// function editPointCB(point) {
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
// }


/*
 * Methods for PointList Tag
 */
 
 /**
  * Delete the current editing point
  */
function deletePoint() {
	 if(currentPoint){
		 if(dataPoints.remove(currentPoint.id))
			 currentPoint = null;
	 }
}

 
 /**
  * Close the Edit Window
  */
 function closePoint() {
	 dataPoints.close();
     currentPoint = null;
 }
 
 /**
  * Save Point method
  */
 function savePoint() {
     startImageFader("pointSaveImg", true);
     hideContextualMessages("pointProperties");
     
     var locator = currentPoint.pointLocator;
     
     // Prevents DWR warnings. These properties are read-only. If sent back to the server
     // DWR will say as much. Deleting the properties saves a bit of logging.
     delete locator.configurationDescription;
     delete locator.dataTypeMessage;
     
     savePointImpl(locator);
 }
 
 /**
  * Method CB from Dwr save in module
  * 
  * @param response
  */
 function savePointCB(response) {
     stopImageFader("pointSaveImg");
     if (response.hasMessages)
         showDwrMessages(response.messages);
     else {
         showMessage("pointMessage", mangoTranslate('dsEdit.pointSaved'));
         dataPoints.setInputs(response.data.vo);
         dataPoints.refresh();
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
 
 function changePurgeOverride() {
     var override = $get("dataSourcePurgeOverride");
     if(typeof override != 'undefined'){
	     $("dataSourcePurgePeriod").disabled = !override;
	     $("dataSourcePurgeType").disabled = !override;
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
 