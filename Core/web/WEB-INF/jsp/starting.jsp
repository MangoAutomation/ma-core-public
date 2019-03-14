<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="">
    <script type="text/javascript" src="/resources/mangoStatus.js?v=3.6.0"></script>

	<div style="width: 100%; padding: 1em 2em 1em 1em;"
		data-dojo-type="dijit/layout/ContentPane"
		data-dojo-props="region:'center'">
		<div id="startingMessage" class='bigTitle'></div>
		<div id="startupProgress"></div>
		<div id="startupMessage"></div>
	</div>

</tag:page>
