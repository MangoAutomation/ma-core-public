<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<c:if test="${!empty startsAndRuntimes}">
  <b><fmt:message key="common.stats.start"/></b>: ${m2m2:fullTime(start)}<br/>
  <b><fmt:message key="common.stats.end"/></b>: ${m2m2:fullTime(end)}<br/>
  <table>
    <tr>
      <th><fmt:message key="common.value"/></th>
      <th><fmt:message key="common.stats.starts"/></th>
      <th><fmt:message key="common.stats.runtime"/></th>
    </tr>
  <c:forEach items="${startsAndRuntimes}" var="sar">
    <tr>
      <td>${m2m2:htmlTextValue(point, sar.dataValue)}</td>
      <td align="right">${sar.starts}</td>
      <td align="right"><fmt:formatNumber value="${sar.proportion}" pattern="0%"/></td>
    </tr>
  </c:forEach>
  </table>
</c:if>
<c:if test="${!empty average}">
  <c:choose>
    <c:when test="${noData}">
      <b><fmt:message key="common.noData"/></b><br/>
    </c:when>
    <c:otherwise>
      <b><fmt:message key="common.stats.start"/></b>: ${m2m2:fullTime(start)}<br/>
      <b><fmt:message key="common.stats.end"/></b>: ${m2m2:fullTime(end)}<br/>
      <b><fmt:message key="common.stats.min"/></b>: ${m2m2:specificHtmlTextValue(point, minimum)} @ ${m2m2:time(minTime)}<br/>
      <b><fmt:message key="common.stats.max"/></b>: ${m2m2:specificHtmlTextValue(point, maximum)} @ ${m2m2:time(maxTime)}<br/>
      <b><fmt:message key="common.stats.avg"/></b>: ${m2m2:specificHtmlTextValue(point, average)}<br/>
      <c:if test="${!empty integral}">
        <b><fmt:message key="common.stats.integral"/></b>: ${m2m2:integralText(point, integral)}<br/>
      </c:if>
      <c:if test="${!empty sum}">
        <b><fmt:message key="common.stats.sum"/></b>: ${m2m2:specificHtmlTextValue(point, sum)}<br/>
      </c:if>
    </c:otherwise>
  </c:choose>
</c:if>
<b><fmt:message key="common.stats.logEntries"/></b>: ${logEntries}