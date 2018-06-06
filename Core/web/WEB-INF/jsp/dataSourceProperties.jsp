<%--
    Copyright (C) 2013 Infinite Automation. All rights reserved.
    @author Terry Packer
--%>
<%@page import="com.serotonin.m2m2.Constants"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<c:set var="dwrClasses">DataSourceEditDwr,DataSourceDwr,DataPointDwr,DataPointEditDwr</c:set>
<c:if test="${!empty dataSource.definition.dwrClass}">
  <c:set var="dwrClasses">${dwrClasses},${dataSource.definition.dwrClass.simpleName}</c:set>
</c:if>

<c:forEach items="${dwrClasses}" var="dwrname">
  <script type="text/javascript" src="/dwr/interface/${dwrname}.js"></script></c:forEach>

<c:forEach items="<%= Common.moduleScripts %>" var="modScript">
  <script type="text/javascript" src="/${modScript}"></script></c:forEach>

  <script type="text/javascript" src="/resources/dataSourceProperties.js"></script>
  
    <script type="text/javascript">      
      function init(){
            initProperties(${dataSource.id},${dataSource.enabled});
      }
  </script>
<!--   <div data-dojo-type="dijit/layout/TabContainer" style="height: auto;"> -->
  <!-- Name for current editing data source -->
  <div class="smallTitle">
    ${fn:escapeXml(dataSource.name)}
  </div>
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
    <hr>
    <div style="float: right"><tag:img png="control_repeat_blue" title="common.refresh" onclick="getPollTimes()"/></div>
    <table>
      <tr><td>
    	<h3><fmt:message key="dsEdit.latestPollTimes"/></h3>
	    <table>
	      <tr id="noPollTimesMsg"><td colspan="2"><b><fmt:message key="dsEdit.noPollTimes"/></b></td></tr>
	      <tr id="pollTimesHeader" class="rowHeader">
	        <td><fmt:message key="dsEdit.pollStartTime"/></td>
	        <td><fmt:message key="dsEdit.pollDuration"/></td>
	      </tr>
	      <tbody id="pollTimesList"></tbody>
	    </table>
	   </td>
	   <td>
	    <h3><fmt:message key="dsEdit.latestAbortedPollTimes"/></h3>
	    <table>
	      <tr id="noAbortedPollTimesMsg"><td colspan="2"><b><fmt:message key="dsEdit.noAbortedPolls"/></b></td></tr>
	      <tr id="abortedPollTimesHeader" class="rowHeader">
	        <td><fmt:message key="dsEdit.pollStartTime"/></td>
	      </tr>
	      <tbody id="abortedPollTimesList"></tbody>
	    </table>
	   </td>
	  </tr>
    </table>
  </tag:labelledSection>

    <div id="dataSourcePropertiesTabContainer" data-dojo-props="doLayout: false"  style="height: auto;">    </div>
      <c:set var="incpage">/<c:out value="<%= Constants.DIR_MODULES %>"/>/${dataSource.definition.module.name}/${dataSource.definition.editPagePath}</c:set>
      <jsp:include page="${incpage}"/>
      <!-- Import the Table and Scripts -->
      <script type="text/javascript"  src="/resources/stores.js"></script>
      <script type="text/javascript"  src="/resources/view/dataPoint/dataPoint.js"></script>
      
      <div style="display: none;"><jsp:include page="/WEB-INF/snippet/view/dataPoint/dataPointTable.jsp"/></div>
      
