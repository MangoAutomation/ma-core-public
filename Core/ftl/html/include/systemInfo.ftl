<#--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
-->
<#if highPriorityWorkItems??>
<table border="1" style="width:100%">
	<caption><b>High Priority Items</b></caption>
	<tr><th>Classname</th><th>Description</th></tr>
	<#if highPriorityWorkItems?has_content>
	<#list highPriorityWorkItems as item>
	<tr><td>${item.classname}</td><td>${item.description}</td></tr>
	</#list>
	<#else>
	<tr><td colspan="2">No Items</td>
	</#if>
</table>
</#if>
<#if mediumPriorityWorkItems??>
<table border="1" style="width:100%">
	<caption><b>Medium Priority Items</b></caption>
	<tr><th>Classname</th><th>Description</th></tr>
	<#if mediumPriorityWorkItems?has_content>
	<#list mediumPriorityWorkItems as item>
	<tr><td>${item.classname}</td><td>${item.description}</td></tr>
	</#list>
	<#else>
	<tr><td colspan="2">No Items</td>
	</#if>
</table>
</#if>
<#if lowPriorityWorkItems??>
<table border="1" style="width:100%">
	<caption><b>Low Priority Items</b></caption>
	<tr><th>Classname</th><th>Description</th></tr>
	<#if lowPriorityWorkItems?has_content>
	<#list lowPriorityWorkItems as item>
	<tr><td>${item.classname}</td><td>${item.description}</td></tr>
	</#list>
	<#else>
	<tr><td colspan="2">No Items</td>
	</#if>	
</table>
</#if>