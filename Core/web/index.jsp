<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%--
!!!!
!!!! J.W. Note: This file must result in a 0 byte response or the redirect in index.jsp will result in a org.eclipse.jetty.io.EofException
!!!! being thrown which closes the TCP connection. The JSP comments at the start and end of each line prevent CR/LF being written to the response.
!!!!
--%><%--
--%><%@include file="/WEB-INF/jsp/include/tech.jsp" %><%--
--%><%@page import="com.serotonin.m2m2.module.DefaultPagesDefinition" %><%--
--%><%@page import="com.serotonin.m2m2.Common" %><%--
--%><c:redirect url="<%= DefaultPagesDefinition.getDefaultUri(request, response, Common.getHttpUser()) %>"/>