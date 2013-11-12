<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.Common"%>
  <script type="text/javascript">
          require([ "dojox/mobile/ToolBarButton","dojo/domReady!"]);
  </script>
<h1 id="header" dojoType="dojox.mobile.Heading" fixed="top">
    <button style="display:none" id="navButton" dojoType="dojox.mobile.ToolBarButton" data-dojo-props="arrow:'left'">Back</button>

    <c:if test="${!empty instanceDescription}">
    <span style="float:center;">${instanceDescription}</span>
    </c:if>
    
    <c:if test="${!empty sessionUser}">
    <span style="float:right; padding-left:10px;">
     <fmt:message key="header.user"/>:${sessionUser.username}
     <tag:img id="userMutedImg" onclick="MiscDwr.toggleUserMuted(setUserMuted)"/>
     <m2m2:menuItem id="logoutMi" href="/logout.htm" png="control-power" key="header.logout"/>
     </span>
     
    </c:if>
                 
    <a href="/events.shtm" style="float:right">
       <span id="__header__alarmLevelDiv" style="display:none;">
         <img id="__header__alarmLevelImg" src="/images/spacer.gif" alt="" title=""/>
         <span id="__header__alarmLevelText"></span>
       </span>
    </a>
</h1>
