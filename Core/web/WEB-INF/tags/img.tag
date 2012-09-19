<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="id" rtexprvalue="true" %><%--
--%><%@attribute name="src" rtexprvalue="true"%><%--
--%><%@attribute name="png" rtexprvalue="true"%><%--
--%><%@attribute name="title"%><%--
--%><%@attribute name="onclick" rtexprvalue="true" %><%--
--%><%@attribute name="onmouseover" rtexprvalue="true"%><%--
--%><%@attribute name="onmouseout"%><%--
--%><%@attribute name="style"%><%--
--%><%@attribute name="align"%><%--
--%><img<c:if test="${!empty id}"> id="${id}"</c:if><%--
--%><c:if test="${!empty src}"> src="${src}"</c:if><%--
--%><c:if test="${!empty png && empty src}"> src="/images/${png}.png"</c:if><%--
--%><c:if test="${!empty title}"> alt="<fmt:message key="${title}"/>" title="<fmt:message key="${title}"/>"</c:if><%--
--%><c:if test="${!empty onclick}"> class="ptr" onclick="${onclick}"</c:if><%--
--%><c:if test="${!empty onmouseover}"> onmouseover="${onmouseover}"</c:if><%--
--%><c:if test="${!empty onmouseout}"> onmouseout="${onmouseout}"</c:if><%--
--%><c:if test="${!empty style}"> style="${style}"</c:if><%--
--%><c:if test="${!empty align}"> align="${align}"</c:if><%--
--%>/>