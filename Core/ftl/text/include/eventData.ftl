<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "alarmLevel.ftl">${evt.prettyActiveTimestamp} - <@fmt message=evt.message/>

<#if evt.eventComments??>
<#list evt.eventComments as comment>

********** <@fmt key="notes.note"/> - ${comment.prettyTime} <@fmt key="notes.by"/> <#if comment.username??>${comment.username}<#else><@fmt key="common.deleted"/></#if>
${comment.comment}

</#list>
</#if>
