<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%-- The snippet used for table charts in rollovers --%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<c:choose>
  <c:when test="${empty chartData}"><fmt:message key="common.noData"/></c:when>
  <c:otherwise>
    <c:forEach items="${chartData}" var="historyPointValue">
      ${m2m2:pointValueTime(historyPointValue)} - ${m2m2:htmlText(point, historyPointValue)}
      <c:catch><c:if test="${historyPointValue.sourceMessage}">(<m2m2:translate message="${historyPointValue.sourceMessage}"/>)</c:if></c:catch>
      <br/>
    </c:forEach>
  </c:otherwise>
</c:choose>