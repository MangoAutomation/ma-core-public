<#--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
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
