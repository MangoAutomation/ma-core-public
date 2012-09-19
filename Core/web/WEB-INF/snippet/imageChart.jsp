<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%-- The snippet used for image charts in rollovers --%><%--
--%><%@ include file="/WEB-INF/jsp/include/tech.jsp" %><%--
--%><c:choose><%--
  --%><c:when test="${empty pointValue}"><tag:img png="hourglass" title="common.noData"/></c:when><%--
  --%><c:otherwise><img src="chart/${pointValue.time}_${point.chartRenderer.duration}_${point.id}.png?w=400&h=150" width="400" height="150" alt="<fmt:message key="common.genChart"/>"/></c:otherwise><%--
--%></c:choose>