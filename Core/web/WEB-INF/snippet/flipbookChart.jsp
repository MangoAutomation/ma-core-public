<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%-- The snippet used for histories of images --%>
<%@ include file="/WEB-INF/snippet/common.jsp" %>
<%@page import="com.serotonin.m2m2.web.servlet.ImageValueServlet"%>
<c:choose>
  <c:when test="${empty chartData}"><fmt:message key="common.noData"/></c:when>
  <c:otherwise>
    <table cellpadding="0" cellspacing="0">
      <tr><td>
        <c:forEach items="${chartData}" var="imageValue" varStatus="status">
          <img src="<%= ImageValueServlet.servletPath %>${imageValue.value.filename}?id=${point.id}w=50&h=50"/>
        </c:forEach>
      </td></tr>
    </table>
    <script type="text/javascript">
      var flipbookArray${componentId} = [];
        flipbookArray${componentId}[${status.index}] = "";
      $("flipbook${componentId}").src = "<%= ImageValueServlet.servletPath %>"+ flipbookArray${componentId}[flipbookArray${componentId}.length-1] +"?w=400&h=300";
      alert($("flipbook${componentId}").src);
    </script>
  </c:otherwise>
</c:choose>