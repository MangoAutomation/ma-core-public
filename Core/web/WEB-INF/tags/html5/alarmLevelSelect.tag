<%--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%><%@tag import="com.serotonin.m2m2.rt.event.AlarmLevels"%><%--
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@tag body-content="empty" %><%--
--%><%@attribute name="id" rtexprvalue="true" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><%@attribute name="onchange" rtexprvalue="true" %><%--
--%><%@attribute name="required" rtexprvalue="true" %><%--
--%><%@attribute name="allOption" type="java.lang.Boolean" %><%--
--%><select id="${id}" name="${name}" value="${value}" onchange="${onchange}" required="${required}" class="formNormal">
  <c:if test="${allOption}"><sst:option value=""><fmt:message key="common.all"/></sst:option></c:if>
  <option value="<%= AlarmLevels.NONE.name() %>"><fmt:message key="<%= AlarmLevels.NONE.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.INFORMATION.name() %>"><fmt:message key="<%= AlarmLevels.INFORMATION.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.IMPORTANT.name() %>"><fmt:message key="<%= AlarmLevels.IMPORTANT.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.WARNING.name() %>"><fmt:message key="<%= AlarmLevels.WARNING.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.URGENT.name() %>"><fmt:message key="<%= AlarmLevels.URGENT.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.CRITICAL.name() %>"><fmt:message key="<%= AlarmLevels.CRITICAL.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.LIFE_SAFETY.name() %>"><fmt:message key="<%= AlarmLevels.LIFE_SAFETY.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.DO_NOT_LOG.name() %>"><fmt:message key="<%= AlarmLevels.DO_NOT_LOG.getDescription().getKey() %>"/></option>
  <option value="<%= AlarmLevels.IGNORE.name() %>"><fmt:message key="<%= AlarmLevels.IGNORE.getDescription().getKey() %>"/></option>
</select>