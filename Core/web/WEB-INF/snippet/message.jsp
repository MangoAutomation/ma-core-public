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
	    "common.alarmLevel.none",
	    "common.alarmLevel.none.rtn",
	    "common.alarmLevel.info",
	    "common.alarmLevel.info.rtn",
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
	    "common.time",
	    "common.inactiveTime",
		"common.nortn",
		
	    "dsList.name",
	    "dsList.type",
	    "dsList.connection",
	    "dsList.status",
	    
	    "dsEdit.deviceName",
	    "dsEdit.pointDataType",
	    "dsEdit.pointSaved",
	    "dsEdit.dataSourceSaved",
	    "dsEdit.saved",
	    "dsEdit.points.details",
	    
	    "emport.export",
	    "emport.import",
	    
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
        "events.unsilence",
        "events.unacknowledged",
	    
        "modules.modules",
        
	    "notes.addNote",
	    
        "table.confirmDelete.DataSource",
        "table.confirmDelete.DataPoint",
        
        "table.edit",
        "table.add",
        "table.delete",
        "table.copy",
        "table.toggle",
        "table.export",
        "table.noData",
        "table.missingKey",
        "table.error.wrongId",
        "table.error.dwr",
        
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
           console.log("Missing Key in mangoMsg:" + key);
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
</script>