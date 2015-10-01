<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<!-- Table Div -->
<div id="eventInstanceTableDiv" class="borderDivPadded marB" >
   
    <tag:img png="flag_white" title="events.report"/>
    <span class="smallTitle"><fmt:message key="event.report"/></span>
    <tag:help id="eventReport"/>
	<a href="/pending_alarms.shtm"><fmt:message key="event.legacyAlarmsPage"/></a>
	   <div class="mangoDownloadLinks" style="float:right">
	      <a href="javascript:eventInstances.download()"><fmt:message key="emport.export" /></a>
	      <tag:img png="arrow_down" onclick="eventInstances.download();" title="emport.export"/>
	   </div>
	    
	    <div id="ackAllDiv" class="titlePadding" style="padding-left: 10px; float:right;">
	      <fmt:message key="events.acknowledgeAllEventsInView"/>
	      <tag:img id="ackEventsInViewImg" png="tick" onclick="eventInstances.acknowledgeEventsInView();" title="events.acknowledgeAllEventsInView"/>&nbsp;
	      <fmt:message key="events.silenceAllEventsInView"/>
	      <tag:img id="silenceEventsInViewImg" png="sound_mute" onclick="eventInstances.silenceEventsInView()" title="events.silenceAllEventsInView"/><br/>
	    </div>
	
	    <div id="eventDateSelectionDiv" class="titlePadding" style="float:right" ></div>

    <!-- Ensure this table sits on its own line below -->
    <div id="eventInstanceTable" style="clear:right"></div>

   <div id="totalEventInstancesInView"/></div>
    
</div>