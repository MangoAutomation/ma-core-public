<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<!-- Table Div -->
<div id="eventInstanceTableDiv" class="borderDivPadded marB" >


   
    <tag:img png="flag_white" title="events.alarms"/>
    <span class="smallTitle"><fmt:message key="events.pending"/></span>
    <tag:help id="eventInstance"/>
	   
	   <div class="mangoDownloadLinks" style="float:right">
	      <a href="javascript:eventInstances.download()"><fmt:message key="emport.export" /></a>
	      <tag:img png="arrow_down" onclick="eventInstances.download();" title="emport.export"/>
	      
	<%--       <a href="javascript:showDataPointEmport()"><fmt:message key="emport.import" /></a> --%>
	<%--       <tag:img png="arrow_up" onclick="showDataPointEmport();" title="emport.import"/> --%>
	   
	   </div>
	    
	    <div id="ackAllDiv" class="titlePadding" style="padding-left: 10px; float:right;">
	      <fmt:message key="events.acknowledgeAll"/>
	      <tag:img png="tick" onclick="eventInstances.acknowledgeAll();" title="events.acknowledgeAll"/>&nbsp;
	      <fmt:message key="events.silenceAll"/>
	      <tag:img png="sound_mute" onclick="silenceAll()" title="events.silenceAll"/><br/>
	    </div>
	
	    <div id="eventDateSelectionDiv" class="titlePadding" style="float:right" ></div>

    <!-- Ensure this table sits on its own line below -->
    <div id="eventInstanceTable" style="clear:right"></div>

   <div id="totalEventInstancesInView"/></div>
    
</div>