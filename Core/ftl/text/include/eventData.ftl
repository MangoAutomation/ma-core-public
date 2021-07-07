<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "alarmLevel.ftl">${evt.activeTimestamp?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} - <@fmt message=evt.message/>

<#if renderedPointValues??>
  <#list renderedPointValues as renderedPvt>
  ${renderedPvt.value} @ ${renderedPvt.time}
  </#list>
</#if>

<#if evt.eventComments??>
<#list evt.eventComments as comment>

********** <@fmt key="notes.note"/> - ${comment.ts?number_to_datetime?string["yyyy/MM/dd HH:mm:ss"]} <@fmt key="notes.by"/> <#if comment.username??>${comment.username}<#else><@fmt key="common.deleted"/></#if>
${comment.comment}

</#list>
</#if>
