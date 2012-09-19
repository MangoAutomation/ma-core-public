<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<c:choose>
  <c:when test="${user.disabled}"><tag:img png="user_disabled" title="common.disabled"/></c:when>
  <c:when test="${user.admin}"><tag:img png="user_suit" title="common.administrator"/></c:when>
  <c:otherwise><tag:img png="user_green" title="common.user"/></c:otherwise>
</c:choose>