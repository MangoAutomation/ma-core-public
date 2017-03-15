<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ page isErrorPage="true" %>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<%
// Store the stack trace as a request attribute.
java.io.StringWriter sw = new java.io.StringWriter();
if (request.getSession(false) != null) {
	java.lang.Throwable t = (java.lang.Throwable)request.getSession().getAttribute(Common.SESSION_USER_EXCEPTION);
	if(t != null)
		t.printStackTrace(new java.io.PrintWriter(sw));
}

// Write the request url into the message.
sw.append("\r\nREQUEST URL\r\n");
sw.append(request.getRequestURL());

// Write the request parameters.
sw.append("\r\n\r\nREQUEST PARAMETERS\r\n");
java.util.Enumeration names = request.getParameterNames();
while (names.hasMoreElements()) {
    String name = (String) names.nextElement();
    sw.append("   ").append(name).append('=').append(request.getParameter(name)).append("\r\n");
}

// Write the request headers.
sw.append("\r\n\r\nREQUEST HEADERS\r\n");
names = request.getHeaderNames();
while (names.hasMoreElements()) {
    String name = (String) names.nextElement();
    sw.append("   ").append(name).append('=').append(request.getHeader(name)).append("\r\n");
}

// Write the page attributes.
//sw.append("\r\n\r\nPAGE ATTRIBUTES\r\n");
//names = pageContext.getAttributeNames();
//while (names.hasMoreElements()) {
//    String name = (String) names.nextElement();
//    sw.append("   ").append(name).append('=').append(pageContext.getAttribute(name)).append("\r\n");
//}

// Write the request attributes.
sw.append("\r\n\r\nREQUEST ATTRIBUTES\r\n");
names = request.getAttributeNames();
while (names.hasMoreElements()) {
    String name = (String) names.nextElement();
    sw.append("   ").append(name).append('=').append(String.valueOf(request.getAttribute(name))).append("\r\n");
}

if (request.getSession(false) != null) {
    // Write the session attributes.
    sw.append("\r\n\r\nSESSION ATTRIBUTES\r\n");
    names = session.getAttributeNames();
    while (names.hasMoreElements()) {
        String name = (String) names.nextElement();
        sw.append("   ").append(name).append('=').append(String.valueOf(session.getAttribute(name))).append("\r\n");
    }
}

request.setAttribute("stackTrace", sw.toString());
%>

<tag:page>
  <br/>
  <span class="bigTitle">Permission denied!</span><br/>
  <br/>
  You do not have sufficient authority to access the resource you requested. Sadly, this exception must be logged
  for review by a system administrator.<br/>
  <br/>
</tag:page>