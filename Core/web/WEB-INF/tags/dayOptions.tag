<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="id" %><%--
--%><%@attribute name="value" rtexprvalue="true" %><%--
--%><sst:select id="${id}" value="${value}">
  <c:forEach begin="1" end="31" var="i"><sst:option value="${i}">${m2m2:padZeros(i, 2)}</sst:option></c:forEach>
</sst:select>
