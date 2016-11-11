<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf" %><%--
--%><%@tag body-content="empty" %><%--
--%><%@attribute name="event" type="com.serotonin.m2m2.rt.event.EventInstance" rtexprvalue="true" %><%--
--%><%@attribute name="eventBean" type="com.serotonin.m2m2.web.dwr.beans.EventInstanceBean" rtexprvalue="true"%><%--
--%><c:if test="${empty event}"><c:set var="event" value="${eventBean}"/></c:if><%--
--%><c:choose>
  <c:when test="${event.active && event.alarmLevel == 0}"><tag:img png="flag_grey" title="common.alarmLevel.none"/></c:when>
  <c:when test="${event.alarmLevel == 0}"><tag:img png="flag_grey_off" title="common.alarmLevel.none.rtn"/></c:when>
  <c:when test="${event.active && event.alarmLevel == 1}"><tag:img png="flag_blue" title="common.alarmLevel.info"/></c:when>
  <c:when test="${event.alarmLevel == 1}"><tag:img png="flag_blue_off" title="common.alarmLevel.info.rtn"/></c:when>
  <c:when test="${event.active && event.alarmLevel == 2}"><tag:img png="flag_yellow" title="common.alarmLevel.urgent"/></c:when>
  <c:when test="${event.alarmLevel == 2}"><tag:img png="flag_yellow_off" title="common.alarmLevel.urgent.rtn"/></c:when>
  <c:when test="${event.active && event.alarmLevel == 3}"><tag:img png="flag_orange" title="common.alarmLevel.critical"/></c:when>
  <c:when test="${event.alarmLevel == 3}"><tag:img png="flag_orange_off" title="common.alarmLevel.critical.rtn"/></c:when>
  <c:when test="${event.active && event.alarmLevel == 4}"><tag:img png="flag_red" title="common.alarmLevel.lifeSafety"/></c:when>
  <c:when test="${event.alarmLevel == 4}"><tag:img png="flag_red_off" title="common.alarmLevel.lifeSafety.rtn"/></c:when>
  <c:otherwise>(<fmt:message key="common.alarmLevel.unknown"/> ${event.alarmLevel})</c:otherwise>
</c:choose>