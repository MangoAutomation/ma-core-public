<%--
    Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
    http://infiniteautomation.com/
    @author Jared Wiltshire
--%>
<%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.Common"%>

<header>
<img id="application_logo" src="<%=Common.applicationLogo%>" alt="Logo" />
<c:if test="${!empty instanceDescription}"><span class="instance-description">${instanceDescription}</span></c:if>
<div class="event-summary">
    <div class="level-summary none-event" style="display:none"></div>
    <div class="level-summary information-event" style="display:none"></div>
    <div class="level-summary urgent-event" style="display:none"></div>
    <div class="level-summary critical-event" style="display:none"></div>
    <div class="level-summary life-safety-event" style="display:none"></div>
</div>
</header>
