<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<tag:page>
<span class="bigTitle"><m2m2:translate key="dsEdit.error"/></span><br/>
<c:if test="${empty errorMessage}">
    <m2m2:translate key="dsEdit.error.general"/>
</c:if>
${errorMessage}
</tag:page>