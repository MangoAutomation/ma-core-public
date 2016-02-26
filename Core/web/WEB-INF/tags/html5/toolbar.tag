<%--
    Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
    http://infiniteautomation.com/
    @author Jared Wiltshire
--%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@tag import="com.serotonin.m2m2.module.MenuItemDefinition"%>

<nav>
  <c:if test="${!empty sessionUser}">
    <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.USER) %>" var="mi">
      <m2m2:html5menuItem def="${mi}"/>
    </c:forEach>
          
    <c:if test="${sessionUser.dataSourcePermission}">
      <c:set var="dataSourceItems" value="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.DATA_SOURCE) %>" />
      <c:if test="${!empty dataSourceItems}">
        <img src="/images/menu_separator.png"/>
        <c:forEach items="${dataSourceItems}" var="mi">
          <m2m2:html5menuItem def="${mi}"/>
        </c:forEach>
      </c:if>
    </c:if>
    
    <c:if test="${sessionUser.admin}">         
      <c:set var="adminItems" value="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ADMINISTRATOR) %>" />
      <c:if test="${!empty adminItems}">
        <img src="/images/menu_separator.png"/> 
        <c:forEach items="${adminItems}" var="mi">
          <m2m2:html5menuItem def="${mi}"/>
        </c:forEach>
      </c:if>
    </c:if>

    <c:set var="anonItems" value="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" />
    <c:if test="${!empty anonItems }">
      <img src="/images/menu_separator.png"/>
      <c:forEach items="${anonItems}" var="mi">
        <m2m2:html5menuItem def="${mi}"/>
      </c:forEach>
    </c:if>
  </c:if>
  
  <c:if test="${empty sessionUser}">
    <m2m2:html5menuItem id="loginMi" href="/login.htm" png="control_play_blue" key="header.login"/>
    <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
      <m2m2:html5menuItem def="${mi}"/>
    </c:forEach>
  </c:if>
  <c:if test="${!empty sessionUser}">
    <div class="user-nav">
      <span class="username"><fmt:message key="header.user"/>: <strong>${sessionUser.username}</strong></span>
      <m2m2:html5menuItem id="logoutMi" href="/logout.htm" png="control-power" key="header.logout"/>
        <img id="userMutedIcon" class="ptr" <%-- We don't have ability to play a sound --%> />
        <div id="homeWidget" style="display: inline;" >
          <img id="goHome" class="ptr" src="/images/house.png" title="<fmt:message key='header.goHomeUrl'/>" alt="<fmt:message key='header.goHomeUrl'/>"/>
          <div id="userHome" style="display:none;" class="labelDiv" >
            <img id="saveHome" class="ptr" src="/images/house_link.png" title="<fmt:message key='header.setHomeUrl'/>" alt="<fmt:message key='header.setHomeUrl'/>" />
          </div>
        </div>
    </div>
  </c:if>
</nav>

