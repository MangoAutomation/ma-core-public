<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" >  
  <div id="help" style="margin:0px 30px 0px 30px">
    <c:if test="${sessionUser.firstLogin}"><h3><fmt:message key="dox.welcomeToMango"/>!</h3></c:if>
    <%-- TODO needs to resolve according to locale rules --%>
    <c:set var="filepath">/WEB-INF/dox/<fmt:message key="dox.dir"/>/help.html</c:set>
    <jsp:include page="${filepath}"/>
  </div>
</tag:page>