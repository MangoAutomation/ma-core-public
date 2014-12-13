<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<tag:page>
<c:if test="${errorMessage != null }">
    <m2m2:translate key="pointEdit.error.general"/>
</c:if>
${fn:escapeXml(errorMessage)}
</tag:page>