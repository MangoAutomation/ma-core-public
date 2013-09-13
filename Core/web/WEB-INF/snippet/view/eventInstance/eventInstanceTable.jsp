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
    
    <div id="ackAllDiv" class="titlePadding" style="float:right;">
      <fmt:message key="events.acknowledgeAll"/>
      <tag:img png="tick" onclick="MiscDwr.acknowledgeAllPendingEvents()" title="events.acknowledgeAll"/>&nbsp;
      <fmt:message key="events.silenceAll"/>
      <tag:img png="sound_mute" onclick="silenceAll()" title="events.silenceAll"/><br/>
    </div>
    
    <div id="eventInstanceTable"></div>

    <span class="smallTitle"><fmt:message key="events.search"/></span>
    
</div>