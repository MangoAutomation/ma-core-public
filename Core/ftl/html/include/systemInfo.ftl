<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Terry Packer
-->
<#if highPriorityWorkItems??>
<div style="padding-top: 10px;">
<table cellPadding="1" cellSpacing="1" border="0" style="width:100%;">
	<caption class="smallTitle">High Priority Items</caption>
	<tbody style="outline: 1px solid #F07800">
	<tr><th class="smallTitle">Classname</th><th class="smallTitle">Description</th></tr>
	<#if highPriorityWorkItems?has_content>
	<#list highPriorityWorkItems as workItem>
	<tr style="outline: 1px solid lightgrey"><td>${workItem.classname}</td><td>${workItem.description}</td></tr>
	</#list>
	<#else>
	<tr style="outline: 1px solid lightgrey; text-align: center"><td colspan="2" style="text-align:center;">No Items</td>
	</#if>
	</tbody>
</table>
</div>
</#if>
<#if mediumPriorityWorkItems??>
<div style="padding-top: 10px;">
<table cellPadding="1" cellSpacing="1" border="0" style="width:100%;">
	<caption class="smallTitle">Medium Priority Items</caption>
	<tbody style="outline: 1px solid #F07800">
	<tr><th class="smallTitle">Classname</th><th class="smallTitle">Description</th></tr>
	<#if mediumPriorityWorkItems?has_content>
	<#list mediumPriorityWorkItems as item>
	<tr style="outline: 1px solid lightgrey"><td>${item.classname}</td><td>${item.description}</td></tr>
	</#list>
	<#else>
	<tr style="outline: 1px solid lightgrey; text-align: center"><td colspan="2">No Items</td>
	</#if>
	</tbody>
</table>
</div>
</#if>
<#if lowPriorityWorkItems??>
<div style="padding-top: 10px;">
<table cellPadding="1" cellSpacing="1" border="0" style="width:100%;">
	<caption class="smallTitle">Low Priority Items</caption>
	<tbody style="outline: 1px solid #F07800">
	<tr><th class="smallTitle">Classname</th><th class="smallTitle">Description</th></tr>
	<#if lowPriorityWorkItems?has_content>
	<#list lowPriorityWorkItems as item>
	<tr style="outline: 1px solid lightgrey"><td>${item.classname}</td><td>${item.description}</td></tr>
	</#list>
	<#else>
	<tr style="outline: 1px solid lightgrey; text-align: center"><td colspan="2">No Items</td>
	</#if>	
	</tbody>
</table>
</div>
</#if>
<#if threadList??>
<div style="padding-top: 10px;">
<table cellPadding="1" cellSpacing="1" border="0" style="width:100%;">
	<caption class="smallTitle">Thread List</caption>
	<tbody style="outline: 1px solid #F07800">
	<tr>
	  <th class="smallTitle">Name</th>
	  <th class="smallTitle">State</th>
	  <th class="smallTitle">CPU Time (ms)</th>
	  <th class="smallTitle">Blocked Time (ms)</th>
	  <th class="smallTitle">Wait Time (ms)</th>
	  <th class="smallTitle">Lock Owner Name</th>
	  <th class="smallTitle">Lock Name</th>
	  <th class="smallTitle">Is Suspended</th>
	  <th class="smallTitle">Is In Native</th></tr>
	<#if threadList?has_content>
	<#list threadList as item>
		<tr style="outline: 1px solid lightgrey"><td>${item.threadName}</td><td>${item.threadState}</td><td>${item.cpuTime}</td><td>${item.blockedTime}</td><td>${item.waitTime}</td><td>${item.lockOwnerName}</td><td>${item.lockName}</td><td>${item.suspended?string("yes","no")}</td><td>${item.inNative?string("yes","no")}</td></tr>
	</#list>
	<#else>
	<tr style="outline: 1px solid lightgrey; text-align: center"<td colspan="8">No Items</td>
	</#if>	
	</tbody>
</table>
</div>
</#if>