<%--
  Define a global store for the messages to use in translations on pages via 
  the mangoMsg['key'] javascript array
  
  Copyright (C) 2013 Infinite Automation. All rights reserved.
  @author Terry Packer, Jared Wiltshire
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<!-- Add in useful messages for page, also done in M2M2ContextListener, should be moved to here -->
<%
String[] mangoMessageKeys = {
		
	    "common.active",
	    "common.alarmLevel",
	    "common.alarmLevel.greaterthan.none",
	    "common.alarmLevel.greaterthan.info",
	    "common.alarmLevel.greaterthan.important",
	    "common.alarmLevel.greaterthan.warning",
	    "common.alarmLevel.greaterthan.urgent",
	    "common.alarmLevel.greaterthan.critical",
	    "common.alarmLevel.greaterthan.lifeSafety",
	    "common.alarmLevel.none",
	    "common.alarmLevel.none.rtn",
	    "common.alarmLevel.info",
	    "common.alarmLevel.info.rtn",
	    "common.alarmLevel.important",
	    "common.alarmLevel.important.rtn",
	    "common.alarmLevel.warning",
	    "common.alarmLevel.warning.rtn",
	    "common.alarmLevel.urgent",
	    "common.alarmLevel.urgent.rtn",
	    "common.alarmLevel.critical",
	    "common.alarmLevel.critical.rtn",
	    "common.alarmLevel.lifeSafety",
	    "common.alarmLevel.lifeSafety.rtn",
	    "common.alarmLevel.unknown",
	    "common.all",
	    "common.dateRangeFrom",
	    "common.dateRangeTo",
	    "common.duration",
	    "common.durationStd",
	    "common.durationDays",
	    "common.inactiveTime",
		"common.nortn",
		"common.name",
		"common.status",
	    "common.time",
		"common.totalResults",
		"common.clearDates",
		"common.xid",
		
		"chartRenderer.none",
		"chartRenderer.image",
		"chartRenderer.flipbook",
		"chartRenderer.statistics",
		"chartRenderer.table",
		
	    "dsList.name",
	    "dsList.type",
	    "dsList.connection",
	    "dsList.status",
	    
	    "dsEdit.deviceName",
	    "dsEdit.dataSourceType",
	    "dsEdit.pointDataType",
	    "dsEdit.pointSaved",
	    "dsEdit.dataSourceSaved",
	    "dsEdit.saved",
	    "dsEdit.saveWarning",
	    "dsEdit.points.details",
	    
	    "emport.export",
	    "emport.import",
	    
	    "event.rtn.rtn",
	    
        "events.acknowledge",
        "events.acknowledged",
        "events.editDataSource",
        "events.editEventHandler",
        "events.editPublisher",
	    "events.id",
        "events.msg",
        "events.pointDetails",
        "events.pointEdit",
	    "events.silence",
	    "events.showAuditEvents",
        "events.unsilence",
        "events.unacknowledged",
        
	    
        "header.dataPoints",
        
        "modules.modules",
        
	    "notes.addNote",
	    "notes.enterComment",
	    
	    "pointEdit.chart.missingLimit",
	    "pointEdit.chart.invalidLimit",
	    "pointEdit.chart.missingPeriods",
	    "pointEdit.chart.invalidPeriods",
	    "pointEdit.detectors.highLimit",
	    "pointEdit.detectors.lowLimit",
	    "pointEdit.detectors.change",
	    "pointEdit.detectors.state",
	    "pointEdit.detectors.changeCount",
	    "pointEdit.detectors.noChange",
	    "pointEdit.detectors.noUpdate",
	    "pointEdit.detectors.posCusum",
	    "pointEdit.detectors.negCusum",
	    "pointEdit.detectors.regexState",
	    "pointEdit.detectors.range",
        "pointEdit.detectors.smoothness",
	    "pointEdit.logging.period",
	    "pointEdit.logging.tolerance",	    
	    "pointEdit.logging.type",
	    "pointEdit.logging.type.change",
	    "pointEdit.logging.type.all",
	    "pointEdit.logging.type.never",
	    "pointEdit.logging.type.interval",
	    "pointEdit.logging.type.tsChange",     
	    "pointEdit.template.templateName",  
        
        "filter.byReadPermissions",
        "filter.bySetPermissions",
        
        "table.confirmDelete.DataSource",
        "table.confirmDelete.DataPoint",
        "table.confirmDelete.AllDataPoints",
        
        "table.edit",
        "table.add",
        "table.delete",
        "table.copy",
        "table.toggle",
        "table.export",
        "table.exportCSV",
        "table.noData",
        "table.missingKey",
        "table.error.wrongId",
        "table.error.dwr",
        "table.pointDetails",
        
        "textRenderer.analog",
        "textRenderer.binary",
        "textRenderer.multistate",
        "textRenderer.none",
        "textRenderer.plain",
        "textRenderer.range",
        "textRenderer.time",
        "textRenderer.engineeringUnits",        
        
        "view.browse",
        "view.clear",
        "view.submit",
        
};
application.setAttribute("mangoMessageKeys",mangoMessageKeys);

%>



<script type="text/javascript">
	
	//Setup the mango New ID Parameter
	mango.newId = <c:out value="<%= Common.NEW_ID %>"></c:out>;
	
	
	//Create a global array of available messages
	var mangoMsg = {};
	<c:forEach items="${mangoMessageKeys}" var="messageKey" >
	mangoMsg['${messageKey}'] = "<fmt:message key='${messageKey}' />";</c:forEach>

	
	function mangoImg(name) {
		return "/images/" + name;
	}
	
	function mangoTranslate(key, vars) {
	    var msg = mangoMsg[key];
       if(typeof msg == 'undefined'){
           console.log("Missing Key in mangoMsg: " + key);
           return "Missing Msg Key: " + key;
       }

	    if (typeof vars == 'undefined') {
	        return msg;
	    }
	    

	    
	    for (var i = 0; i < vars.length; i++) {
		        msg = msg.replace("'{" + i + "}'", vars[i]);
		        msg = msg.replace("{" + i + "}", vars[i]);
		}
	    return msg;
	}
	
	/**
	 * Some description on how to use this would be good.
	 */
	function mangoAppendTranslations(map) {
		if(typeof(map) != "object")
			return;
		for(key in map) {
			if(key in mangoMsg)
				console.log("Recieved duplicate key: " + key);
			else if(typeof(map[key]) != "string")
				console.log("Recieved nonstring value as translation for: " + key);
			else
				mangoMsg[key] = map[key];
		}
	}
</script>