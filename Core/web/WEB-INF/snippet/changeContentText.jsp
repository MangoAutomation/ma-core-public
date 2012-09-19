<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<fmt:message key="common.enterSetPoint"/>:<br/>
<input id="txtChange${componentId}" type="text" value="${m2m2:rawText(point, pointValue)}" 
        onkeypress="if (event.keyCode==13) $('txtSet${componentId}').onclick();"/>
<a id="txtSet${componentId}" class="ptr"
        onclick="mango.view.setPoint(${point.id}, '${componentId}', $('txtChange${componentId}').value);"><fmt:message key="common.set"/></a>
<tag:relinquish/>