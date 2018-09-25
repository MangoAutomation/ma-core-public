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
--%><%@attribute name="useIds" type="java.lang.Boolean" %><%--
--%><select id="${id}" name="${name}" value="${value}" onchange="${onchange}" required="${required}" class="formNormal">
<c:choose>
  <c:when test="${useIds}">
    <c:if test="${allOption}"><sst:option value="-1"><fmt:message key="common.all"/></sst:option></c:if>
    <option value="<%= AlarmLevels.NONE %>"><fmt:message key="<%= AlarmLevels.NONE_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.INFORMATION %>"><fmt:message key="<%= AlarmLevels.INFORMATION_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.IMPORTANT %>"><fmt:message key="<%= AlarmLevels.IMPORTANT_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.WARNING %>"><fmt:message key="<%= AlarmLevels.WARNING_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.URGENT %>"><fmt:message key="<%= AlarmLevels.URGENT_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CRITICAL %>"><fmt:message key="<%= AlarmLevels.CRITICAL_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.LIFE_SAFETY %>"><fmt:message key="<%= AlarmLevels.LIFE_SAFETY_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.DO_NOT_LOG %>"><fmt:message key="<%= AlarmLevels.DO_NOT_LOG_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.IGNORE %>"><fmt:message key="<%= AlarmLevels.IGNORE_DESCRIPTION %>"/></option>
  </c:when><c:otherwise>
  <c:if test="${allOption}"><sst:option value="-1"><fmt:message key="common.all"/></sst:option></c:if>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.NONE) %>"><fmt:message key="<%= AlarmLevels.NONE_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.INFORMATION) %>"><fmt:message key="<%= AlarmLevels.INFORMATION_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.IMPORTANT) %>"><fmt:message key="<%= AlarmLevels.IMPORTANT_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.WARNING) %>"><fmt:message key="<%= AlarmLevels.WARNING_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.URGENT) %>"><fmt:message key="<%= AlarmLevels.URGENT_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.CRITICAL) %>"><fmt:message key="<%= AlarmLevels.CRITICAL_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.LIFE_SAFETY) %>"><fmt:message key="<%= AlarmLevels.LIFE_SAFETY_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.DO_NOT_LOG) %>"><fmt:message key="<%= AlarmLevels.DO_NOT_LOG_DESCRIPTION %>"/></option>
    <option value="<%= AlarmLevels.CODES.getCode(AlarmLevels.IGNORE) %>"><fmt:message key="<%= AlarmLevels.IGNORE_DESCRIPTION %>"/></option>
  </c:otherwise>
</c:choose>
</select>