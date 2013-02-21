<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="mi" required="true" rtexprvalue="true" type="com.serotonin.m2m2.module.UrlMappingDefinition"%><%--
--%><c:if test="${m2m2:menuItemIsVisible(mi, pageContext)}"><%--
  --%><tag:menuItem href="${mi.urlPath}" src="${mi.menuImagePath}" key="${mi.menuKey}" target="${mi.target}"/><%--
--%></c:if>