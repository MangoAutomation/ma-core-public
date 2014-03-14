<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><c:if test="${point.pointLocator.relinquishable}"><%--
  --%><a href="#" onclick="mango.view.setPoint(${point.id}, '${componentId}', null); return false;"><fmt:message key="common.relinquish"/></a><%--
--%></c:if>