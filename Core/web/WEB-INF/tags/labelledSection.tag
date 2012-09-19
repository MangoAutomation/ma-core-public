<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@attribute name="labelKey" rtexprvalue="true" %>
<%@attribute name="closed" type="java.lang.Boolean" rtexprvalue="true" %>

<div class="labelled-section<c:if test="${closed}"> closed</c:if>">
  <label onclick="mango.toggleLabelledSection(this)"><fmt:message key="${labelKey}"/></label>
  <div class="labelled-section-inner">
    <jsp:doBody/>
  </div>
</div>