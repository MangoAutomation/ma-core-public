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
<#if threadList??>
<table border="1" style="width:100%">
	<caption><b>Thread List</b></caption>
	<tr><th>Name</th><th>State</th><th>CPU Time (ms)</th><th>Blocked Time (ms)</th><th>Wait Time (ms)</th><th>Lock Owner Name</th><th>Lock Name</th><th>Is Suspended</th><th>Is In Native</th></tr>
	<#if threadList?has_content>
	<#list threadList as item>
		<tr><td>${item.threadName}</td><td>${item.threadState}</td><td>${item.cpuTime}</td><td>${item.blockedTime}</td><td>${item.waitTime}</td><td>${item.lockOwnerName}</td><td>${item.lockName}</td><td>${item.suspended?string("yes","no")}</td><td>${item.inNative?string("yes","no")}</td></tr>
	</#list>
	<#else>
	<tr><td colspan="8">No Items</td>
	</#if>	
</table>
</#if>