<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.module.DefaultPagesDefinition"%>
<%@page import="com.serotonin.m2m2.vo.Common"%>
<c:redirect url="<%= DefaultPagesDefinition.getDefaultUri(request, response, Common.getHttpUser()) %>"/>