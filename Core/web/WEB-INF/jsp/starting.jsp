<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="">
    <script type="text/javascript" src="/resources/mangoStatus.js"></script>

	<div style="width: 100%; padding: 1em 2em 1em 1em;"
		data-dojo-type="dijit/layout/ContentPane"
		data-dojo-props="region:'center'">
		<div id="startingMessage" class='bigTitle'></div>
		<div id="startupProgress"></div>
		<div id="startupMessage"></div>
		<div id="startupConsole"
			style="height: 500px; margin: 1em 3em 1em 1em; border: 2px; padding: .2em 1em 1em 1em; overflow: auto; border: 2px solid; border-radius: 10px; border-color: lightblue;"
			data-dojo-type="dijit/layout/ContentPane"></div>
	</div>

</tag:page>
