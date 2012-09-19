<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%--
  This snippet supports only multistate types. In particular, it only supports
  point views with a MultistateRenderer.
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<fmt:message key="common.chooseSetPoint"/>:<br/>
<sst:select value="${m2m2:rawText(point, pointValue)}" onchange="mango.view.setPoint(${point.id}, '${componentId}', this.value)">
  <c:forEach items="${point.textRenderer.multistateValues}" var="valueDef">
    <sst:option value="${valueDef.key}">${valueDef.text}</sst:option>
  </c:forEach>
</sst:select>
<tag:relinquish/>