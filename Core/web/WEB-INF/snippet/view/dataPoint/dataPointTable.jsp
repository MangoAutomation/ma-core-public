<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<div id="pointTableDiv" class="borderDivPadded marB" >
    
    <tag:img png="icon_comp_edit" title="dsEdit.points.details"/>
    <span class="smallTitle"><fmt:message key="header.dataPoints"/></span>
    <c:if test="${!empty pointHelpId}"><tag:help id="${pointHelpId}"/></c:if>
    <tag:img png="add" title="common.add" id="addDataPoint" onclick="dataPoints.open(-1)"/>  
    <tag:img png="emport" title="emport.export" style="float:right" id="exportDataPointForDataSource" onclick="dataPoints.showExportUsingFilter()" />
    
    
    <div id="dataPointTable"></div>

<%-- Include the Edit Div --%>
<jsp:include page="dataPointEdit.jsp"/>
</div>
