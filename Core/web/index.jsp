<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.provider.Providers"%>
<%@page import="com.serotonin.m2m2.web.filter.LoginPageProvider"%>
<c:choose>
  <c:when test="${!empty sessionUser}">
    <c:redirect url="data_point_details.shtm"/>
  </c:when>
  <c:otherwise>
    <c:redirect url="<%= Providers.get(LoginPageProvider.class).getForwardUri() %>"/>
  </c:otherwise>
</c:choose>