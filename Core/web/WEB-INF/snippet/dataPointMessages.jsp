<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%--
  This snippet supports all data types.
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<c:if test="${!empty disabled}">
  <tag:img png="warn" title="common.pointWarning"/> <fmt:message key="common.pointWarning"/><br/>
</c:if>
<c:if test="${pointRT.attributes.UNRELIABLE}">
  <tag:img png="warn" title="common.valueUnreliable"/> <fmt:message key="common.valueUnreliable"/>
  <tag:img png="arrow_refresh" title="common.refresh" onclick="DataPointDetailsDwr.forcePointRead(${point.id})"/><br/>
</c:if>
<c:forEach items="${events}" var="event">
  <tag:eventIcon event="${event}"/>
  ${m2m2:time(event.activeTimestamp)} - <m2m2:translate message="${event.message}"/>
  <tag:alarmAck event="${event}"/><br/>
</c:forEach>