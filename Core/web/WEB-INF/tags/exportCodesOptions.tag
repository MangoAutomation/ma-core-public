<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="optionList" type="java.util.List" required="true"%><%--
--%><%@attribute name="id" %><%--
--%><%@attribute name="name" %><%--
--%><%@attribute name="onchange" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><sst:select id="${id}" name="${name}" onchange="${onchange}" value="${value}">
  <c:forEach items="${optionList}" var="option">
    <sst:option value="${option.key}"><fmt:message key="${option.value}"/></sst:option>
  </c:forEach>
</sst:select>
