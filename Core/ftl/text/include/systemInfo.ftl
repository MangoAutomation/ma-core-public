<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Terry Packer
-->
<#if highPriorityWorkItems??>
** High Priority Items **

<#if highPriorityWorkItems?has_content>
Classname --- Description
<#list highPriorityWorkItems as item>
${item.classname} --- ${item.description}
</#list>
<#else>
No Items
</#if>
</#if>

<#if mediumPriorityWorkItems??>
** Medium Priority Items **

<#if mediumPriorityWorkItems?has_content>
Classname ---  Description
<#list mediumPriorityWorkItems as item>
${item.classname} --- ${item.description}
</#list>
<#else>
No Items
</#if>
</#if>

<#if lowPriorityWorkItems??>
** Low Priority Items **

<#if lowPriorityWorkItems?has_content>
Classname ---  Description
<#list lowPriorityWorkItems as item>
${item.classname} --- ${item.description}
</#list>
<#else>
No Items
</#if>
</#if>
<#if threadList??>
    ** Thread List**
	Name --- State --- CPU Time (ms) --- Blocked Time (ms) --- Wait Time (ms) --- Lock Owner Name --- Lock Name --- Is Suspended --- Is In Native
	<#if threadList?has_content>
	<#list threadList as item>
	${item.threadName} --- ${item.threadState} --- ${item.cpuTime} --- ${item.blockedTime} --- ${item.waitTime} --- ${item.lockOwnerName} --- ${item.lockName} --- ${item.suspended?string("yes","no")} --- ${item.inNative?string("yes","no")}
	</#list>
	<#else>
	No Items
	</#if>	
</#if>