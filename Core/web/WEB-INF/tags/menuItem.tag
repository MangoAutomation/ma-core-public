<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="href" required="true" rtexprvalue="true"%><%--
--%><%@attribute name="png" %><%--
--%><%@attribute name="src" rtexprvalue="true" %><%--
--%><%@attribute name="key" rtexprvalue="true" required="true"%><%--
--%><%@attribute name="target" rtexprvalue="true" %><%--
--%><c:set var="text"><fmt:message key="${key}"/></c:set><%--
--%><a href="${href}"<c:if test="${!empty target}"> target="${target}"</c:if>><tag:img png="${png}" src="${src}"
  onmouseout="if (typeof hMD == 'function') hMD();"
  onmouseover="if (typeof hMD == 'function') hMD('${text}', this);"/></a>