<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@tag import="com.serotonin.m2m2.rt.event.AlarmLevels"%><%--
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@tag body-content="empty" %><%--
--%><%@attribute name="id" rtexprvalue="true" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><%@attribute name="onchange" rtexprvalue="true" %><%--
--%><%@attribute name="allOption" type="java.lang.Boolean" %><%--
--%><sst:select id="${id}" name="${name}" value="${value}" onchange="${onchange}">
  <c:if test="${allOption}"><sst:option value="-1"><fmt:message key="common.all"/></sst:option></c:if>
  <sst:option value="<%= Integer.toString(AlarmLevels.NONE) %>"><fmt:message key="<%= AlarmLevels.NONE_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.INFORMATION) %>"><fmt:message key="<%= AlarmLevels.INFORMATION_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.URGENT) %>"><fmt:message key="<%= AlarmLevels.URGENT_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.CRITICAL) %>"><fmt:message key="<%= AlarmLevels.CRITICAL_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.LIFE_SAFETY) %>"><fmt:message key="<%= AlarmLevels.LIFE_SAFETY_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.DO_NOT_LOG) %>"><fmt:message key="<%= AlarmLevels.DO_NOT_LOG_DESCRIPTION %>"/></sst:option>
  <sst:option value="<%= Integer.toString(AlarmLevels.IGNORE) %>"><fmt:message key="<%= AlarmLevels.IGNORE_DESCRIPTION %>"/></sst:option>
  
</sst:select>