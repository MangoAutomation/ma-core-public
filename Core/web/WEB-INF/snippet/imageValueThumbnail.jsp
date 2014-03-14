<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.web.servlet.ImageValueServlet"%>
<c:if test="${!empty point.pointLocator.webcamLiveFeedCode}"><a href="webcam_live_feed.htm?pointId=${point.id}" target="webcamLiveFeed"></c:if>
<img src="<%= ImageValueServlet.servletPath %>${pointValue.time}_${point.id}.${pointValue.value.typeExtension}?w=80&h=80" alt="<fmt:message key="common.genThumb"/>"/>
<c:if test="${!empty point.pointLocator.webcamLiveFeedCode}"></a></c:if>