<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@tag import="com.serotonin.m2m2.module.MenuItemDefinition"%>

<c:if test="${!simple}">
  <table width="100%" cellspacing="0" cellpadding="0" border="0" id="subHeader">
    <tr>
      <td style="cursor:default">
        <c:if test="${!empty sessionUser}">
          <m2m2:menuItem href="/data_point_details.shtm" png="icon_comp" key="header.dataPoints"/>
          <m2m2:menuItem href="/events.shtm" png="flag_white" key="header.alarms"/>
          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.USER) %>" var="mi">
            <m2m2:menuItem def="${mi}"/>
          </c:forEach>
                
          <c:if test="${sessionUser.dataSourcePermission}">
            <img src="/images/menu_separator.png"/>
            <m2m2:menuItem href="/event_handlers.shtm" png="cog" key="header.eventHandlers"/>
            <m2m2:menuItem href="/data_sources.shtm" png="icon_ds" key="header.dataSources"/>
            <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.DATA_SOURCE) %>" var="mi">
              <m2m2:menuItem def="${mi}"/>
            </c:forEach>
          </c:if>
          
          <img src="/images/menu_separator.png"/>
          <m2m2:menuItem href="/users.shtm" png="user" key="header.users"/>
          
          <c:if test="${sessionUser.admin}">
            <m2m2:menuItem href="/point_hierarchy.shtm" png="folder_brick" key="header.pointHierarchy"/>
            <m2m2:menuItem href="/mailing_lists.shtm" png="book" key="header.mailingLists"/>
            <m2m2:menuItem href="/publishers.shtm" png="transmit" key="header.publishers"/>
            <m2m2:menuItem href="/system_settings.shtm" png="application_form" key="header.systemSettings"/>
            <m2m2:menuItem href="/modules.shtm" png="puzzle" key="modules.modules"/>
            <m2m2:menuItem href="/emport.shtm" png="emport" key="header.emport"/>
            <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ADMINISTRATOR) %>" var="mi">
              <m2m2:menuItem def="${mi}"/>
            </c:forEach>
          </c:if>
          
          <img src="/images/menu_separator.png"/>
          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
            <m2m2:menuItem def="${mi}"/>
          </c:forEach>
          <m2m2:menuItem href="/logout.htm" png="control-power" key="header.logout"/>
          <m2m2:menuItem href="/help.shtm" png="help" key="header.help"/>
        </c:if>
        <c:if test="${empty sessionUser}">
          <m2m2:menuItem href="/login.htm" png="control_play_blue" key="header.login"/>
          <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
            <m2m2:menuItem def="${mi}"/>
          </c:forEach>
        </c:if>
        <div id="headerMenuDescription" class="labelDiv" style="position:absolute;display:none;"></div>
      </td>
      
      <td align="right">
        <c:if test="${!empty sessionUser}">
          <span class="copyTitle"><fmt:message key="header.user"/>: <b>${sessionUser.username}</b></span>
          <tag:img id="userMutedImg" onclick="MiscDwr.toggleUserMuted(setUserMuted)" onmouseover="hideLayerIgnoreMissing('localeEdit')"/>
          <tag:img png="house" title="header.goHomeUrl" onclick="goHomeUrl()" onmouseover="hideLayerIgnoreMissing('localeEdit')"/>
          <tag:img png="house_link" title="header.setHomeUrl" onclick="setHomeUrl()" onmouseover="hideLayerIgnoreMissing('localeEdit')"/>
        </c:if>
        <c:if test="${fn:length(availableLanguages) > 1}">
          <div style="display:inline;" onmouseover="showMenu('localeEdit', null, 10, 10);">
            <tag:img png="locale" title="header.changeLanguage"/>
            <div id="localeEdit" style="visibility:hidden;left:0px;top:15px;" class="labelDiv" onmouseout="hideLayer(this)">
              <c:forEach items="${availableLanguages}" var="lang">
                <a class="ptr" onclick="setLocale('${lang.key}')">${lang.value}</a><br/>
              </c:forEach>
            </div>
          </div>
        </c:if>
      </td>
    </tr>
  </table>
</c:if>
