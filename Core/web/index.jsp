<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.module.DefaultPagesDefinition"%>
<%@page import="com.serotonin.m2m2.vo.User"%>
<c:redirect url="<%= DefaultPagesDefinition.getDefaultUri(request, response, (User) session.getAttribute("sessionUser")) %>"/>