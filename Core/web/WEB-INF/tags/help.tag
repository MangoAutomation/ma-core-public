<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="id" rtexprvalue="true" %><%--
--%><tag:img png="help" title="common.help" style="display:inline" onclick="help('${id}', this);"/>