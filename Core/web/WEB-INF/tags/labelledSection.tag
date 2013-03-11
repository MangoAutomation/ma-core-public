<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@attribute name="id" rtexprvalue="true" %>
<%@attribute name="sectionId" rtexprvalue="true" %>
<%@attribute name="labelKey" rtexprvalue="true" %>
<%@attribute name="closed" type="java.lang.Boolean" rtexprvalue="true" %>
<div<c:if test="${!empty id}"> id="${id}"</c:if> class="labelled-section<c:if test="${closed}"> closed</c:if>">
  <label<c:if test="${!empty sectionId}"> id="labelled-section-${sectionId}"</c:if> onclick="mango.toggleLabelledSection(this)"><fmt:message key="${labelKey}"/></label>
  <div class="labelled-section-inner">
    <jsp:doBody/>
  </div>
</div>