<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@page import="com.serotonin.m2m2.Constants"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<c:set var="dwrClasses">DataSourceEditDwr,DataSourceDwr,DataPointDwr</c:set>
<c:if test="${!empty dataSource.definition.dwrClass}">
  <c:set var="dwrClasses">${dwrClasses},${dataSource.definition.dwrClass.simpleName}</c:set>
</c:if>

<c:forEach items="${dwrClasses}" var="dwrname">
  <script type="text/javascript" src="/dwr/interface/${dwrname}.js"></script></c:forEach>

<c:forEach items="<%= Common.moduleScripts %>" var="modScript">
  <script type="text/javascript" src="/${modScript}"></script></c:forEach>

  <script type="text/javascript" src="/resources/dataSourceProperties.js"></script>
  
    <script type="text/javascript">      
      //Load up our ds, MAY NOT NEED THIS ....
      function init(){
            initProperties(${dataSource.id},${dataSource.enabled});
      }
      
      
  </script>
  
  <tag:labelledSection labelKey="dsEdit.currentAlarms" id="alarmsTable" closed="true">
    <div style="float: right"><tag:img png="control_repeat_blue" title="common.refresh" onclick="getAlarms()"/></div>
    <table>
      <tr id="noAlarmsMsg"><td><b><fmt:message key="dsEdit.noAlarms"/></b></td></tr>
      <tbody id="alarmsList"></tbody>
    </table>
  </tag:labelledSection>
  
  <tag:labelledSection labelKey="dsEdit.rtStatus" closed="true">
    <div style="float: right"><tag:img png="control_repeat_blue" title="common.refresh" onclick="getStatusMessages()"/></div>
    <ul id="generalStatusMessages"></ul>
    <c:if test="${!empty dataSource.definition.statusPagePath}">
      Module-defined status.
      <c:set var="statpage">/<c:out value="<%= Constants.DIR_MODULES %>"/>/${dataSource.definition.module.name}/${dataSource.definition.statusPagePath}</c:set>
      <jsp:include page="${statpage}"/>
    </c:if>
  </tag:labelledSection>
  
  <c:set var="incpage">/<c:out value="<%= Constants.DIR_MODULES %>"/>/${dataSource.definition.module.name}/${dataSource.definition.editPagePath}</c:set>
  <jsp:include page="${incpage}"/>

  <!-- Import the Table and Scripts -->
  <script language="javascript" type="text/javascript" src="/resources/stores.js"></script>
  <script language="javascript" type="text/javascript" src="/resources/dataPoint.js"></script>
  <jsp:include page="/WEB-INF/snippet/dataPointTable.jsp"/>

  