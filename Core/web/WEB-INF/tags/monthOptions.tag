<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag import="org.joda.time.DateTimeConstants"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="id" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><sst:select id="${id}" value="${value}">
  <sst:option value="<%= Integer.toString(DateTimeConstants.JANUARY) %>"><fmt:message key="common.month.jan"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.FEBRUARY) %>"><fmt:message key="common.month.feb"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.MARCH) %>"><fmt:message key="common.month.mar"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.APRIL) %>"><fmt:message key="common.month.apr"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.MAY) %>"><fmt:message key="common.month.may"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.JUNE) %>"><fmt:message key="common.month.jun"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.JULY) %>"><fmt:message key="common.month.jul"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.AUGUST) %>"><fmt:message key="common.month.aug"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.SEPTEMBER) %>"><fmt:message key="common.month.sep"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.OCTOBER) %>"><fmt:message key="common.month.oct"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.NOVEMBER) %>"><fmt:message key="common.month.nov"/></sst:option>
  <sst:option value="<%= Integer.toString(DateTimeConstants.DECEMBER) %>"><fmt:message key="common.month.dec"/></sst:option>
</sst:select>