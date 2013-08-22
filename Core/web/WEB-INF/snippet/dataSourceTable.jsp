<%--
    Copyright (C) 2013 Deltamation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<!-- Table Div -->
<div id="dataSourceTableDiv" class="borderDivPadded marB" >
	<tag:img png="icon_ds" title="dsList.dataSources"/>
	<span class="smallTitle"><fmt:message key="dsList.dataSources"/></span>
	<tag:help id="dataSourceList"/>
	
    <div id="dataSourceTable"></div>

	<span class="smallTitle"><fmt:message key="common.add"/></span>
	<tag:img png="add" title="common.add" id="addDataSource" onclick="dataSources.open(-1)"/>
	<!-- Select Type of DataSource -->
    <select id="dataSourceTypes" ></select>
	
</div>

<!-- Include the Edit Div -->
<jsp:include page="dataSourceEdit.jsp"/>
<!-- Include the Export Dialog -->
<jsp:include page="/WEB-INF/snippet/exportDialog.jsp"/>

