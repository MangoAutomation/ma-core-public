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
      <m2m2:menuItem def="${mi}"/>
    </c:forEach>
          
    <c:if test="${sessionUser.dataSourcePermission}">
      <img src="/images/menu_separator.png"/>
      <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.DATA_SOURCE) %>" var="mi">
        <m2m2:html5menuItem def="${mi}"/>
      </c:forEach>
    </c:if>
    
    <img src="/images/menu_separator.png"/>
    <m2m2:html5menuItem id="usersMi" href="/users.shtm" png="user" key="header.users"/>
    
    <c:if test="${sessionUser.admin}">
      <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ADMINISTRATOR) %>" var="mi">
        <m2m2:html5menuItem def="${mi}"/>
      </c:forEach>
    </c:if>
    
    <img src="/images/menu_separator.png"/>
    <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
      <m2m2:html5menuItem def="${mi}"/>
    </c:forEach>
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
    </div>
  </c:if>
</nav>

