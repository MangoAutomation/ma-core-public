<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%--
  This snippet supports only binary types. In particular, it only supports
  point views with a BinaryTextRenderer.
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<fmt:message key="common.chooseSetPoint"/>:<br/>
<input type="radio"${pointValue.booleanValue == false ? " checked=\"checked\"" : ""} name="rbChange${componentId}"
        id="rbChange${componentId}F" onclick="mango.view.setPoint(${point.id}, '${componentId}', 'false')"/>
<label for="rbChange${componentId}F">${point.textRenderer.zeroLabel}</label>
<input type="radio"${pointValue.booleanValue == true ? " checked=\"checked\"" : ""}  name="rbChange${componentId}"
        id="rbChange${componentId}T" onclick="mango.view.setPoint(${point.id}, '${componentId}', 'true')"/>
<label for="rbChange${componentId}T">${point.textRenderer.oneLabel}</label>
<tag:relinquish/>