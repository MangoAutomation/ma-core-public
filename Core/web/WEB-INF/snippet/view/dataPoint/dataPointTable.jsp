<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<div id="pointTableDiv" class="borderDivPadded marB" >
    
<!--    <div class="mangoDownloadLinks"> -->
<%--       <a href="javascript:dataPoints.download()"><fmt:message key="emport.export" /></a> --%>
<%--       <tag:img png="arrow_down" onclick="dataPoints.download();" title="emport.export"/> --%>
      
<%--       <a href="javascript:showDataPointEmport()"><fmt:message key="emport.import" /></a> --%>
<%--       <tag:img png="arrow_up" onclick="showDataPointEmport();" title="emport.import"/> --%>
   
<!--    </div> -->
    
    <tag:img png="icon_comp_edit" title="dsEdit.points.details"/>
    <span class="smallTitle"><fmt:message key="header.dataPoints"/></span>
    <c:if test="${!empty pointHelpId}"><tag:help id="${pointHelpId}"/></c:if>
    <tag:img png="add" title="common.add" id="addDataPoint" onclick="dataPoints.open(-1)"/>  

    <div id="dataPointTable"></div>

<!-- Include the Edit Div -->
<jsp:include page="dataPointEdit.jsp"/>
</div>
<%-- <jsp:include page="/WEB-INF/snippet/dataPointEmport.jsp"/> --%>