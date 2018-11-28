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
  <c:if test="${allOption}"><sst:option value=""><fmt:message key="common.all"/></sst:option></c:if>
  <sst:option value="<%= AlarmLevels.NONE.name() %>"><fmt:message key="<%= AlarmLevels.NONE.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.INFORMATION.name() %>"><fmt:message key="<%= AlarmLevels.INFORMATION.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.IMPORTANT.name() %>"><fmt:message key="<%= AlarmLevels.IMPORTANT.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.WARNING.name() %>"><fmt:message key="<%= AlarmLevels.WARNING.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.URGENT.name() %>"><fmt:message key="<%= AlarmLevels.URGENT.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.CRITICAL.name() %>"><fmt:message key="<%= AlarmLevels.CRITICAL.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.LIFE_SAFETY.name() %>"><fmt:message key="<%= AlarmLevels.LIFE_SAFETY.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.DO_NOT_LOG.name() %>"><fmt:message key="<%= AlarmLevels.DO_NOT_LOG.getDescription().getKey() %>"/></sst:option>
  <sst:option value="<%= AlarmLevels.IGNORE.name() %>"><fmt:message key="<%= AlarmLevels.IGNORE.getDescription().getKey() %>"/></sst:option>
</sst:select>