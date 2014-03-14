<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@attribute name="id" rtexprvalue="true" %><%--
--%><%@attribute name="name" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><%@attribute name="onchange" rtexprvalue="true" %><%--
--%><%@attribute name="ms" type="java.lang.Boolean" %><%--
--%><%@attribute name="s" type="java.lang.Boolean" %><%--
--%><%@attribute name="min" type="java.lang.Boolean" %><%--
--%><%@attribute name="h" type="java.lang.Boolean" %><%--
--%><%@attribute name="d" type="java.lang.Boolean" %><%--
--%><%@attribute name="w" type="java.lang.Boolean" %><%--
--%><%@attribute name="mon" type="java.lang.Boolean" %><%--
--%><%@attribute name="y" type="java.lang.Boolean" %><%--
--%><%@attribute name="singular" type="java.lang.Boolean" %><%--
--%><%@attribute name="custom" fragment="true" %><%--
--%><%@tag import="com.serotonin.m2m2.Common"%><%--
--%><sst:select id="${id}" name="${name}" value="${value}" onchange="${onchange}">
  <jsp:invoke fragment="custom"/>
  <c:choose>
    <c:when test="${singular}">
      <c:if test="${ms}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MILLISECONDS) %>"><fmt:message key="common.tp.millisecond"/></sst:option></c:if>
      <c:if test="${s}"><sst:option value="<%= Integer.toString(Common.TimePeriods.SECONDS) %>"><fmt:message key="common.tp.second"/></sst:option></c:if>
      <c:if test="${min}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MINUTES) %>"><fmt:message key="common.tp.minute"/></sst:option></c:if>
      <c:if test="${h}"><sst:option value="<%= Integer.toString(Common.TimePeriods.HOURS) %>"><fmt:message key="common.tp.hour"/></sst:option></c:if>
      <c:if test="${d}"><sst:option value="<%= Integer.toString(Common.TimePeriods.DAYS) %>"><fmt:message key="common.tp.day"/></sst:option></c:if>
      <c:if test="${w}"><sst:option value="<%= Integer.toString(Common.TimePeriods.WEEKS) %>"><fmt:message key="common.tp.week"/></sst:option></c:if>
      <c:if test="${mon}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MONTHS) %>"><fmt:message key="common.tp.month"/></sst:option></c:if>
      <c:if test="${y}"><sst:option value="<%= Integer.toString(Common.TimePeriods.YEARS) %>"><fmt:message key="common.tp.year"/></sst:option></c:if>
    </c:when>
    <c:otherwise>
      <c:if test="${ms}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MILLISECONDS) %>"><fmt:message key="common.tp.milliseconds"/></sst:option></c:if>
      <c:if test="${s}"><sst:option value="<%= Integer.toString(Common.TimePeriods.SECONDS) %>"><fmt:message key="common.tp.seconds"/></sst:option></c:if>
      <c:if test="${min}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MINUTES) %>"><fmt:message key="common.tp.minutes"/></sst:option></c:if>
      <c:if test="${h}"><sst:option value="<%= Integer.toString(Common.TimePeriods.HOURS) %>"><fmt:message key="common.tp.hours"/></sst:option></c:if>
      <c:if test="${d}"><sst:option value="<%= Integer.toString(Common.TimePeriods.DAYS) %>"><fmt:message key="common.tp.days"/></sst:option></c:if>
      <c:if test="${w}"><sst:option value="<%= Integer.toString(Common.TimePeriods.WEEKS) %>"><fmt:message key="common.tp.weeks"/></sst:option></c:if>
      <c:if test="${mon}"><sst:option value="<%= Integer.toString(Common.TimePeriods.MONTHS) %>"><fmt:message key="common.tp.months"/></sst:option></c:if>
      <c:if test="${y}"><sst:option value="<%= Integer.toString(Common.TimePeriods.YEARS) %>"><fmt:message key="common.tp.years"/></sst:option></c:if>
    </c:otherwise>
  </c:choose>  
</sst:select>