<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<!-- Table Div -->
<div id="dataSourceTableDiv" class="borderDivPadded marB" >
    <tag:img png="icon_ds" title="dsList.dataSources"/>
    <span class="smallTitle"><fmt:message key="dsList.dataSources"/></span>
    <tag:help id="dataSourceList"/>
    
    <!-- Select Type of DataSource -->
    <select id="dataSourceTypes" ></select>             
    <span tabindex="0" onkeypress="if(event.keyCode == 13 || event.keyCode == 32) dataSources.open(-1);"><tag:img png="add" title="common.add" id="addDataSource" onclick="dataSources.open(-1)"/></span>
    
    <tag:img png="emport" title="emport.export" style="float:right" id="exportDataSources" onclick="dataSources.showExportUsingFilter()" />
    
    <div id="dataSourceTable"></div>
    
</div>