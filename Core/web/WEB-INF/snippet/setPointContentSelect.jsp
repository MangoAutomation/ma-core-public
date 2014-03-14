<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<sst:select value="${rawText}" id="setPointValue${idSuffix}">
  <c:forEach items="${point.textRenderer.multistateValues}" var="valueDef">
    <sst:option value="${valueDef.key}">${valueDef.text}</sst:option>
  </c:forEach>
</sst:select>